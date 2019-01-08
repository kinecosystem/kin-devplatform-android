package kin.devplatform;

import android.content.Context;
import android.support.annotation.NonNull;
import java.util.UUID;

import kin.devplatform.accountmanager.AccountManager;
import kin.devplatform.accountmanager.AccountManager.AccountState;
import kin.devplatform.accountmanager.AccountManagerImpl;
import kin.devplatform.accountmanager.AccountManagerLocal;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.EventLoggerImpl;
import kin.devplatform.bi.events.GeneralEcosystemSdkError;
import kin.devplatform.bi.events.KinSdkInitiated;
import kin.devplatform.core.util.DeviceUtils;
import kin.devplatform.core.util.ExecutorsUtil;
import kin.devplatform.data.auth.AuthLocalData;
import kin.devplatform.data.auth.AuthRemoteData;
import kin.devplatform.data.auth.AuthRepository;
import kin.devplatform.data.blockchain.BlockchainSourceImpl;
import kin.devplatform.data.blockchain.BlockchainSourceLocal;
import kin.devplatform.data.offer.OfferRemoteData;
import kin.devplatform.data.offer.OfferRepository;
import kin.devplatform.data.order.OrderLocalData;
import kin.devplatform.data.order.OrderRemoteData;
import kin.devplatform.data.order.OrderRepository;
import kin.devplatform.exception.BlockchainException;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.network.model.AuthToken;
import kin.devplatform.network.model.KinVersionProvider;
import kin.devplatform.network.model.SignInData;
import kin.devplatform.util.ErrorUtil;
import kin.sdk.migration.MigrationManager;
import kin.sdk.migration.MigrationNetworkInfo;
import kin.sdk.migration.exception.MigrationInProcessException;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.interfaces.MigrationManagerListener;

public final class KinEcosystemInitiator {

	private static final String TAG = KinEcosystemInitiator.class.getSimpleName();
	private static final String KIN_ECOSYSTEM_STORE_PREFIX_KEY = "kinecosystem_store";
	private static KinEcosystemInitiator instance;

	private final ExecutorsUtil executorsUtil;
	private volatile boolean isInitialized = false;
	private volatile boolean isLoggedIn = false;

	public static KinEcosystemInitiator getInstance() {
		if (instance == null) {
			synchronized (KinEcosystemInitiator.class) {
				if (instance == null) {
					instance = new KinEcosystemInitiator();
				}
			}
		}
		return instance;
	}

	private KinEcosystemInitiator() {
		this.executorsUtil = new ExecutorsUtil();
	}

	/**
	 * Uses for internal initialization after process restart
	 */
	public void internalInit(Context context) {
		try {
			init(context, null, null, false, null, null);
		} catch (BlockchainException e) {
			EventLoggerImpl.getInstance().send(GeneralEcosystemSdkError.create(
				ErrorUtil.getPrintableStackTrace(e), String.valueOf(e.getCode()), // TODO: 02/01/2019 why not firing some error? and why print the stack trace
				"KinEcosystemInitiator internalInit failed"
			));
			e.printStackTrace();
		}
	}

	/**
	 * Uses for external (public API) initialization and jwt login.
	 */
	public void externalInit(Context context, String appId, KinEnvironment environment, @NonNull SignInData signInData,
							 final KinCallback<Void> loginCallback) {
		if (isInitialized && isLoggedIn) {
			fireStartCompleted(loginCallback);
			return;
		}

		try {
			init(context, appId, environment, true, signInData, loginCallback);
		} catch (BlockchainException e) {
			fireStartError(e, loginCallback);
		}
	}

	private void init(Context context, String appId, KinEnvironment environment, boolean withLogin,
					  SignInData signInData, KinCallback<Void> loginCallback) throws BlockchainException {
		if (!isInitialized) {
			ConfigurationLocal configurationLocal = ConfigurationLocal.getInstance(context);
			if (environment != null) {
				configurationLocal.setEnvironment(environment);
			}

			ConfigurationImpl.init(configurationLocal);
			KinEnvironment kinEnvironment = ConfigurationImpl.getInstance().getEnvironment();
			final EventLogger eventLogger = EventLoggerImpl.getInstance();
			final String oldNetworkUrl = kinEnvironment.getOldBlockchainNetworkUrl();
			final String oldNetworkId = kinEnvironment.getOldBlockchainPassphrase();
			final String newNetworkUrl = kinEnvironment.getNewBlockchainNetworkUrl();
			final String newNetworkId = kinEnvironment.getNewBlockchainPassphrase();
			final String issuer = kinEnvironment.getIssuer();

			// TODO: 01/01/2019 is it ok to use issuer like i did or like they did in the past like this:
//			new ServiceProvider(networkUrl, networkId) {
//				@Override
//				protected String getIssuerAccountId() {
//					return issuer;
//				}

			MigrationNetworkInfo migrationNetworkInfo = new MigrationNetworkInfo(oldNetworkUrl, oldNetworkId, newNetworkUrl, newNetworkId, issuer);
			MigrationManager migrationManager = new MigrationManager(context, appId, migrationNetworkInfo, new KinVersionProvider(), KIN_ECOSYSTEM_STORE_PREFIX_KEY);
			try {
				handleMigration(context, appId, eventLogger, migrationManager, withLogin, signInData, loginCallback);
			} catch (MigrationInProcessException e) {
				Logger.log(new Log().priority(Log.WARN).withTag(TAG).text("Start migration when it was already in migration process"));
			}
		}
	}

