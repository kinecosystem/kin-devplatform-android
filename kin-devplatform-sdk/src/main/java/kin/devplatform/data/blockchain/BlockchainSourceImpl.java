package kin.devplatform.data.blockchain;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import com.kin.ecosystem.recovery.KeyStoreProvider;
import java.math.BigDecimal;
import kin.devplatform.KinCallback;
import kin.devplatform.Log;
import kin.devplatform.Logger;
import kin.devplatform.base.ObservableData;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.KinBalanceUpdated;
import kin.devplatform.bi.events.PayToUserTransactionBroadcastToBlockchainFailed;
import kin.devplatform.bi.events.PayToUserTransactionBroadcastToBlockchainSubmitted;
import kin.devplatform.bi.events.PayToUserTransactionBroadcastToBlockchainSucceeded;
import kin.devplatform.bi.events.SpendTransactionBroadcastToBlockchainFailed;
import kin.devplatform.bi.events.SpendTransactionBroadcastToBlockchainSubmitted;
import kin.devplatform.bi.events.SpendTransactionBroadcastToBlockchainSucceeded;
import kin.devplatform.bi.events.StellarKinTrustlineSetupFailed;
import kin.devplatform.bi.events.StellarKinTrustlineSetupSucceeded;
import kin.devplatform.core.util.ExecutorsUtil.MainThreadExecutor;
import kin.devplatform.data.blockchain.CreateTrustLineCall.TrustlineCallback;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.model.Payment;
import kin.devplatform.exception.BlockchainException;
import kin.devplatform.network.model.Offer.OfferType;
import kin.devplatform.network.model.OpenOrder;
import kin.devplatform.network.model.WhitelistService;
import kin.devplatform.util.ErrorUtil;
import kin.sdk.migration.common.KinSdkVersion;
import kin.sdk.migration.common.exception.CreateAccountException;
import kin.sdk.migration.common.exception.OperationFailedException;
import kin.sdk.migration.common.interfaces.IBalance;
import kin.sdk.migration.common.interfaces.IEventListener;
import kin.sdk.migration.common.interfaces.IKinAccount;
import kin.sdk.migration.common.interfaces.IKinClient;
import kin.sdk.migration.common.interfaces.IListenerRegistration;
import kin.sdk.migration.common.interfaces.IPaymentInfo;
import kin.utils.ResultCallback;

public class BlockchainSourceImpl implements BlockchainSource {

	private static final String TAG = BlockchainSourceImpl.class.getSimpleName();

	private static volatile BlockchainSourceImpl instance;
	private final BlockchainSource.Local local;

	private final EventLogger eventLogger;

	private IKinClient kinClient;
	private IKinAccount account;
	private ObservableData<Balance> balance = ObservableData.create(new Balance());
	/**
	 * Listen for {@code completedPayment} in order to be notify about completed transaction sent to the blockchain, it
	 * could failed or succeed.
	 */
	private ObservableData<Payment> completedPayment = ObservableData.create();
	private final Object paymentObserversLock = new Object();
	private final Object balanceObserversLock = new Object();
	private int paymentObserversCount;
	private int balanceObserversCount;

	private IListenerRegistration paymentRegistration;
	private IListenerRegistration balanceRegistration;

	private final MainThreadExecutor mainThread = new MainThreadExecutor();

	private String appID;
	private static final String MEMO_DELIMITER = "-";

	private static final int APP_ID_INDEX = 1;
	private static final int ORDER_ID_INDEX = 2;
	private static final int MEMO_SPLIT_LENGTH = 3;

	private BlockchainSourceImpl(@NonNull EventLogger eventLogger, @NonNull final IKinClient kinClient,
		@NonNull BlockchainSource.Local local)
		throws BlockchainException {
		this.eventLogger = eventLogger;
		this.kinClient = kinClient;
		this.local = local;
		createKinAccountIfNeeded();
		initBalance();
	}

