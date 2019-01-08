package kin.devplatform.data.order;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.EarnOrderCreationFailed;
import kin.devplatform.bi.events.EarnOrderCreationReceived;
import kin.devplatform.bi.events.PayToUserOrderCreationFailed;
import kin.devplatform.bi.events.PayToUserOrderCreationReceived;
import kin.devplatform.bi.events.SpendOrderCreationFailed;
import kin.devplatform.bi.events.SpendOrderCreationReceived;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.core.util.ExecutorsUtil.MainThreadExecutor;
import kin.devplatform.data.Callback;
import kin.devplatform.data.blockchain.BlockchainSource;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.model.Payment;
import kin.devplatform.data.order.OrderDataSource.Remote;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.network.model.JWTBodyPaymentConfirmationResult;
import kin.devplatform.network.model.Offer.OfferType;
import kin.devplatform.network.model.OpenOrder;
import kin.devplatform.network.model.Order;
import kin.devplatform.network.model.Order.Status;
import kin.devplatform.util.ErrorUtil;
import kin.sdk.migration.exception.InsufficientKinException;
import kin.sdk.migration.exception.OperationFailedException;

class CreateExternalOrderCall extends Thread {

	private final OrderDataSource.Remote remote;
	private final BlockchainSource blockchainSource;
	private final String orderJwt;
	private final ExternalOrderCallbacks externalOrderCallbacks;
	private final EventLogger eventLogger;
	private final long paymentListeningTimeout;

	private OpenOrder openOrder;
	private MainThreadExecutor mainThreadExecutor = new MainThreadExecutor();

	CreateExternalOrderCall(@NonNull Remote remote, @NonNull BlockchainSource blockchainSource,
		@NonNull String orderJwt, @NonNull EventLogger eventLogger,
		@NonNull ExternalOrderCallbacks externalOrderCallbacks, long paymentListeningTimeoutMillis) {
		this.remote = remote;
		this.blockchainSource = blockchainSource;
		this.orderJwt = orderJwt;
		this.eventLogger = eventLogger;
		this.externalOrderCallbacks = externalOrderCallbacks;
		this.paymentListeningTimeout = paymentListeningTimeoutMillis;
	}

	@Override
	public void run() {
		try {
			openOrder = remote.createExternalOrderSync(orderJwt);
			sendOrderCreationReceivedEvent();

			if (doesClientSendsTransaction(openOrder)) {
				Balance balance = blockchainSource.getBalance();
				if (balance.getAmount().intValue() < openOrder.getAmount()) {
					remote.cancelOrderSync(openOrder.getId());
					runOnMainThread(new Runnable() {
						@Override
						public void run() {
							externalOrderCallbacks
								.onOrderFailed(ErrorUtil.getBlockchainException(new InsufficientKinException()),
									openOrder);
						}
					});
					return;
				}
			} else {
				// start listen to payment in earn cases, before onOrderCreated, in order to listen before server sends transaction
				// server will send transaction in response to 'submit order' call.
				listenToPaymentsOnBlockchain(openOrder.getId());
			}

			runOnMainThread(new Runnable() {
				@Override
				public void run() {
					externalOrderCallbacks.onOrderCreated(openOrder);
				}
			});
		} catch (final ApiException e) {
			if (isOrderConflictError(e)) {
				String orderID = extractOrderID(e.getResponseHeaders());
				getOrder(orderID);
			} else {
				sendOrderCreationFailedEvent(e);
				onOrderFailed(ErrorUtil.fromApiException(e));
			}
			return;
		}

		if (doesClientSendsTransaction(openOrder)) {
			performTransactionSending();
		}
	}

