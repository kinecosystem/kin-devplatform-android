package kin.devplatform;

import static kin.devplatform.exception.ClientException.SDK_NOT_STARTED;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.UUID;9

import kin.devplatform.accountmanager.AccountManagerImpl;
import kin.devplatform.accountmanager.AccountManagerLocal;
import kin.devplatform.base.ObservableData;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.EventLoggerImpl;
import kin.devplatform.bi.events.EntrypointButtonTapped;
import kin.devplatform.bi.events.KinSdkInitiated;
import kin.devplatform.core.util.DeviceUtils;
import kin.devplatform.core.util.ExecutorsUtil;
import kin.devplatform.data.auth.AuthLocalData;
import kin.devplatform.data.auth.AuthRemoteData;
import kin.devplatform.data.auth.AuthRepository;
import kin.devplatform.data.blockchain.BlockchainSourceImpl;
import kin.devplatform.data.blockchain.BlockchainSourceLocal;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.model.OrderConfirmation;
import kin.devplatform.data.model.WhitelistData;
import kin.devplatform.data.offer.OfferRemoteData;
import kin.devplatform.data.offer.OfferRepository;
import kin.devplatform.data.order.OrderLocalData;
import kin.devplatform.data.order.OrderRemoteData;
import kin.devplatform.data.order.OrderRepository;
import kin.devplatform.exception.BlockchainException;
import kin.devplatform.exception.ClientException;
import kin.devplatform.main.view.EcosystemActivity;
import kin.devplatform.marketplace.model.NativeSpendOffer;
import kin.devplatform.network.model.SignInData;
import kin.devplatform.network.model.SignInData.SignInTypeEnum;
import kin.devplatform.network.model.WhitelistService;
import kin.devplatform.splash.view.SplashViewActivity;
import kin.devplatform.util.ErrorUtil;
import kin.sdk.migration.IKinClient;
import kin.sdk.migration.KinVersionProvider;
import kin.sdk.migration.MigrationManager;


public class Kin {

	private static final String KIN_ECOSYSTEM_STORE_PREFIX_KEY = "kinecosystem_store";
	private static Kin instance;

	private final ExecutorsUtil executorsUtil;
	private EventLogger eventLogger;

	private Kin() {
		executorsUtil = new ExecutorsUtil();
	}

	private static Kin getInstance() {
		if (instance == null) {
			synchronized (Kin.class) {
				instance = new Kin();
			}
		}

		return instance;
	}

	public static void start(@NonNull Context appContext, @NonNull WhitelistData whitelistData,
		@NonNull KinEnvironment environment)
		throws ClientException, BlockchainException {
		if (isInstanceNull()) {
			SignInData signInData = getWhiteListSignInData(whitelistData);
			init(appContext, signInData, environment);
		}
	}

	public static void start(@NonNull Context appContext, @NonNull String jwt, @NonNull KinEnvironment environment)
		throws ClientException, BlockchainException {
		if (isInstanceNull()) {
			SignInData signInData = getJwtSignInData(jwt);
			init(appContext, signInData, environment);
		}
	}

	private static SignInData getWhiteListSignInData(@NonNull final WhitelistData whitelistData) {
		return new SignInData()
			.signInType(SignInTypeEnum.WHITELIST)
			.userId(whitelistData.getUserID())
			.appId(whitelistData.getAppID())
			.apiKey(whitelistData.getApiKey());
	}

	public static void enableLogs(final boolean enableLogs) {
		Logger.enableLogs(enableLogs);
	}

	private static SignInData getJwtSignInData(@NonNull final String jwt) {
		return new SignInData()
			.signInType(SignInTypeEnum.JWT)
			.jwt(jwt);
	}

	private static void init(@NonNull Context appContext, @NonNull SignInData signInData,
		@NonNull KinEnvironment environment) throws ClientException, BlockchainException {
		instance = getInstance();
		appContext = appContext.getApplicationContext(); // use application context to avoid leaks.
		DeviceUtils.init(appContext);
		Configuration.setEnvironment(environment);
		initEventLogger();
		initBlockchain(appContext);
		registerAccount(appContext, signInData);
		initEventCommonData(appContext);
		instance.eventLogger.send(KinSdkInitiated.create());
		initAccountManager(appContext);
		initOrderRepository(appContext);
		initOfferRepository();
		setAppID();
	}

	private static void initAccountManager(@NonNull final Context context) {
		AccountManagerImpl
			.init(AccountManagerLocal.getInstance(context), instance.eventLogger, AuthRepository.getInstance(),
				BlockchainSourceImpl.getInstance());
		if (!AccountManagerImpl.getInstance().isAccountCreated()) {
			AccountManagerImpl.getInstance().start();
		}
	}

	private static void initEventLogger() {
		instance.eventLogger = EventLoggerImpl.getInstance();
	}