	private void handleMigration(final Context context, final String appId, final EventLogger eventLogger, MigrationManager migrationManager,
								 final boolean withLogin, final SignInData signInData, final KinCallback<Void> loginCallback) throws MigrationInProcessException {
		migrationManager.start(new MigrationManagerListener() {
			@Override
			public void onMigrationStart() {
				// TODO: 06/01/2019 when implement the features we talked about with Ayelet then add here the onMigrationStarted for the external developers to use.
			}

			@Override
			public void onReady(IKinClient kinClient) {
				try {
					String applicationId = appId;
					BlockchainSourceImpl.init(eventLogger, kinClient, BlockchainSourceLocal.getInstance(context));
					if (applicationId != null) {
						BlockchainSourceImpl.getInstance().setAppID(applicationId);
					}

					applicationId = BlockchainSourceImpl.getInstance().getAppId();

					AuthRepository
							.init(AuthLocalData.getInstance(context, executorsUtil),
									AuthRemoteData.getInstance(executorsUtil), applicationId);

					EventCommonDataUtil.setBaseData(context);

					AccountManagerImpl
							.init(AccountManagerLocal.getInstance(context), eventLogger, AuthRepository.getInstance(),
									BlockchainSourceImpl.getInstance());

					OrderRepository.init(BlockchainSourceImpl.getInstance(),
							eventLogger,
							OrderRemoteData.getInstance(executorsUtil),
							OrderLocalData.getInstance(context, executorsUtil));

					OfferRepository
							.init(OfferRemoteData.getInstance(executorsUtil), OrderRepository.getInstance());

					DeviceUtils.init(context);

					eventLogger.send(KinSdkInitiated.create());
					isInitialized = true;
					if (withLogin) {
						login(signInData, loginCallback);
					}
				} catch (BlockchainException e) {
					onError(e);
				}
			}

			@Override
			public void onError(Exception e) {
				// TODO: 06/01/2019 when implement the features we talked about with Ayelet then add here the onError for the external developers to use.
				// TODO: 06/01/2019 Also add the meaniningful error that we talked about.
			}
		});
	}

	private void login(@NonNull SignInData signInData, final KinCallback<Void> loginCallback) {
		setAuthRepositoryData(signInData);
		performLogin(loginCallback);
	}

	private void setAuthRepositoryData(@NonNull SignInData signInData) {
		String publicAddress = BlockchainSourceImpl.getInstance().getPublicAddress();
		String deviceID = AuthRepository.getInstance().getDeviceID();
		signInData.setDeviceId(deviceID != null ? deviceID : UUID.randomUUID().toString());
		signInData.setWalletAddress(publicAddress);
		AuthRepository.getInstance().setSignInData(signInData);
	}

	private void performLogin(final KinCallback<Void> loginCallback) {
		AuthRepository.getInstance().getAuthToken(new KinCallback<AuthToken>() {
			@Override
			public void onResponse(AuthToken response) {
				final AccountManager accountManager = AccountManagerImpl.getInstance();
				if (accountManager.isAccountCreated()) {
					activateAccount(loginCallback);
				} else {
					handleAccountNotCreatedState(accountManager, loginCallback);
				}
			}

			@Override
			public void onFailure(final KinEcosystemException exception) {
				fireStartError(exception, loginCallback);
			}
		});
	}

	private void handleAccountNotCreatedState(final AccountManager accountManager,
		final KinCallback<Void> loginCallback) {
		final Observer<Integer> accountStateObserver = new Observer<Integer>() {
			@Override
			public void onChanged(@AccountState Integer value) {
				if (value == AccountManager.ERROR) {
					accountManager.removeAccountStateObserver(this);
					fireStartError(accountManager.getError(), loginCallback);
				} else if (value == AccountManager.CREATION_COMPLETED) {
					accountManager.removeAccountStateObserver(this);
					activateAccount(loginCallback);
				}
			}
		};
		accountManager.addAccountStateObserver(accountStateObserver);
		accountManager.start();
	}

	private void activateAccount(final KinCallback<Void> callback) {
		if (AuthRepository.getInstance().isActivated()) {
			fireStartCompleted(callback);
		} else {
			AuthRepository.getInstance().activateAccount(new KinCallback<Void>() {
				@Override
				public void onResponse(Void response) {
					fireStartCompleted(callback);
				}

				@Override
				public void onFailure(KinEcosystemException error) {
					fireStartError(error, callback);
				}
			});
		}
	}

	private void fireStartCompleted(final KinCallback<Void> loginCallback) {
		isLoggedIn = true;
		executorsUtil.mainThread().execute(new Runnable() {
			@Override
			public void run() {
				loginCallback.onResponse(null);
			}
		});
	}

	private void fireStartError(final KinEcosystemException exception, final KinCallback<Void> loginCallback) {
		isLoggedIn = false;
		executorsUtil.mainThread().execute(new Runnable() {
			@Override
			public void run() {
				loginCallback.onFailure(exception);
			}
		});
	}

	public boolean isInitialized() {
		return isInitialized && isLoggedIn;
	}

}