	public static void init(@NonNull EventLogger eventLogger, @NonNull final IKinClient kinClient,
		@NonNull BlockchainSource.Local local) throws BlockchainException {
		if (instance == null) {
			synchronized (BlockchainSourceImpl.class) {
				if (instance == null) {
					instance = new BlockchainSourceImpl(eventLogger, kinClient, local);
				}
			}
		}
	}

	public static BlockchainSourceImpl getInstance() {
		return instance;
	}

	private void createKinAccountIfNeeded() throws BlockchainException {
		int accountIndex = local.getAccountIndex();
		if (kinClient.hasAccount()) {
			account = kinClient.getAccount(accountIndex);
		} else {
			try {
				account = kinClient.addAccount();
				local.setAccountIndex(0);
			} catch (CreateAccountException e) {
				throw ErrorUtil.getBlockchainException(e);
			}
		}
		if (account == null) {
			throw ErrorUtil.createAccountCannotLoadedExcpetion(accountIndex);
		}
	}

	@Override
	@Nullable
	public IKinAccount getKinAccount() {
		return account;
	}

	@Override
	public void setAppID(String appID) {
		if (!TextUtils.isEmpty(appID)) {
			this.appID = appID;
			local.setAppId(appID);
		}
	}

	@Override
	public String getAppId() {
		return appID != null ? appID : local.getAppId();
	}

	@Override
	public void sendTransaction(@NonNull final String publicAddress, @NonNull final BigDecimal amount,
		@NonNull OpenOrder order) throws OperationFailedException {
		OfferType offerType = order.getOfferType();
		String offerId = order.getOfferId();
		String orderId = order.getId();

		sendBroadcastToBlockchainSubmittedEvent(offerType, offerId, orderId);
		try {
			String transactionId = account
				.sendTransactionSync(publicAddress, amount, new WhitelistService(orderId), orderId).id();
			sendTransactionSucceededEvent(offerType, offerId, orderId, transactionId);
			Logger.log(new Log().withTag(TAG).put("sendTransaction onResult", transactionId));
		} catch (OperationFailedException e) {
			sendTransactionFailedEvent(offerType, offerId, orderId, e);
			completedPayment.postValue(new Payment(orderId, false, e));
			throw e;
		}
	}

	private void sendBroadcastToBlockchainSubmittedEvent(OfferType offerType, String offerId, String orderId) {
		if (offerType == OfferType.SPEND) {
			eventLogger.send(SpendTransactionBroadcastToBlockchainSubmitted.create(offerId, orderId));
		} else if (offerType == OfferType.PAY_TO_USER) {
			eventLogger
				.send(PayToUserTransactionBroadcastToBlockchainSubmitted.create(offerId, orderId));
		}
	}

	private void sendTransactionSucceededEvent(OfferType offerType, String offerId, String orderId,
		String transactionId) {
		if (offerType == OfferType.SPEND) {
			eventLogger
				.send(SpendTransactionBroadcastToBlockchainSucceeded.create(transactionId, offerId, orderId));
		} else if (offerType == OfferType.PAY_TO_USER) {
			eventLogger
				.send(PayToUserTransactionBroadcastToBlockchainSucceeded
					.create(transactionId, offerId, orderId));
		}
	}

	private void sendTransactionFailedEvent(OfferType offerType, String offerId, String orderId,
		OperationFailedException e) {
		if (offerType == OfferType.SPEND) {
			eventLogger.send(SpendTransactionBroadcastToBlockchainFailed
				.create(ErrorUtil.getPrintableStackTrace(e), offerId, orderId, "", e.getMessage()));
		} else if (offerType == OfferType.PAY_TO_USER) {
			eventLogger.send(PayToUserTransactionBroadcastToBlockchainFailed
				.create(ErrorUtil.getPrintableStackTrace(e), offerId, orderId, "", e.getMessage()));
		}
	}

	private void initBalance() {
		balance.postValue(getBalance());
		getBalance(null);
	}

