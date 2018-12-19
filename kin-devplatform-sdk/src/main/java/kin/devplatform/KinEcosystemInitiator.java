package kin.devplatform;

import static kin.devplatform.exception.ClientException.SDK_NOT_STARTED;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
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
	private final ExecutorsUtil executorsUtil;
	private volatile boolean isInitialized = false;
	private volatile boolean isAccountLoggedIn = false;

	public KinEcosystemInitiator(ExecutorsUtil executorsUtil) {
		this.executorsUtil = executorsUtil;
	}

	public void init(Context context) {
		init(context, null);
	}

	public void init(Context context, KinEnvironment environment) {
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

			eventLogger.send(KinSdkInitiated.create());
			isInitialized = true;
		}
	}

	public void start(@NonNull SignInData signInData, final KinCallback<Void> loginCallback) {
		if (!isInitialized) {
			fireStartError(ErrorUtil.getClientException(SDK_NOT_STARTED, null), loginCallback);
		}
		if (isAccountLoggedIn) {
			fireStartCompleted(loginCallback);
			return;
		}

		String publicAddress = null;
		try {
			BlockchainSourceImpl.getInstance().createAccount();
			publicAddress = BlockchainSourceImpl.getInstance().getPublicAddress();
		} catch (final BlockchainException exception) {
			fireStartError(exception, loginCallback);
		}

		String deviceID = AuthRepository.getInstance().getDeviceID();
		signInData.setDeviceId(deviceID != null ? deviceID : UUID.randomUUID().toString());
		signInData.setWalletAddress(publicAddress);
		AuthRepository.getInstance().setSignInData(signInData);

		ObservableData<String> observableData = AuthRepository.getInstance().getAppID();
		String appID = observableData.getValue();
		if (appID == null) {
			observableData.addObserver(new Observer<String>() {
				@Override
				public void onChanged(String appID) {
					BlockchainSourceImpl.getInstance().setAppID(appID);
					AuthRepository.getInstance().getAppID().removeObserver(this);
				}
			});
		}
		BlockchainSourceImpl.getInstance().setAppID(appID);

		login(loginCallback);
	}

	private void login(final KinCallback<Void> loginCallback) {
		AuthRepository.getInstance().getAuthToken(new KinCallback<AuthToken>() {
			@Override
			public void onResponse(AuthToken response) {
				final AccountManager accountManager = AccountManagerImpl.getInstance();
				if (accountManager.isAccountCreated()) {
					fireStartCompleted(loginCallback);
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
		final Handler handler = new Handler(Looper.getMainLooper());
		final Observer<Integer> accountStateObserver = new Observer<Integer>() {
			@Override
			public void onChanged(@AccountState Integer value) {
				if (value == AccountManager.ERROR) {
					handler.removeCallbacksAndMessages(null);
					fireStartError(accountManager.getError(), loginCallback);
					accountManager.removeAccountStateObserver(this);
				} else if (value == AccountManager.CREATION_COMPLETED) {
					handler.removeCallbacksAndMessages(null);
					fireStartCompleted(loginCallback);
					accountManager.removeAccountStateObserver(this);
				}
			}
		};

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				accountManager.removeAccountStateObserver(accountStateObserver);
				fireStartError(ErrorUtil.createCreateAccountTimeoutException(), loginCallback);
			}
		}, 30000);
		accountManager.addAccountStateObserver(accountStateObserver);
		accountManager.start();
	}


	private void fireStartCompleted(final KinCallback<Void> loginCallback) {
		isAccountLoggedIn = true;
		executorsUtil.mainThread().execute(new Runnable() {
			@Override
			public void run() {
				loginCallback.onResponse(null);
			}
		});
	}

	private void fireStartError(final KinEcosystemException exception, final KinCallback<Void> loginCallback) {
		isAccountLoggedIn = false;
		executorsUtil.mainThread().execute(new Runnable() {
			@Override
			public void run() {
				loginCallback.onFailure(exception);
			}
		});
	}

	public boolean isInitialized() {
		return isInitialized;
	}

	public boolean isAccountLoggedIn() {
		return isAccountLoggedIn;
	}
}
