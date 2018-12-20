package kin.devplatform.data.blockchain;

import android.annotation.SuppressLint;
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
import kin.devplatform.data.KinCallbackAdapter;
import kin.devplatform.data.blockchain.CreateTrustLineCall.TrustlineCallback;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.model.Payment;
import kin.devplatform.exception.BlockchainException;
import kin.devplatform.network.model.Offer.OfferType;
import kin.devplatform.util.ErrorUtil;
import kin.sdk.migration.IBalance;
import kin.sdk.migration.IEventListener;
import kin.sdk.migration.IKinAccount;
import kin.sdk.migration.IKinClient;
import kin.sdk.migration.IListenerRegistration;
import kin.sdk.migration.IPaymentInfo;
import kin.sdk.migration.IResultCallback;
import kin.sdk.migration.ITransactionId;
import kin.sdk.migration.exception.CreateAccountException;
import kin.sdk.migration.exception.OperationFailedException;

public class BlockchainSourceImpl implements BlockchainSource {

	private static final String TAG = BlockchainSourceImpl.class.getSimpleName();

	private static volatile BlockchainSourceImpl instance;
	private final BlockchainSource.Local local;

	private final EventLogger eventLogger;

	private final IKinClient kinClient;
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
	private static final int MEMO_FORMAT_VERSION = 1;
	private static final String MEMO_DELIMITER = "-";
	private static final String MEMO_FORMAT =
		"%d" + MEMO_DELIMITER + "%s" + MEMO_DELIMITER + "%s"; // version-appID-orderID

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
		@NonNull BlockchainSource.Local local)
		throws BlockchainException {
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
		}
	}

	@Override
	@Nullable
	public String getAppId() {
		return appID;
	}

	@Override
	public void sendTransaction(@NonNull final String publicAddress, @NonNull final BigDecimal amount,
		@NonNull final String orderID, @NonNull final String offerID, final OfferType offerType) {
		if (offerType == OfferType.SPEND) {
			eventLogger.send(SpendTransactionBroadcastToBlockchainSubmitted.create(offerID, orderID));
		} else if (offerType == OfferType.PAY_TO_USER) {
			eventLogger.send(PayToUserTransactionBroadcastToBlockchainSubmitted.create(offerID, orderID));
		}
		account.sendTransaction(publicAddress, amount, generateMemo(orderID)).run(
			new IResultCallback<ITransactionId>() {
				@Override
				public void onResult(ITransactionId result) {
					if (offerType == OfferType.SPEND) {
						eventLogger
							.send(SpendTransactionBroadcastToBlockchainSucceeded.create(result.id(), offerID, orderID));
					} else if (offerType == OfferType.PAY_TO_USER) {
						eventLogger
							.send(PayToUserTransactionBroadcastToBlockchainSucceeded
								.create(result.id(), offerID, orderID));
					}
					Logger.log(new Log().withTag(TAG).put("sendTransaction onResult", result.id()));
				}

				@Override
				public void onError(Exception e) {
					if (offerType == OfferType.SPEND) {
						eventLogger
							.send(SpendTransactionBroadcastToBlockchainFailed
								.create(ErrorUtil.getPrintableStackTrace(e), offerID, orderID, "", e.getMessage()));
					} else if (offerType == OfferType.PAY_TO_USER) {
						eventLogger
							.send(PayToUserTransactionBroadcastToBlockchainFailed
								.create(ErrorUtil.getPrintableStackTrace(e), offerID, orderID, "", e.getMessage()));
					}
					completedPayment.postValue(new Payment(orderID, false, e));
					Logger.log(new Log().withTag(TAG).put("sendTransaction onError", e.getMessage()));
				}
			});
	}

	@SuppressLint("DefaultLocale")
	@VisibleForTesting
	String generateMemo(@NonNull final String orderID) {
		return String.format(MEMO_FORMAT, MEMO_FORMAT_VERSION, appID, orderID);
	}


	private void initBalance() {
		balance.postValue(getBalance());
		getBalance(new KinCallbackAdapter<Balance>() {
		});
	}

	@Override
	public Balance getBalance() {
		Balance balance = new Balance();
		balance.setAmount(new BigDecimal(local.getBalance()));
		return balance;
	}

	@Override
	public void getBalance(@Nullable final KinCallback<Balance> callback) {
		account.getBalance().run(new IResultCallback<IBalance>() {
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

	@VisibleForTesting
	void setBalance(final IBalance balanceObj) {
		Balance balanceTemp = balance.getValue();
		// if the values are not equals so we need to update,
		// no need to update for equal values.
		if (balanceTemp.getAmount().compareTo(balanceObj.value()) != 0) {
			Logger.log(new Log().withTag(TAG).text("setBalance: Balance changed, should get update"));
			balanceTemp.setAmount(balanceObj.value());
			balance.postValue(balanceTemp);
			local.setBalance(balanceObj.value().intValue());
		}
	}

	@Override
	public void addBalanceObserver(@NonNull Observer<Balance> observer) {
		balance.addObserver(observer);
		observer.onChanged(balance.getValue());
	}

	@Override
	public void addBalanceObserverAndStartListen(@NonNull Observer<Balance> observer) {
		addBalanceObserver(observer);
		Logger.log(new Log().withTag(TAG).put("addBalanceObserverAndStartListen count", balanceObserversCount));
		incrementBalanceCount();
	}

	private void incrementBalanceCount() {
		synchronized (balanceObserversLock) {
			if (balanceObserversCount == 0) {
				startBalanceListener();
			}
			balanceObserversCount++;
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
	public void removeBalanceObserver(@NonNull Observer<Balance> observer) {
		Logger.log(new Log().withTag(TAG).text("removeBalanceObserver"));
		balance.removeObserver(observer);
	}


	public void removeBalanceObserverAndStopListen(@NonNull Observer<Balance> observer) {
		removeBalanceObserver(observer);
		decrementBalanceCount();
	}

	private void decrementBalanceCount() {
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
					// UpdateBalance if there is no balance sse open connection.
					if (balanceObserversCount == 0) {
						final double prevBalance = balance.getValue().getAmount().doubleValue();
						getBalance(new KinCallbackAdapter<Balance>() {
							@Override
							public void onResponse(Balance response) {
								eventLogger.send(KinBalanceUpdated.create(prevBalance));
							}
						});
					}
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
	public void updateActiveAccount(int accountIndex) throws BlockchainException {
		local.setAccountIndex(accountIndex);
		createKinAccountIfNeeded();

		balanceRegistration.remove();
		startBalanceListener();
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