	@Override
	public Balance getBalance() {
		Balance balance = new Balance();
		balance.setAmount(new BigDecimal(local.getBalance()));
		return balance;
	}

	@Override
	public void getBalance(@Nullable final KinCallback<Balance> callback) {
		account.getBalance().run(new ResultCallback<IBalance>() {
			@Override
			public void onResult(final IBalance balanceObj) {
				setBalance(balanceObj);
				if (callback != null) {
					mainThread.execute(new Runnable() {
						@Override
						public void run() {
							callback.onResponse(balance.getValue());
						}
					});
				}
				Logger.log(new Log().withTag(TAG).put("getBalance onResult", balanceObj.value().intValue()));
			}

			@Override
			public void onError(final Exception e) {
				if (callback != null) {
					mainThread.execute(new Runnable() {
						@Override
						public void run() {
							callback.onFailure(ErrorUtil.getBlockchainException(e));
						}
					});
				}
				Logger.log(new Log().withTag(TAG).priority(Log.ERROR).put("getBalance onError", e));
			}
		});
	}

	@Override
	public KinSdkVersion getKinSdkVersion() {
		return account.getKinSdkVersion();
	}

	@Override
	public void reconnectBalanceConnection() {
		synchronized (balanceObserversLock) {
			if (balanceObserversCount > 0) {
				if (balanceRegistration != null) {
					balanceRegistration.remove();
				}
				startBalanceListener();
			}
		}
	}

	@VisibleForTesting
	void setBalance(final IBalance balanceObj) {
		Balance balanceTemp = balance.getValue();
		// if the values are not equals so we need to update,
		// no need to update for equal values.
		if (balanceTemp.getAmount().compareTo(balanceObj.value()) != 0) {
			eventLogger.send(KinBalanceUpdated.create(balanceTemp.getAmount().doubleValue()));
			Logger.log(new Log().withTag(TAG).text("setBalance: Balance changed, should get update"));
			balanceTemp.setAmount(balanceObj.value());
			balance.postValue(balanceTemp);
			local.setBalance(balanceObj.value().intValue());
		}
	}

	@Override
	public void addBalanceObserver(@NonNull Observer<Balance> observer, boolean startSSE) {
		balance.addObserver(observer);
		observer.onChanged(balance.getValue());

		if (startSSE) {
			incrementBalanceSSECount();
		}
	}

	private void incrementBalanceSSECount() {
		synchronized (balanceObserversLock) {
			if (balanceObserversCount == 0) {
				startBalanceListener();
			}
			balanceObserversCount++;
			Logger.log(new Log().withTag(TAG).put("incrementBalanceSSECount count", balanceObserversCount));
		}
	}

	private void startBalanceListener() {
		Logger.log(new Log().withTag(TAG).text("startBalanceListener"));
		balanceRegistration = account.addBalanceListener(new IEventListener<IBalance>() {
			@Override
			public void onEvent(IBalance data) {
				final double prevBalance = balance.getValue().getAmount().doubleValue();
				setBalance(data);
				eventLogger.send(KinBalanceUpdated.create(prevBalance));
			}
		});
	}

	@Override
	public void removeBalanceObserver(@NonNull Observer<Balance> observer, boolean stopSSE) {
		Logger.log(new Log().withTag(TAG).text("removeBalanceObserver"));
		balance.removeObserver(observer);
		if (stopSSE) {
			decrementBalanceSSECount();
		}
	}

	private void decrementBalanceSSECount() {
		synchronized (balanceObserversLock) {
			if (balanceObserversCount > 0) {
				balanceObserversCount--;
			}

			if (balanceObserversCount == 0) {
				removeRegistration(balanceRegistration);
				Logger.log(new Log().withTag(TAG).text("decrementBalanceCount: removeRegistration"));
			}
		}
		Logger.log(new Log().withTag(TAG).put("decrementBalanceCount: count", balanceObserversCount));
	}