	private static void initEventCommonData(@NonNull Context context) {
		EventCommonDataUtil.setBaseData(context);
	}

	private static void setAppID() {
		ObservableData<String> observableData = AuthRepository.getInstance().getAppID();
		String appID = observableData.getValue();
		observableData.addObserver(new Observer<String>() {
			@Override
			public void onChanged(String appID) {
				BlockchainSourceImpl.getInstance().setAppID(appID);
			}
		});

		BlockchainSourceImpl.getInstance().setAppID(appID);
	}

	private static void initBlockchain(Context context) throws BlockchainException {
		final String networkUrl = Configuration.getEnvironment().getBlockchainNetworkUrl();
		final String networkId = Configuration.getEnvironment().getBlockchainPassphrase();
		IKinClient kinClient = new MigrationManager(context, appId, networkUrl, networkId, Configuration.getEnvironment().getIssuer(
				new KinVersionProvider() {
					@Override
					public boolean isKinSdkVersion() {
						return false;
					}
				},
				new WhitelistService(),
				KIN_ECOSYSTEM_STORE_PREFIX_KEY) {
				@Override
				protected String getIssuerAccountId() {
					return Configuration.getEnvironment().getIssuer();
				}


		BlockchainSourceImpl.init(instance.eventLogger, kinClient, BlockchainSourceLocal.getInstance(context));
	}

	private static void registerAccount(@NonNull final Context context, @NonNull final SignInData signInData)
		throws ClientException {
		AuthRepository.init(AuthLocalData.getInstance(context, instance.executorsUtil),
			AuthRemoteData.getInstance(instance.executorsUtil));
		String deviceID = AuthRepository.getInstance().getDeviceID();
		signInData.setDeviceId(deviceID != null ? deviceID : UUID.randomUUID().toString());
		signInData.setWalletAddress(getPublicAddress());
		AuthRepository.getInstance().setSignInData(signInData);
	}

	private static void initOfferRepository() {
		OfferRepository.init(OfferRemoteData.getInstance(instance.executorsUtil), OrderRepository.getInstance());
	}

	private static void initOrderRepository(@NonNull final Context context) {
		OrderRepository.init(BlockchainSourceImpl.getInstance(),
			instance.eventLogger,
			OrderRemoteData.getInstance(instance.executorsUtil),
			OrderLocalData.getInstance(context, instance.executorsUtil));
	}

	private static boolean isInstanceNull() {
		return instance == null;
	}

	private static void checkInstanceNotNull() throws ClientException {
		if (isInstanceNull()) {
			throw ErrorUtil.getClientException(SDK_NOT_STARTED,
				new IllegalStateException("Kin.start(...) should be called first"));
		}
	}

	/**
	 * Launch Kin Marketplace if the user is activated, otherwise it will launch Welcome to Kin page.
	 *
	 * @param activity the activity user can go back to.
	 */
	public static void launchMarketplace(@NonNull final Activity activity) throws ClientException {
		checkInstanceNotNull();
		instance.eventLogger.send(EntrypointButtonTapped.create());
		boolean isActivated = AuthRepository.getInstance().isActivated();
		if (isActivated) {
			navigateToMarketplace(activity);
		} else {
			navigateToSplash(activity);
		}
	}

	private static void navigateToSplash(@NonNull final Activity activity) {
		activity.startActivity(new Intent(activity, SplashViewActivity.class));
		activity.overridePendingTransition(R.anim.kinecosystem_slide_in_right, R.anim.kinecosystem_slide_out_left);
	}

	private static void navigateToMarketplace(@NonNull final Activity activity) {
		activity.startActivity(new Intent(activity, EcosystemActivity.class));
		activity.overridePendingTransition(R.anim.kinecosystem_slide_in_right, R.anim.kinecosystem_slide_out_left);
	}

	/**
	 * @return The account public address
	 */
	public static String getPublicAddress() throws ClientException {
		checkInstanceNotNull();
		return BlockchainSourceImpl.getInstance().getPublicAddress();
	}

	/**
	 * Get the cached balance, can be different from the current balance on the network.
	 *
	 * @return balance amount
	 */
	public static Balance getCachedBalance() throws ClientException {
		checkInstanceNotNull();
		return BlockchainSourceImpl.getInstance().getBalance();
	}

	/**
	 * Get the current account balance from the network.
	 *
	 * @param callback balance amount
	 */
	public static void getBalance(@NonNull final KinCallback<Balance> callback) throws ClientException {
		checkInstanceNotNull();
		BlockchainSourceImpl.getInstance().getBalance(callback);
	}

	/**
	 * Add balance observer to start getting notified when the balance is changed on the blockchain network. On balance
	 * changes you will get {@link Balance} with the balance amount.
	 *
	 * Take in consideration that on adding this observer, a live network connection will be open to the blockchain
	 * network, In order to close the connection use {@link #removeBalanceObserver(Observer)} with the same observer. If
	 * no other observers on this connection, the connection will be closed.
	 */
	public static void addBalanceObserver(@NonNull final Observer<Balance> observer) throws ClientException {
		checkInstanceNotNull();
		BlockchainSourceImpl.getInstance().addBalanceObserverAndStartListen(observer);
	}

	/**
	 * Remove the balance observer, this method will close the live network connection to the blockchain network if
	 * there is no more observers.
	 */
	public static void removeBalanceObserver(@NonNull final Observer<Balance> observer) throws ClientException {
		checkInstanceNotNull();
		BlockchainSourceImpl.getInstance().removeBalanceObserverAndStopListen(observer);
	}

	/**
	 * Allowing your users to purchase virtual goods you define within your app, using KIN. This call might take time,
	 * due to transaction validation on the blockchain network.
	 *
	 * @param offerJwt Represents a 'Spend' offer in a JWT manner.
	 * @param callback {@link OrderConfirmation} the result will be a failure or a succeed with a jwt confirmation.
	 */
	public static void purchase(String offerJwt, @Nullable KinCallback<OrderConfirmation> callback)
		throws ClientException {
		checkInstanceNotNull();
		OrderRepository.getInstance().purchase(offerJwt, callback);
	}

	/**
	 * Allowing a user to pay to a different user for an offer defined within your app, using KIN. This call might take
	 * time, due to transaction validation on the blockchain network.
	 *
	 * @param offerJwt Represents a 'Pay to user' offer in a JWT manner.
	 * @param callback {@link OrderConfirmation} the result will be a failure or a succeed with a jwt confirmation.
	 */
	public static void payToUser(String offerJwt, @Nullable KinCallback<OrderConfirmation> callback)
		throws ClientException {
		checkInstanceNotNull();
		OrderRepository.getInstance().payToUser(offerJwt, callback);
	}

	/**
	 * Allowing your users to earn Kin as a reward for native task you define. This call might take time, due to
	 * transaction validation on the blockchain network.
	 *
	 * @param offerJwt the offer details represented in a JWT manner.
	 * @param callback after validating the info and sending the payment to the user, you will receive {@link
	 * OrderConfirmation}, with the jwtConfirmation and you can validate the order when the order status is completed.
	 */
	public static void requestPayment(String offerJwt, @Nullable KinCallback<OrderConfirmation> callback)
		throws ClientException {
		checkInstanceNotNull();
		OrderRepository.getInstance().requestPayment(offerJwt, callback);
	}

	/**
	 * Returns a {@link OrderConfirmation}, with the order status and a jwtConfirmation if the order is completed.
	 *
	 * @param offerID The offerID that this order created from
	 */
	public static void getOrderConfirmation(@NonNull String offerID, @NonNull KinCallback<OrderConfirmation> callback)
		throws ClientException {
		checkInstanceNotNull();
		OrderRepository.getInstance().getExternalOrderStatus(offerID, callback);
	}

	/**
	 * Add a native offer {@link Observer} to receive a trigger when you native offers on Kin Marketplace are clicked.
	 */
	public static void addNativeOfferClickedObserver(@NonNull Observer<NativeSpendOffer> observer)
		throws ClientException {
		checkInstanceNotNull();
		OfferRepository.getInstance().addNativeOfferClickedObserver(observer);
	}

	/**
	 * Remove the callback if you no longer want to get triggered when your offer on Kin marketplace are clicked.
	 */
	public static void removeNativeOfferClickedObserver(@NonNull Observer<NativeSpendOffer> observer)
		throws ClientException {
		checkInstanceNotNull();
		OfferRepository.getInstance().removeNativeOfferClickedObserver(observer);
	}

	/**
	 * Adds an {@link NativeSpendOffer} to spend offer list on Kin Marketplace activity. The offer will be added at
	 * index 0 in the spend list.
	 *
	 * @param nativeSpendOffer The spend offer you want to add to the spend list.
	 * @return true if the offer added successfully, the list was changed.
	 * @throws ClientException Could not add the offer to the list.
	 */
	public static boolean addNativeOffer(@NonNull NativeSpendOffer nativeSpendOffer) throws ClientException {
		checkInstanceNotNull();
		return OfferRepository.getInstance().addNativeOffer(nativeSpendOffer);
	}

	/**
	 * Removes a {@link NativeSpendOffer} from the spend list on Kin Marketplace activity.
	 *
	 * @param nativeSpendOffer The spend offer you want to remove from the spend list.
	 * @return true if the offer removed successfully, the list was changed.
	 * @throws ClientException Could not remove the offer from the list.
	 */
	public static boolean removeNativeOffer(@NonNull NativeSpendOffer nativeSpendOffer) throws ClientException {
		checkInstanceNotNull();
		return OfferRepository.getInstance().removeNativeOffer(nativeSpendOffer);
	}
}