	private void listenToPaymentsOnBlockchain(final String orderId) {
		final Handler handler = new Handler(Looper.getMainLooper());
		final Observer<Payment> paymentObserver = new Observer<Payment>() {
			@Override
			public void onChanged(final Payment payment) {
				if (isPaymentOrderEquals(payment, openOrder.getId())) {
					handlePaymentListenerOnChanged(payment);
					blockchainSource.removePaymentObserver(this);
					handler.removeCallbacksAndMessages(null);
				}
			}
		};
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				blockchainSource.removePaymentObserver(paymentObserver);
				getOrder(orderId);
			}
		}, paymentListeningTimeout);
		blockchainSource.addPaymentObservable(paymentObserver);
	}

	private void handlePaymentListenerOnChanged(final Payment payment) {
		if (payment.isSucceed()) {
			getOrder(payment.getOrderID());
		} else {
			runOnMainThread(new Runnable() {
				@Override
				public void run() {
					((ExternalSpendOrderCallbacks) externalOrderCallbacks)
						.onTransactionFailed(openOrder, ErrorUtil.getBlockchainException(payment.getException()));
				}
			});
		}
	}

	private void performTransactionSending() {
		fireOnTransactionSent();
		try {
			blockchainSource.sendTransaction(openOrder.getBlockchainData().getRecipientAddress(),
				new BigDecimal(openOrder.getAmount()), openOrder);
			getOrder(openOrder.getId());
		} catch (final OperationFailedException e) {
			runOnMainThread(new Runnable() {
				@Override
				public void run() {
					((ExternalSpendOrderCallbacks) externalOrderCallbacks)
						.onTransactionFailed(openOrder, ErrorUtil.getBlockchainException(e));
				}
			});
		}
	}

	private void fireOnTransactionSent() {
		runOnMainThread(new Runnable() {
			@Override
			public void run() {
				((ExternalSpendOrderCallbacks) externalOrderCallbacks).onTransactionSent(openOrder);
			}
		});
	}

	private boolean doesClientSendsTransaction(OpenOrder order) {
		return order.getOfferType() == OfferType.SPEND || order.getOfferType() == OfferType.PAY_TO_USER;
	}

	private void sendOrderCreationFailedEvent(ApiException exception) {
		if (openOrder != null && openOrder.getOfferType() != null) {
			switch (openOrder.getOfferType()) {
				case SPEND:
					eventLogger.send(SpendOrderCreationFailed
						.create(ErrorUtil.getPrintableStackTrace(exception), openOrder.getOfferId(),
							SpendOrderCreationFailed.Origin.EXTERNAL, String.valueOf(exception.getCode()),
							exception.getMessage()));
					break;
				case EARN:
					eventLogger.send(EarnOrderCreationFailed
						.create(ErrorUtil.getPrintableStackTrace(exception), openOrder.getOfferId(),
							EarnOrderCreationFailed.Origin.EXTERNAL, String.valueOf(exception.getCode()),
							exception.getMessage()));
					break;
				case PAY_TO_USER:
					eventLogger.send(PayToUserOrderCreationFailed
						.create(ErrorUtil.getPrintableStackTrace(exception), openOrder.getOfferId(),
							PayToUserOrderCreationFailed.Origin.EXTERNAL, String.valueOf(exception.getCode()),
							exception.getMessage()));
					break;
			}

		}
	}

	private void sendOrderCreationReceivedEvent() {
		if (openOrder != null && openOrder.getOfferType() != null) {
			switch (openOrder.getOfferType()) {
				case SPEND:
					eventLogger.send(SpendOrderCreationReceived
						.create(openOrder.getOfferId(), openOrder.getId(), SpendOrderCreationReceived.Origin.EXTERNAL));
					break;
				case EARN:
					eventLogger.send(EarnOrderCreationReceived
						.create(openOrder.getOfferId(), openOrder.getId(), EarnOrderCreationReceived.Origin.EXTERNAL));
					break;
				case PAY_TO_USER:
					eventLogger.send(PayToUserOrderCreationReceived
						.create(openOrder.getOfferId(), openOrder.getId(),
							PayToUserOrderCreationReceived.Origin.EXTERNAL));
					break;
			}
		}
	}

	private String extractOrderID(Map<String, List<String>> responseHeaders) {
		String orderID = null;
		List<String> locationList = responseHeaders.get("location");
		if (locationList != null && locationList.size() > 0) {
			String url = locationList.get(0);
			String[] parts = url.split("/");
			orderID = parts[parts.length - 1];
		}

		return orderID;
	}

	private boolean isOrderConflictError(ApiException e) {
		return e.getCode() == 409 && e.getResponseBody().getCode() == 4091;
	}

	private boolean isPaymentOrderEquals(Payment payment, String orderId) {
		String paymentOrderID = payment.getOrderID();
		return paymentOrderID != null && paymentOrderID.equals(orderId);
	}

	private void getOrder(String orderID) {
		new GetOrderPollingCall(remote, orderID, new Callback<Order, ApiException>() {
			@Override
			public void onResponse(final Order order) {
				runOnMainThread(new Runnable() {
					@Override
					public void run() {
						if (order.getStatus() == Status.FAILED) {
							onOrderFailed(ErrorUtil.fromFailedOrder(order));
						} else {
							externalOrderCallbacks
								.onOrderConfirmed(((JWTBodyPaymentConfirmationResult) order.getResult()).getJwt(),
									order);
						}
					}
				});

			}

			@Override
			public void onFailure(final ApiException e) {
				onOrderFailed(ErrorUtil.fromApiException(e));
			}
		}).start();
	}

	private void onOrderFailed(final KinEcosystemException exception) {
		final OpenOrder finalOpenOrder = openOrder;
		runOnMainThread(new Runnable() {
			@Override
			public void run() {
				externalOrderCallbacks
					.onOrderFailed(exception, finalOpenOrder);
			}
		});
	}

	private void runOnMainThread(Runnable runnable) {
		mainThreadExecutor.execute(runnable);
	}

	interface ExternalOrderCallbacks {

		void onOrderCreated(OpenOrder openOrder);

		void onOrderConfirmed(String confirmationJwt, Order order);

		void onOrderFailed(KinEcosystemException exception, OpenOrder order);
	}

	interface ExternalSpendOrderCallbacks extends ExternalOrderCallbacks {

		void onTransactionSent(OpenOrder openOrder);

		void onTransactionFailed(OpenOrder openOrder, KinEcosystemException exception);
	}
}
