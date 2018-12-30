package kin.devplatform;

import static kin.devplatform.exception.ClientException.SDK_NOT_STARTED;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLoggerImpl;
import kin.devplatform.bi.events.EntrypointButtonTapped;
import kin.devplatform.data.auth.AuthRepository;
import kin.devplatform.data.blockchain.BlockchainSourceImpl;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.model.OrderConfirmation;
import kin.devplatform.data.offer.OfferRepository;
import kin.devplatform.data.order.OrderRepository;
import kin.devplatform.exception.BlockchainException;
import kin.devplatform.exception.ClientException;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.main.view.EcosystemActivity;
import kin.devplatform.marketplace.model.NativeOffer;
import kin.devplatform.marketplace.model.NativeSpendOffer;
import kin.devplatform.network.model.SignInData;
import kin.devplatform.network.model.SignInData.SignInTypeEnum;
import kin.devplatform.splash.view.SplashViewActivity;
import kin.devplatform.util.ErrorUtil;


public class Kin {

	public static void enableLogs(final boolean enableLogs) {
		Logger.enableLogs(enableLogs);
	}

	/**
	 * @deprecated use {@link #start(Context, String, KinEnvironment, KinCallback)} instead.
	 */
	@Deprecated
	public static void start(@NonNull Context appContext, @NonNull String jwt, @NonNull KinEnvironment environment)
		throws ClientException, BlockchainException {
		start(appContext, jwt, environment, new KinCallback<Void>() {
			@Override
			public void onResponse(Void response) {
			}

			@Override
			public void onFailure(KinEcosystemException error) {
			}
		});
	}

	/**
	 * Initialize, login and starts the SDK. Provide a callback to be notified when SDK is ready for use or some error
	 * happened in initialization process. This method can be called again (retry) in case of an error.
	 * <p></p>
	 * <p><b>Note:</b> SDK cannot be used before calling this method.</p>
	 * <p><b>Note:</b> This method is not thread safe, and shouldn't be called multiple times in parallel. callbacks will be fired on
	 * the main thread.</p>
	 * <b>Note:</b> The first call to this method will create Kin wallet account for the user on Kin blockchain, this
	 * process can take some time (couple of seconds), callback will be called only after account setup and creation will be
	 * completed.
	 *
	 * @param jwt 'register' jwt token required for authorized the user.
	 * @param callback success/failure callback
	 */
	public static void start(Context appContext, @NonNull String jwt, @NonNull KinEnvironment environment,
		KinCallback<Void> callback) {
		SignInData signInData = getJwtSignInData(jwt);
		KinEcosystemInitiator.getInstance().externalInit(appContext, environment, signInData, callback);
	}

	private static SignInData getJwtSignInData(@NonNull final String jwt) {
		return new SignInData()
			.signInType(SignInTypeEnum.JWT)
			.jwt(jwt);
	}

	private static void checkInitialized() throws ClientException {
		if (!KinEcosystemInitiator.getInstance().isInitialized()) {
			throw ErrorUtil.getClientException(SDK_NOT_STARTED, null);
		}
	}

