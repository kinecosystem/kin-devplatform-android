package kin.devplatform;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
import kin.sdk.migration.KinSdkVersion;
import kin.sdk.migration.MigrationManager;
import kin.sdk.migration.MigrationNetworkInfo;
import kin.sdk.migration.bi.IMigrationEventsListener;
import kin.sdk.migration.exception.MigrationInProcessException;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.interfaces.IMigrationManagerCallbacks;

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
	public void internalInit(
		Context context) {
		if (!isInitialized) {
			initConfiguration(context, null);
			MigrationManager migrationManager = getMigrationManager(context, null);
			try {
				handleKinClientReady(migrationManager.getCurrentKinClient(), context, null, null,
					false, null, null);
			} catch (BlockchainException e) {
				EventLoggerImpl.getInstance().send(GeneralEcosystemSdkError.create(
					ErrorUtil.getPrintableStackTrace(e), String.valueOf(e.getCode()),
					"KinEcosystemInitiator internalInit failed"
				));
				e.printStackTrace();
			}
		}
	}

	/**
	 * Uses for external (public API) initialization and jwt login.
	 */
	public void externalInit(Context context, String appId, KinEnvironment environment, @NonNull SignInData signInData,
		final KinCallback<Void> loginCallback, @Nullable final KinMigrationListener migrationProcessCallback) {
		if (isInitialized && isLoggedIn) {
			fireStartCompleted(loginCallback);
			return;
		}
		if (appId != null) {
			signInData.setAppId(appId);
		}
		init(context, appId, environment, signInData, loginCallback, migrationProcessCallback);
		// If initialized then do the login and if not then will do login at end of migration which happens inside init.
		if (isInitialized) {
			login(signInData, loginCallback);
		}
	}

	private void init(Context context, String appId, KinEnvironment environment,
		SignInData signInData, KinCallback<Void> loginCallback, KinMigrationListener migrationCallback) {
		if (!isInitialized) {
			initConfiguration(context, environment);
			MigrationManager migrationManager = getMigrationManager(context, appId);
			try {
				handleMigration(context, appId, migrationManager, signInData, loginCallback, migrationCallback);
			} catch (MigrationInProcessException e) {
				Logger.log(new Log().priority(Log.WARN).withTag(TAG)
					.text("Start migration when it was already in migration process"));
			}
		}
	}

	private MigrationManager getMigrationManager(Context context, String appId) {
		KinEnvironment kinEnvironment = ConfigurationImpl.getInstance().getEnvironment();
		final String oldNetworkUrl = kinEnvironment.getOldBlockchainNetworkUrl();
		final String oldNetworkId = kinEnvironment.getOldBlockchainPassphrase();
		final String newNetworkUrl = kinEnvironment.getNewBlockchainNetworkUrl();
		final String newNetworkId = kinEnvironment.getNewBlockchainPassphrase();
		final String issuer = kinEnvironment.getOldBlockchainIssuer();

		if (appId == null) {
			appId = BlockchainSourceLocal.getInstance(context).getAppId();
		}

		MigrationNetworkInfo migrationNetworkInfo = new MigrationNetworkInfo(oldNetworkUrl, oldNetworkId, newNetworkUrl,
			newNetworkId, issuer);
		return new MigrationManager(context, appId, migrationNetworkInfo, new KinVersionProvider(appId)
			, new IMigrationEventsListener() {

			@Override
			public void onMethodStarted() {

			}

			@Override
			public void onVersionCheckStarted() {

			}

			@Override
			public void onVersionCheckSucceeded(KinSdkVersion sdkVersion) {

			}

			@Override
			public void onVersionCheckFailed(Exception exception) {

			}

			@Override
			public void onCallbackStart() {

			}

			@Override
			public void onCheckBurnStarted(String publicAddress) {

			}

			@Override
			public void onCheckBurnSucceeded(String publicAddress, CheckBurnReason reason) {

			}

			@Override
			public void onCheckBurnFailed(String publicAddress, Exception exception) {

			}

			@Override
			public void onBurnStarted(String publicAddress) {

			}

			@Override
			public void onBurnSucceeded(String publicAddress, BurnReason reason) {

			}

			@Override
			public void onBurnFailed(String publicAddress, Exception exception) {

			}

			@Override
			public void onRequestAccountMigrationStarted(String publicAddress) {

			}

			@Override
			public void onRequestAccountMigrationSucceeded(String publicAddress,
				RequestAccountMigrationSuccessReason reason) {

			}

			@Override
			public void onRequestAccountMigrationFailed(String publicAddress, Exception exception) {

			}

			@Override
			public void onCallbackReady(KinSdkVersion sdkVersion, SelectedSdkReason selectedSdkReason) {

			}

			@Override
			public void onCallbackFailed(Exception exception) {

			}
		}, KIN_ECOSYSTEM_STORE_PREFIX_KEY);
	}

	private void initConfiguration(Context context, KinEnvironment environment) {
		ConfigurationLocal configurationLocal = ConfigurationLocal.getInstance(context);
		if (environment != null) {
			configurationLocal.setEnvironment(environment);
		}

		ConfigurationImpl.init(configurationLocal);
	}

	private void handleMigration(final Context context, final String appId, final MigrationManager migrationManager,
		final SignInData signInData,
		final KinCallback<Void> loginCallback, final KinMigrationListener migrationCallback)
		throws MigrationInProcessException {
		migrationManager.start(new IMigrationManagerCallbacks() {
			@Override
			public void onMigrationStart() {
				Logger.log(new Log().priority(Log.DEBUG).withTag(TAG).text("onMigrationStart"));
				if (migrationCallback != null) {
					migrationCallback.onStart();
				}
			}

			@Override
			public void onReady(IKinClient kinClient) {
				Logger.log(new Log().priority(Log.DEBUG).withTag(TAG).text("onReady"));
				try {
					handleKinClientReady(kinClient, context, appId, signInData, true, loginCallback, migrationCallback);
				} catch (BlockchainException e) {
					fireStartError(e, loginCallback);
				}
			}

			@Override
			public void onError(Exception e) {
				Logger.log(new Log().priority(Log.DEBUG).withTag(TAG).text("onError"));
				if (migrationCallback != null) {
					migrationCallback.onError(e);
				}
			}
		});
	}

	private void handleKinClientReady(IKinClient kinClient, Context context, String appId, SignInData signInData,
		boolean withLogin,
		KinCallback<Void> loginCallback, KinMigrationListener migrationCallback) throws BlockchainException {
		final EventLogger eventLogger = EventLoggerImpl.getInstance();
		if (migrationCallback != null) {
			migrationCallback.onFinish();
		}
		BlockchainSourceImpl.init(eventLogger, kinClient, BlockchainSourceLocal.getInstance(context));
		if (appId != null) {
			BlockchainSourceImpl.getInstance().setAppID(appId);
		}

		AuthRepository
			.init(AuthLocalData.getInstance(context, executorsUtil),
				AuthRemoteData.getInstance(executorsUtil));

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
