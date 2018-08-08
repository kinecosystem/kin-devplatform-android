package kin.devplatform.data.order;

import android.support.annotation.NonNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import kin.core.exception.InsufficientKinException;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.SpendOrderCreationFailed;
import kin.devplatform.bi.events.SpendOrderCreationReceived;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.core.util.ExecutorsUtil.MainThreadExecutor;
import kin.devplatform.data.Callback;
import kin.devplatform.data.blockchain.BlockchainSource;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.model.Payment;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.network.model.JWTBodyPaymentConfirmationResult;
import kin.devplatform.network.model.Offer.OfferType;
import kin.devplatform.network.model.OpenOrder;
import kin.devplatform.network.model.Order;
import kin.devplatform.network.model.Order.Status;
import kin.devplatform.util.ErrorUtil;

class CreateExternalOrderCall extends Thread {

	private final OrderDataSource.Remote remote;
	private final BlockchainSource blockchainSource;
	private final String orderJwt;
	private final ExternalOrderCallbacks externalOrderCallbacks;
	private final EventLogger eventLogger;

	private OpenOrder openOrder;
	private MainThreadExecutor mainThreadExecutor = new MainThreadExecutor();

	CreateExternalOrderCall(@NonNull OrderDataSource.Remote remote, @NonNull BlockchainSource blockchainSource,
		@NonNull String orderJwt, @NonNull EventLogger eventLogger,
		@NonNull ExternalOrderCallbacks externalOrderCallbacks) {
		this.remote = remote;
		this.blockchainSource = blockchainSource;
		this.orderJwt = orderJwt;
		this.eventLogger = eventLogger;
		this.externalOrderCallbacks = externalOrderCallbacks;
	}

	@Override
	public void run() {
		try {
			// Create external order
			openOrder = remote.createExternalOrderSync(orderJwt);
			sendOrderCreationReceivedEvent();

			if (openOrder.getOfferType() == OfferType.SPEND) {
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

		if (externalOrderCallbacks instanceof ExternalSpendOrderCallbacks) {
			blockchainSource.sendTransaction(openOrder.getBlockchainData().getRecipientAddress(),
				new BigDecimal(openOrder.getAmount()), openOrder.getId(), openOrder.getOfferId());

			runOnMainThread(new Runnable() {
				@Override
				public void run() {
					((ExternalSpendOrderCallbacks) externalOrderCallbacks).onTransactionSent(openOrder);
				}
			});
		}

		//Listen for payments, make sure the transaction succeed.
		blockchainSource.addPaymentObservable(new Observer<Payment>() {
			@Override
			public void onChanged(final Payment payment) {
				if (isPaymentOrderEquals(payment, openOrder.getId())) {
					if (payment.isSucceed()) {
						getOrder(payment.getOrderID());
					} else {
						if (externalOrderCallbacks instanceof ExternalSpendOrderCallbacks) {
							runOnMainThread(new Runnable() {
								@Override
								public void run() {
									((ExternalSpendOrderCallbacks) externalOrderCallbacks)
										.onTransactionFailed(openOrder,
											ErrorUtil.getBlockchainException(payment.getException()));
								}
							});
						}
					}
					blockchainSource.removePaymentObserver(this);
				}
			}
		});
	}

	private void sendOrderCreationFailedEvent(ApiException exception) {
		if (openOrder != null && openOrder.getOfferType() != null) {
			switch (openOrder.getOfferType()) {
				case SPEND:
					final Throwable cause = exception.getCause();
					final String reason = cause != null ? cause.getMessage() : exception.getMessage();
					eventLogger.send(SpendOrderCreationFailed.create(reason, openOrder.getOfferId(), true));
					break;
				case EARN:
					//TODO add event
					// We don't have event correctly
					break;
			}

		}
	}

	private void sendOrderCreationReceivedEvent() {
		if (openOrder != null && openOrder.getOfferType() != null) {
			switch (openOrder.getOfferType()) {
				case SPEND:
					eventLogger.send(SpendOrderCreationReceived
						.create(openOrder.getOfferId(), openOrder.getId(), true));
					break;
				case EARN:
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
