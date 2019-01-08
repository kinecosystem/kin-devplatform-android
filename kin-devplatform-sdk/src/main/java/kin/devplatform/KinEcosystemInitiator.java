package kin.devplatform;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import java.util.UUID;
import kin.core.KinClient;
import kin.core.ServiceProvider;
import kin.devplatform.accountmanager.AccountManager;
import kin.devplatform.accountmanager.AccountManager.AccountState;
import kin.devplatform.accountmanager.AccountManagerImpl;
import kin.devplatform.accountmanager.AccountManagerLocal;
import kin.devplatform.base.ObservableData;
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
import kin.devplatform.network.model.SignInData;
import kin.devplatform.util.ErrorUtil;

public final class KinEcosystemInitiator {

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
			init(context, null);
		} catch (BlockchainException e) {
			EventLoggerImpl.getInstance().send(GeneralEcosystemSdkError.create(
				ErrorUtil.getPrintableStackTrace(e), String.valueOf(e.getCode()),
				"KinEcosystemInitiator internalInit failed"
			));
			e.printStackTrace();
		}
	}

	/**
	 * Uses for external (public API) initialization and jwt login.
	 */
	public void externalInit(Context context, KinEnvironment environment, @NonNull SignInData signInData,
		final KinCallback<Void> loginCallback) {
		if (isInitialized && isLoggedIn) {
			fireStartCompleted(loginCallback);
			return;
		}

		try {
			init(context, environment);
		} catch (BlockchainException e) {
			fireStartError(e, loginCallback);
			return;
		}
		login(signInData, loginCallback);
	}

	private void init(Context context, KinEnvironment environment) throws BlockchainException {
		if (!isInitialized) {
			ConfigurationLocal configurationLocal = ConfigurationLocal.getInstance(context);
			if (environment != null) {
				configurationLocal.setEnvironment(environment);
			}
			ConfigurationImpl.init(configurationLocal);
			KinEnvironment kinEnvironment = ConfigurationImpl.getInstance().getEnvironment();
			EventLogger eventLogger = EventLoggerImpl.getInstance();
			final String networkUrl = kinEnvironment.getBlockchainNetworkUrl();
			final String networkId = kinEnvironment.getBlockchainPassphrase();
			final String issuer = kinEnvironment.getIssuer();

			KinClient kinClient = new KinClient(context, new ServiceProvider(networkUrl, networkId) {
				@Override
				protected String getIssuerAccountId() {
					return issuer;
				}
			}, KIN_ECOSYSTEM_STORE_PREFIX_KEY);
			BlockchainSourceImpl.init(eventLogger, kinClient, BlockchainSourceLocal.getInstance(context));

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

			setAppID();

			eventLogger.send(KinSdkInitiated.create());
			isInitialized = true;
		}
	}

	private void setAppID() {
		ObservableData<String> observableData = AuthRepository.getInstance().getAppID();
		String appID = observableData.getValue();
		if (appID == null) {
			observableData.addObserver(new Observer<String>() {
				@Override
				public void onChanged(String appID) {
					if (!TextUtils.isEmpty(appID)) {
						BlockchainSourceImpl.getInstance().setAppID(appID);
						AuthRepository.getInstance().getAppID().removeObserver(this);
					}
				}
			});
		}
		BlockchainSourceImpl.getInstance().setAppID(appID);
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
