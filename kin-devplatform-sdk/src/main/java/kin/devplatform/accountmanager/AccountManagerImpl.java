package kin.devplatform.accountmanager;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import kin.devplatform.KinCallback;
import kin.devplatform.Log;
import kin.devplatform.Logger;
import kin.devplatform.base.ObservableData;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.StellarAccountCreationRequested;
import kin.devplatform.bi.events.WalletCreationSucceeded;
import kin.devplatform.data.auth.AuthDataSource;
import kin.devplatform.data.blockchain.BlockchainSource;
import kin.devplatform.exception.BlockchainException;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.network.model.AuthToken;
import kin.devplatform.util.ErrorUtil;
import kin.sdk.migration.MigrationManager;
import kin.sdk.migration.common.exception.AccountNotActivatedException;
import kin.sdk.migration.common.exception.DeleteAccountException;
import kin.sdk.migration.common.exception.MigrationInProcessException;
import kin.sdk.migration.common.interfaces.IBalance;
import kin.sdk.migration.common.interfaces.IEventListener;
import kin.sdk.migration.common.interfaces.IKinAccount;
import kin.sdk.migration.common.interfaces.IKinClient;
import kin.sdk.migration.common.interfaces.IListenerRegistration;
import kin.sdk.migration.common.interfaces.IMigrationManagerCallbacks;
import kin.utils.ResultCallback;

public class AccountManagerImpl implements AccountManager {

	private static final String TAG = AccountManagerImpl.class.getSimpleName();
	private static final long ACCOUNT_CREATION_TIME_OUT_MILLIS = 15 * DateUtils.SECOND_IN_MILLIS;

	private static volatile AccountManagerImpl instance;

	private final AccountManager.Local local;
	private final MigrationManager migrationManager;
	private final EventLogger eventLogger;
	private AuthDataSource authRepository;
	private BlockchainSource blockchainSource;
	private final ObservableData<Integer> accountState;
	private KinEcosystemException error;

	private IListenerRegistration accountCreationRegistration;

	private AccountManagerImpl(@NonNull final Local local,
		MigrationManager migrationManager, @NonNull final EventLogger eventLogger,
		@NonNull final AuthDataSource authRepository,
		@NonNull final BlockchainSource blockchainSource) {
		this.local = local;
		this.migrationManager = migrationManager;
		this.eventLogger = eventLogger;
		this.authRepository = authRepository;
		this.blockchainSource = blockchainSource;
		this.accountState = ObservableData.create(local.getAccountState());
	}

	public static void init(@NonNull final Local local,
		MigrationManager migrationManager, @NonNull final EventLogger eventLogger,
		@NonNull final AuthDataSource authRepository,
		@NonNull final BlockchainSource blockchainSource) {
		if (instance == null) {
			synchronized (AccountManagerImpl.class) {
				if (instance == null) {
					instance = new AccountManagerImpl(local, migrationManager, eventLogger, authRepository,
						blockchainSource);
				}
			}
		}
	}

	public static AccountManager getInstance() {
		return instance;
	}

	@Override
	public @AccountState
	int getAccountState() {
		return accountState.getValue() == ERROR ? ERROR : local.getAccountState();
	}

	@Override
	public boolean isAccountCreated() {
		return local.getAccountState() == CREATION_COMPLETED;
	}

	@Override
	public void addAccountStateObserver(@NonNull Observer<Integer> observer) {
		accountState.addObserver(observer);
		accountState.postValue(accountState.getValue());
	}

	@Override
	public void removeAccountStateObserver(@NonNull Observer<Integer> observer) {
		accountState.removeObserver(observer);
	}

	@Override
	public void retry() {
		if (getKinAccount() != null && accountState.getValue() == ERROR) {
			this.setAccountState(local.getAccountState());
		}
	}

	@Override
	public void start() {
		if (getKinAccount() != null) {
			Logger.log(new Log().withTag(TAG).put("setAccountState", "start"));
			if (getAccountState() != CREATION_COMPLETED) {
				this.setAccountState(local.getAccountState());
			}
		}
	}