	/**
	 * Launch Kin Marketplace if the user is activated, otherwise it will launch Welcome to Kin page.
	 *
	 * @param activity the activity user can go back to.
	 */
	public static void launchMarketplace(@NonNull final Activity activity) throws ClientException {
		checkInitialized();
		EventLoggerImpl.getInstance().send(EntrypointButtonTapped.create());
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
	 * @throws ClientException - sdk not initialized or account not logged in.
	 */
	public static String getPublicAddress() throws ClientException {
		checkInitialized();
		return BlockchainSourceImpl.getInstance().getPublicAddress();
	}

	/**
	 * Get the cached balance, can be different from the current balance on the network.
	 *
	 * @return balance amount
	 * @throws ClientException - sdk not initialized or account not logged in.
	 */
	public static Balance getCachedBalance() throws ClientException {
		checkInitialized();
		return BlockchainSourceImpl.getInstance().getBalance();
	}

	/**
	 * Get the current account balance from the network.
	 *
	 * @param callback balance amount
	 * @throws ClientException - sdk not initialized or account not logged in.
	 */
	public static void getBalance(@NonNull final KinCallback<Balance> callback) throws ClientException {
		checkInitialized();
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
		checkInitialized();
		BlockchainSourceImpl.getInstance().addBalanceObserverAndStartListen(observer);
	}

	/**
	 * Remove the balance observer, this method will close the live network connection to the blockchain network if
	 * there is no more observers.
	 *
	 * @throws ClientException - sdk not initialized.
	 */
	public static void removeBalanceObserver(@NonNull final Observer<Balance> observer) throws ClientException {
		checkInitialized();
		BlockchainSourceImpl.getInstance().removeBalanceObserverAndStopListen(observer);
	}

	/**
	 * Allowing your users to purchase virtual goods you define within your app, using KIN. This call might take time,
	 * due to transaction validation on the blockchain network.
	 *
	 * @param offerJwt Represents the offer in a JWT manner.
	 * @param callback {@link OrderConfirmation} The result will be a failure or a success with a jwt confirmation.
	 * @throws ClientException - sdk not initialized or account not logged in.
	 */
	public static void purchase(String offerJwt, @Nullable KinCallback<OrderConfirmation> callback)
		throws ClientException {
		checkInitialized();
		OrderRepository.getInstance().purchase(offerJwt, callback);
	}


	/**
	 * Allowing a user to pay to a different user for an offer defined within your app, using KIN. This call might take
	 * time, due to transaction validation on the blockchain network.
	 *
	 * @param offerJwt Represents a 'Pay to user' offer in a JWT manner.
	 * @param callback {@link OrderConfirmation} The result will be a failure or a success with a jwt confirmation.
	 * @throws ClientException - sdk not initialized or account not logged in.
	 */
	public static void payToUser(String offerJwt, @Nullable KinCallback<OrderConfirmation> callback)
		throws ClientException {
		checkInitialized();
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
		checkInitialized();
		OrderRepository.getInstance().requestPayment(offerJwt, callback);
	}

	/**
	 * Returns a {@link OrderConfirmation}, with the order status and a jwtConfirmation if the order is completed.
	 *
	 * @param offerID The offerID that this order created from
	 * @throws ClientException - sdk not initialized.
	 */
	public static void getOrderConfirmation(@NonNull String offerID, @NonNull KinCallback<OrderConfirmation> callback)
		throws ClientException {
		checkInitialized();
		OrderRepository.getInstance().getExternalOrderStatus(offerID, callback);
	}

	/**
	 * Add a native offer {@link Observer} to receive a trigger when you native offers on Kin Marketplace are clicked.
	 *
	 * @throws ClientException - sdk not initialized.
	 */
	public static void addNativeOfferClickedObserver(@NonNull Observer<NativeSpendOffer> observer)
		throws ClientException {
		checkInitialized();
		OfferRepository.getInstance().addNativeOfferClickedObserver(observer);
	}

	/**
	 * Remove the callback if you no longer want to get triggered when your offer on Kin marketplace are clicked.
	 *
	 * @throws ClientException - sdk not initialized.
	 */
	public static void removeNativeOfferClickedObserver(@NonNull Observer<NativeSpendOffer> observer)
		throws ClientException {
		checkInitialized();
		OfferRepository.getInstance().removeNativeOfferClickedObserver(observer);
	}

	/**
	 * Adds an {@link NativeOffer} to spend or earn offer list on Kin Marketplace activity. The offer will be added at
	 * index 0 in the spend list.
	 *
	 * @param nativeSpendOffer The spend offer you want to add to the spend list.
	 * @return true if the offer added successfully, the list was changed.
	 * @throws ClientException - sdk not initialized.
	 */
	public static boolean addNativeOffer(@NonNull NativeSpendOffer nativeSpendOffer) throws ClientException {
		checkInitialized();
		return OfferRepository.getInstance().addNativeOffer(nativeSpendOffer);
	}

	/**
	 * Removes a {@link NativeOffer} from the spend or earn list on Kin Marketplace activity.
	 *
	 * @param nativeSpendOffer The spend offer you want to remove from the spend list.
	 * @return true if the offer removed successfully, the list was changed.
	 * @throws ClientException - sdk not initialized.
	 */
	public static boolean removeNativeOffer(@NonNull NativeSpendOffer nativeSpendOffer) throws ClientException {
		checkInitialized();
		return OfferRepository.getInstance().removeNativeOffer(nativeSpendOffer);
	}
}