	@Override
	public String getPublicAddress() {
		if (account == null) {
			return null;
		}
		return account.getPublicAddress();
	}

	@Override
	@Nullable
	public String getPublicAddress(final int accountIndex) {
		IKinAccount account = kinClient.getAccount(accountIndex);
		return account != null ? account.getPublicAddress() : null;
	}

	@Override
	public void addPaymentObservable(Observer<Payment> observer) {
		completedPayment.addObserver(observer);
		incrementPaymentCount();
	}

	private void incrementPaymentCount() {
		synchronized (paymentObserversLock) {
			if (paymentObserversCount == 0) {
				startPaymentListener();
			}
			paymentObserversCount++;
		}
	}

	private void startPaymentListener() {
		paymentRegistration = account.addPaymentListener(new IEventListener<IPaymentInfo>() {
			@Override
			public void onEvent(IPaymentInfo data) {
				final String orderID = extractOrderId(data.memo());
				Logger.log(new Log().withTag(TAG).put("startPaymentListener onEvent: the orderId", orderID)
					.put("with memo", data.memo()));
				final String accountPublicAddress = getPublicAddress();
				if (orderID != null && accountPublicAddress != null) {
					completedPayment.postValue(PaymentConverter.toPayment(data, orderID, accountPublicAddress));
					Logger.log(new Log().withTag(TAG).put("completedPayment order id", orderID));
				}
				// UpdateBalance
				getBalance(null);
			}
		});
	}

	@Override
	public void removePaymentObserver(Observer<Payment> observer) {
		completedPayment.removeObserver(observer);
		decrementPaymentCount();
	}

	@Override
	public void createTrustLine(@NonNull final KinCallback<Void> callback) {
		new CreateTrustLineCall(account, new TrustlineCallback() {
			@Override
			public void onSuccess() {
				eventLogger.send(StellarKinTrustlineSetupSucceeded.create());
				mainThread.execute(new Runnable() {
					@Override
					public void run() {
						callback.onResponse(null);
					}
				});
			}

			@Override
			public void onFailure(final OperationFailedException e) {
				eventLogger.send(
					StellarKinTrustlineSetupFailed.create(ErrorUtil.getPrintableStackTrace(e), "", e.getMessage()));
				mainThread.execute(new Runnable() {
					@Override
					public void run() {
						callback.onFailure(ErrorUtil.getBlockchainException(e));
					}
				});
			}
		}).start();
	}

	@Override
	public KeyStoreProvider getKeyStoreProvider() {
		return new KeyStoreProviderImpl(kinClient, account);
	}

	@Override
	public void updateActiveAccount(IKinClient kinClient, int accountIndex) throws BlockchainException {
		this.kinClient = kinClient;
		local.setAccountIndex(accountIndex);
		createKinAccountIfNeeded();

		synchronized (balanceObserversLock) {
			removeRegistration(balanceRegistration);
			if (balanceObserversCount > 0) {
				startBalanceListener();
			}
		}
		//trigger balance update
		getBalance(null);
	}

	private void decrementPaymentCount() {
		synchronized (paymentObserversLock) {
			if (paymentObserversCount > 0) {
				paymentObserversCount--;
			}

			if (paymentObserversCount == 0) {
				removeRegistration(paymentRegistration);
			}
		}
	}

	private void removeRegistration(IListenerRegistration listenerRegistration) {
		Logger.log(new Log().withTag(TAG).text("removeRegistration"));
		if (listenerRegistration != null) {
			listenerRegistration.remove();
		}
	}


	@VisibleForTesting
	String extractOrderId(String memo) {
		String[] memoParts = memo.split(MEMO_DELIMITER);
		String orderID = null;
		if (memoParts.length == MEMO_SPLIT_LENGTH && memoParts[APP_ID_INDEX].equals(appID)) {
			orderID = memoParts[ORDER_ID_INDEX];
		}
		return orderID;
	}
}