	private void setAccountState(@AccountState final int accountState) {
		if (isValidState(this.accountState.getValue(), accountState)) {
			if (accountState != ERROR) {
				this.local.setAccountState(accountState);
			}
			this.accountState.postValue(accountState);
			switch (accountState) {
				case REQUIRE_CREATION:
					handleRequiresCreationState();
					break;
				case PENDING_CREATION:
					handlePendingCreationState();
					break;
				case REQUIRE_TRUSTLINE:
					Logger.log(new Log().withTag(TAG).put("setAccountState", "REQUIRE_TRUSTLINE"));
					// Create trustline transaction with KIN
					blockchainSource.createTrustLine(new KinCallback<Void>() {
						@Override
						public void onResponse(Void response) {
							setAccountState(CREATION_COMPLETED);
						}

						@Override
						public void onFailure(KinEcosystemException error) {
							instance.error = error;
							setAccountState(ERROR);
						}
					});
					break;
				case CREATION_COMPLETED:
					// Mark account creation completed.
					eventLogger.send(WalletCreationSucceeded.create());
					Logger.log(new Log().withTag(TAG).put("setAccountState", "CREATION_COMPLETED"));
					break;
				default:
				case AccountManager.ERROR:
					Logger.log(new Log().withTag(TAG).put("setAccountState", "ERROR"));
					break;

			}
		}
	}

	private void handleRequiresCreationState() {
		eventLogger.send(StellarAccountCreationRequested.create());
		Logger.log(new Log().withTag(TAG).put("setAccountState", "REQUIRE_CREATION"));
		// Trigger account creation from server side, if not triggered already
		if (authRepository.getCachedAuthToken() == null) {
			authRepository.getAuthToken(new KinCallback<AuthToken>() {
				@Override
				public void onResponse(AuthToken response) {
					setAccountState(PENDING_CREATION);
				}

				@Override
				public void onFailure(KinEcosystemException error) {
					instance.error = error;
					setAccountState(ERROR);
				}
			});
		} else {
			setAccountState(PENDING_CREATION);
		}
	}

	private void handlePendingCreationState() {
		Logger.log(new Log().withTag(TAG).put("setAccountState", "PENDING_CREATION"));
		// Start listen for account creation on the blockchain side.
		final Handler handler = new Handler(Looper.getMainLooper());
		removeAccountCreationRegistration();

		prepareAccountCreationTimeoutFallback(handler);
		accountCreationRegistration = getKinAccount().addAccountCreationListener(new IEventListener<Void>() {
			@Override
			public void onEvent(Void data) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						removeAccountCreationRegistration();
						handler.removeCallbacksAndMessages(null);
						setAccountState(REQUIRE_TRUSTLINE);
					}
				});
			}
		});
	}

	private void prepareAccountCreationTimeoutFallback(final Handler handler) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Logger.log(new Log().withTag(TAG).put("PENDING_CREATION", "failed with timeout, querying blockchain"));
				removeAccountCreationRegistration();
				handler.removeCallbacksAndMessages(null);
				//in case of SSE listening timeout, rely on direct call to blockchain for checking account creation
				getKinAccount().getBalance().run(new ResultCallback<IBalance>() {
					@Override
					public void onResult(IBalance result) {
						//result means we have account created successfully on kin blockchain
						setAccountState(REQUIRE_TRUSTLINE);
					}

					@Override
					public void onError(Exception e) {
						if (e instanceof AccountNotActivatedException) {
							//Account not activated means that we have a created account but no trustline, this is the
							//expected result as we should establish trustline in the next state
							setAccountState(REQUIRE_TRUSTLINE);
						} else {
							instance.error = ErrorUtil.createCreateAccountTimeoutException(e);
							setAccountState(ERROR);
						}
					}
				});
			}
		}, ACCOUNT_CREATION_TIME_OUT_MILLIS);
	}

	@Override
	public void switchAccount(final int accountIndex, @NonNull final KinCallback<Boolean> callback,
		@NonNull final IMigrationManagerCallbacks migrationManagerCallbacks) {
		Logger.log(new Log().withTag(TAG).put("switchAccount", "start"));
		updateWalletAddressProcess(accountIndex, callback, migrationManagerCallbacks);
	}

	private void updateWalletAddressProcess(final int accountIndex, @NonNull final KinCallback<Boolean> callback,
		@NonNull final IMigrationManagerCallbacks migrationManagerCallbacks) {
		authRepository.isRestorableWallet(blockchainSource.getPublicAddress(accountIndex),
			new KinCallback<Boolean>() {
				@Override
				public void onResponse(Boolean isRestorable) {
					Logger.log(new Log().priority(Log.DEBUG).withTag(TAG)
						.text("getIsRestorableWallet: onSuccess - restorable = " + isRestorable));
					if (isRestorable) {
						startMigration(accountIndex, callback, migrationManagerCallbacks);
					} else {
						onFailure(ErrorUtil.createWalletWasNotCreatedInThisAppException());
					}
				}

				@Override
				public void onFailure(KinEcosystemException error) {
					Logger.log(new Log().priority(Log.ERROR).withTag(TAG).text("getIsRestorableWallet: onFailure"));
					deleteImportedAccount(accountIndex);
					callback.onFailure(error);
				}
			});
	}

	private void startMigration(final int accountIndex, final KinCallback<Boolean> callback,
		final IMigrationManagerCallbacks migrationManagerCallbacks) {
		Logger.log(new Log().withTag(TAG).put("startMigration", "start"));
		try {
			migrationManager.start(new IMigrationManagerCallbacks() {
				@Override
				public void onMigrationStart() {
					Logger.log(new Log().priority(Log.DEBUG).withTag(TAG).text("onMigrationStart"));
					migrationManagerCallbacks.onMigrationStart();
				}

				@Override
				public void onReady(IKinClient kinClient) {
					Logger.log(new Log().priority(Log.DEBUG).withTag(TAG).text("onReady"));
					migrationManagerCallbacks.onReady(kinClient);
					updateWalletAddress(kinClient, accountIndex, callback);
				}

				@Override
				public void onError(Exception e) {
					Logger.log(new Log().priority(Log.ERROR).withTag(TAG).text("onError"));
					deleteImportedAccount(accountIndex);
					migrationManagerCallbacks.onError(e);
				}
			});
		} catch (MigrationInProcessException e) {
			Logger.log(new Log().priority(Log.DEBUG).withTag(TAG).text("MigrationInProcessException"));
			deleteImportedAccount(accountIndex);
			migrationManagerCallbacks.onError(e);
		}
	}

	private void updateWalletAddress(final IKinClient kinClient, final int accountIndex,
		@NonNull final KinCallback<Boolean> callback) {
		IKinAccount account = kinClient.getAccount(accountIndex);
		final String address = account != null ? account.getPublicAddress() : null;
		//update sign in data with new wallet address and update servers
		authRepository.updateWalletAddress(address, new KinCallback<Boolean>() {
			@Override
			public void onResponse(Boolean response) {
				try {
					//switch to the new KinAccount
					blockchainSource.updateActiveAccount(kinClient, accountIndex);
				} catch (BlockchainException e) {
					onFailure(ErrorUtil.getBlockchainException(e));
					return;
				}
				Logger.log(new Log().withTag(TAG).put("switchAccount", "ended successfully"));
				callback.onResponse(response);
			}

			@Override
			public void onFailure(KinEcosystemException exception) {
				deleteImportedAccount(accountIndex);
				callback.onFailure(exception);
				Logger.log(new Log().priority(Log.ERROR).withTag(TAG).put("switchAccount", "ended with failure"));
			}
		});
	}

	private void deleteImportedAccount(int accountIndex) {
		try {
			Logger.log(new Log().withTag(TAG).put("deleteImportedAccount", "account index = " + accountIndex));
			blockchainSource.deleteAccount(accountIndex);
		} catch (DeleteAccountException e) {
			Logger.log(new Log().priority(Log.ERROR).withTag(TAG).put("deleteImportedAccount", "error " + e));
		}
	}

	private IKinAccount getKinAccount() {
		return blockchainSource.getKinAccount();
	}

	private void removeAccountCreationRegistration() {
		if (accountCreationRegistration != null) {
			accountCreationRegistration.remove();
			accountCreationRegistration = null;
		}
	}

	private boolean isValidState(int currentState, int newState) {
		return newState == ERROR || currentState == ERROR || newState >= currentState
			//allow recreating an account in case of switching account after restore
			|| (currentState == CREATION_COMPLETED && newState == REQUIRE_CREATION);
	}

	@Override
	public KinEcosystemException getError() {
		return error;
	}

}
