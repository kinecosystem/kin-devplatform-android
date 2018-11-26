package kin.devplatform.data.order;

import static kin.devplatform.exception.ClientException.INTERNAL_INCONSISTENCY;
import static kin.devplatform.util.ErrorUtil.getClientException;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import kin.devplatform.KinCallback;
import kin.devplatform.base.ObservableData;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.EarnOrderCompleted;
import kin.devplatform.bi.events.EarnOrderCompletionSubmitted;
import kin.devplatform.bi.events.EarnOrderCreationRequested;
import kin.devplatform.bi.events.EarnOrderFailed;
import kin.devplatform.bi.events.EarnOrderPaymentConfirmed;
import kin.devplatform.bi.events.PayToUserOrderCompleted;
import kin.devplatform.bi.events.PayToUserOrderCompletionSubmitted;
import kin.devplatform.bi.events.PayToUserOrderCreationRequested;
import kin.devplatform.bi.events.PayToUserOrderFailed;
import kin.devplatform.bi.events.SpendOrderCompleted;
import kin.devplatform.bi.events.SpendOrderCompletionSubmitted;
import kin.devplatform.bi.events.SpendOrderCreationRequested;
import kin.devplatform.bi.events.SpendOrderFailed;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.data.Callback;
import kin.devplatform.data.KinCallbackAdapter;
import kin.devplatform.data.blockchain.BlockchainSource;
import kin.devplatform.data.model.OrderConfirmation;
import kin.devplatform.data.model.Payment;
import kin.devplatform.data.order.CreateExternalOrderCall.ExternalOrderCallbacks;
import kin.devplatform.data.order.CreateExternalOrderCall.ExternalSpendOrderCallbacks;
import kin.devplatform.exception.DataNotAvailableException;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.network.model.JWTBodyPaymentConfirmationResult;
import kin.devplatform.network.model.Offer.OfferType;
import kin.devplatform.network.model.OpenOrder;
import kin.devplatform.network.model.Order;
import kin.devplatform.network.model.Order.Origin;
import kin.devplatform.network.model.Order.Status;
import kin.devplatform.network.model.OrderList;
import kin.devplatform.util.ErrorUtil;

public class OrderRepository implements OrderDataSource {

	private static OrderRepository instance = null;
	private final OrderDataSource.Local localData;
	private final OrderDataSource.Remote remoteData;

	private final BlockchainSource blockchainSource;
	private final EventLogger eventLogger;

	private OrderList cachedOrderList;
	private ObservableData<OpenOrder> cachedOpenOrder = ObservableData.create();
	private ObservableData<Order> orderWatcher = ObservableData.create();
	private Observer<Payment> paymentObserver;

	private volatile AtomicInteger pendingOrdersCount = new AtomicInteger(0);

	private final Object paymentObserversLock = new Object();
	private int paymentObserverCount;

	private OrderRepository(@NonNull final BlockchainSource blockchainSource,
		@NonNull final EventLogger eventLogger,
		@NonNull final OrderDataSource.Remote remoteData,
		@NonNull final OrderDataSource.Local localData) {
		this.remoteData = remoteData;
		this.localData = localData;
		this.blockchainSource = blockchainSource;
		this.eventLogger = eventLogger;
	}

	public static void init(@NonNull final BlockchainSource blockchainSource,
		@NonNull final EventLogger eventLogger,
		@NonNull final OrderDataSource.Remote remoteData,
		@NonNull final OrderDataSource.Local localData) {
		if (instance == null) {
			synchronized (OrderRepository.class) {
				if (instance == null) {
					instance = new OrderRepository(blockchainSource, eventLogger, remoteData,
						localData);
				}
			}
		}
	}

	public static OrderRepository getInstance() {
		return instance;
	}

	public OrderList getAllCachedOrderHistory() {
		return cachedOrderList;
	}

	@Override
	public void getAllOrderHistory(@NonNull final KinCallback<OrderList> callback) {
		remoteData.getAllOrderHistory(new Callback<OrderList, ApiException>() {
			@Override
			public void onResponse(OrderList response) {
				cachedOrderList = response;
				callback.onResponse(response);
			}

			@Override
			public void onFailure(ApiException e) {
				callback.onFailure(ErrorUtil.fromApiException(e));
			}
		});
	}

	public ObservableData<OpenOrder> getOpenOrder() {
		return cachedOpenOrder;
	}

	@Override
	public void createOrder(@NonNull final String offerID, @Nullable final KinCallback<OpenOrder> callback) {
		remoteData.createOrder(offerID, new Callback<OpenOrder, ApiException>() {
			@Override
			public void onResponse(OpenOrder response) {
				cachedOpenOrder.postValue(response);
				if (callback != null) {
					callback.onResponse(response);
				}
			}

			@Override
			public void onFailure(ApiException e) {
				if (callback != null) {
					callback.onFailure(ErrorUtil.fromApiException(e));
				}
			}
		});
	}

	@Override
	public void submitOrder(@NonNull final String offerID, @Nullable String content, @NonNull final String orderID,
		kin.devplatform.network.model.Origin origin, @Nullable final KinCallback<Order> callback) {
		listenForCompletedPayment(origin);
		remoteData.submitOrder(content, orderID, new Callback<Order, ApiException>() {
			@Override
			public void onResponse(Order response) {
				pendingOrdersCount.incrementAndGet();
				getOrderWatcher().postValue(response);
				if (callback != null) {
					callback.onResponse(response);
				}
			}

			@Override
			public void onFailure(ApiException e) {
				getOrderWatcher().postValue(
					new Order().orderId(orderID).offerId(offerID).status(Status.FAILED).error(e.getResponseBody()));
				removeCachedOpenOrderByID(orderID);
				if (callback != null) {
					callback.onFailure(ErrorUtil.fromApiException(e));
				}
			}
		});
	}

	private void listenForCompletedPayment(final kin.devplatform.network.model.Origin origin) {
		synchronized (paymentObserversLock) {
			if (paymentObserverCount == 0) {
				paymentObserver = new Observer<Payment>() {
					@Override
					public void onChanged(Payment payment) {
						sendEarnPaymentConfirmed(payment, origin);
						decrementPaymentObserverCount();
						getOrder(payment.getOrderID());
					}
				};
				blockchainSource.addPaymentObservable(paymentObserver);
			}
			paymentObserverCount++;
		}
	}

	private void sendEarnPaymentConfirmed(Payment payment, kin.devplatform.network.model.Origin origin) {
		boolean isNative = origin == kin.devplatform.network.model.Origin.EXTERNAL;
		if (payment.isSucceed() && payment.getAmount() != null && payment.getType() == Payment.EARN) {
			eventLogger
				.send(EarnOrderPaymentConfirmed.create(payment.getTransactionID(), payment.getOrderID(), isNative,
					isNative ? EarnOrderPaymentConfirmed.Origin.EXTERNAL : EarnOrderPaymentConfirmed.Origin.MARKETPLACE,
					""));
		}
	}

	private void decrementPaymentObserverCount() {
		synchronized (paymentObserversLock) {
			if (paymentObserverCount > 0) {
				paymentObserverCount--;
			}

			if (paymentObserverCount == 0 && paymentObserver != null) {
				blockchainSource.removePaymentObserver(paymentObserver);
				paymentObserver = null;
			}
		}
	}

	private void getOrder(final String orderID) {
		remoteData.getOrder(orderID, new Callback<Order, ApiException>() {
			@Override
			public void onResponse(Order order) {
				decrementPendingOrdersCount();
				getOrderWatcher().postValue(order);
				//in case of marketplace orders, this is the point where we verify order completion,
				//in case of native/external orders, we will verify on onOrderConfirmed callback at CreateExternalOrder class
				sendMarketplaceOrderCompleted(order);
				if (!hasMorePendingOffers()) {
					removeCachedOpenOrderByID(order.getOrderId());
				}
			}

			@Override
			public void onFailure(ApiException t) {
				decrementPendingOrdersCount();
			}
		});
	}

	private void decrementPendingOrdersCount() {
		if (hasMorePendingOffers()) {
			pendingOrdersCount.decrementAndGet();
		}
	}

	@VisibleForTesting
	ObservableData<Order> getOrderWatcher() {
		return orderWatcher;
	}

	private void sendMarketplaceOrderCompleted(Order order) {
		if (order.getOrigin() == Origin.MARKETPLACE) {
			if (order.getStatus() == Status.COMPLETED) {
				if (order.getOfferType() == OfferType.SPEND) {
					eventLogger.send(SpendOrderCompleted.create(order.getOfferId(), order.getOrderId(), false,
						SpendOrderCompleted.Origin.MARKETPLACE, Double.valueOf(order.getAmount())));
				} else if (order.getOfferType() == OfferType.EARN) {
					eventLogger.send(EarnOrderCompleted.create(order.getOfferId(), order.getOrderId(), false,
						EarnOrderCompleted.Origin.MARKETPLACE, Double.valueOf(order.getAmount())));
				}
			} else {
				String reason = "";
				String errorCode = "";
				String message = "";
				if (order.getError() != null) {
					reason = order.getError().getMessage();
					errorCode = String.valueOf(order.getError().getCode());
					message = order.getError().getError();
				}
				if (order.getOfferType() == OfferType.SPEND) {
					eventLogger.send(SpendOrderFailed.create(reason, order.getOfferId(), order.getOrderId(), false,
						SpendOrderFailed.Origin.MARKETPLACE, errorCode, message));
				} else if (order.getOfferType() == OfferType.EARN) {
					eventLogger.send(EarnOrderFailed.create(reason, order.getOfferId(), order.getOrderId(), false,
						EarnOrderFailed.Origin.MARKETPLACE, errorCode, message));
				}
			}
		}
	}

	private boolean hasMorePendingOffers() {
		return pendingOrdersCount.get() > 0;
	}

	private void removeCachedOpenOrderByID(String orderId) {
		if (isCachedOpenOrderEquals(orderId)) {
			cachedOpenOrder.postValue(null);
		}
	}

	private boolean isCachedOpenOrderEquals(String orderId) {
		if (cachedOpenOrder != null) {
			final OpenOrder openOrder = cachedOpenOrder.getValue();
			if (openOrder != null) {
				return openOrder.getId().equals(orderId);
			}
		}
		return false;
	}

	@Override
	public void cancelOrder(@NonNull final String offerID, @NonNull final String orderID,
		@Nullable final KinCallback<Void> callback) {
		removeCachedOpenOrderByID(orderID);
		remoteData.cancelOrder(orderID, new Callback<Void, ApiException>() {
			@Override
			public void onResponse(Void response) {
				if (callback != null) {
					callback.onResponse(response);
				}
			}

			@Override
			public void onFailure(ApiException e) {
				if (callback != null) {
					callback.onFailure(ErrorUtil.fromApiException(e));
				}
			}
		});
	}

	@Override
	public void payToUser(String offerJwt, @Nullable final KinCallback<OrderConfirmation> callback) {
		//pay to user has a similar flow like purchase (spend), the only different is the expected input JWT and the corresponding events
		spendFlow(offerJwt, true, callback);
	}

	@Override
	public void purchase(String offerJwt, @Nullable final KinCallback<OrderConfirmation> callback) {
		spendFlow(offerJwt, false, callback);
	}

	private void spendFlow(String offerJwt, final boolean isPayToUser,
		@Nullable final KinCallback<OrderConfirmation> callback) {
		if (isPayToUser) {
			eventLogger.send(
				PayToUserOrderCreationRequested.create("", true, PayToUserOrderCreationRequested.Origin.EXTERNAL));
		} else {
			eventLogger.send(SpendOrderCreationRequested.create("", true, SpendOrderCreationRequested.Origin.EXTERNAL));
		}
		new ExternalSpendOrderCall(remoteData, blockchainSource, offerJwt, eventLogger,
			new ExternalSpendOrderCallbacks() {
				@Override
				public void onOrderCreated(OpenOrder openOrder) {
					cachedOpenOrder.postValue(openOrder);
				}

				@Override
				public void onTransactionSent(final OpenOrder openOrder) {
					submitOrder(openOrder.getOfferId(), null, openOrder.getId(),
						kin.devplatform.network.model.Origin.EXTERNAL, new KinCallbackAdapter<Order>() {
							@Override
							public void onFailure(KinEcosystemException exception) {
								handleOnFailure(exception, openOrder.getOfferId(), openOrder.getId());
							}
						});
					if (isPayToUser) {
						eventLogger.send(PayToUserOrderCompletionSubmitted
							.create(openOrder.getOfferId(), openOrder.getId(), true,
								PayToUserOrderCompletionSubmitted.Origin.EXTERNAL));
					} else {
						eventLogger.send(SpendOrderCompletionSubmitted
							.create(openOrder.getOfferId(), openOrder.getId(), true,
								SpendOrderCompletionSubmitted.Origin.EXTERNAL));
					}
				}

				@Override
				public void onTransactionFailed(final OpenOrder openOrder, final KinEcosystemException exception) {
					cancelOrder(openOrder.getOfferId(), openOrder.getId(), new KinCallback<Void>() {
						@Override
						public void onResponse(Void response) {
							handleOnFailure(exception, openOrder.getOfferId(), openOrder.getId());
						}

						@Override
						public void onFailure(KinEcosystemException e) {
							handleOnFailure(exception, openOrder.getOfferId(), openOrder.getId());
						}

					});

				}

				@Override
				public void onOrderConfirmed(String confirmationJwt, Order order) {
					String offerID = "null";
					String orderId = "null";
					if (order != null) {
						offerID = order.getOfferId();
						orderId = order.getOrderId();
					}
					if (isPayToUser) {
						eventLogger.send(PayToUserOrderCompleted
							.create(offerID, orderId, true, PayToUserOrderCompleted.Origin.EXTERNAL,
								Double.valueOf(order.getAmount())));
					} else {
						eventLogger.send(SpendOrderCompleted
							.create(offerID, orderId, true, SpendOrderCompleted.Origin.EXTERNAL,
								Double.valueOf(order.getAmount())));
					}

					if (callback != null) {
						callback.onResponse(createOrderConfirmation(confirmationJwt));
					}
				}

				@Override
				public void onOrderFailed(KinEcosystemException exception, OpenOrder openOrder) {
					if (openOrder != null) { // did not fail before submit
						decrementCount();
					}
					handleOnFailure(exception, openOrder != null ? openOrder.getOfferId() : "null",
						openOrder != null ? openOrder.getId() : "null");
				}

				private void handleOnFailure(KinEcosystemException exception, String offerId, String orderId) {
					if (isPayToUser) {
						eventLogger.send(PayToUserOrderFailed
							.create(ErrorUtil.getPrintableStackTrace(exception), offerId, orderId, true,
								PayToUserOrderFailed.Origin.EXTERNAL, String.valueOf(exception.getCode()),
								exception.getMessage()));
					} else {
						eventLogger.send(SpendOrderFailed
							.create(ErrorUtil.getPrintableStackTrace(exception), offerId, orderId, true,
								SpendOrderFailed.Origin.EXTERNAL, String.valueOf(exception.getCode()),
								exception.getMessage()));
					}

					if (callback != null) {
						callback.onFailure(exception);
					}
				}

			}).start();
	}

	@Override
	public void requestPayment(String offerJwt, final KinCallback<OrderConfirmation> callback) {
		eventLogger
			.send(EarnOrderCreationRequested.create(null, 0.0, "", true, EarnOrderCreationRequested.Origin.EXTERNAL));
		new ExternalEarnOrderCall(remoteData, blockchainSource, offerJwt, eventLogger, new ExternalOrderCallbacks() {
			@Override
			public void onOrderCreated(final OpenOrder openOrder) {
				cachedOpenOrder.postValue(openOrder);
				submitOrder(openOrder.getOfferId(), null, openOrder.getId(),
					kin.devplatform.network.model.Origin.EXTERNAL, new KinCallbackAdapter<Order>() {
						@Override
						public void onFailure(KinEcosystemException exception) {
							handleOnFailure(exception, openOrder.getOfferId(), openOrder.getId());
						}
					});
				eventLogger
					.send(EarnOrderCompletionSubmitted.create(openOrder.getOfferId(), openOrder.getId(), true,
						EarnOrderCompletionSubmitted.Origin.EXTERNAL));
			}

			@Override
			public void onOrderConfirmed(String confirmationJwt, Order order) {
				String offerID = "null";
				String orderId = "null";
				if (order != null) {
					offerID = order.getOfferId();
					orderId = order.getOrderId();
				}
				eventLogger.send(EarnOrderCompleted
					.create(offerID, orderId, true, EarnOrderCompleted.Origin.EXTERNAL,
						Double.valueOf(order.getAmount())));

				if (callback != null) {
					callback.onResponse(createOrderConfirmation(confirmationJwt));
				}

			}

			@Override
			public void onOrderFailed(KinEcosystemException exception, OpenOrder openOrder) {
				if (openOrder != null) { // did not fail before submit
					decrementCount();
				}
				handleOnFailure(exception, openOrder != null ? openOrder.getOfferId() : "null",
					openOrder != null ? openOrder.getId() : "null");
			}

			private void handleOnFailure(KinEcosystemException exception, String offerId, String orderId) {
				eventLogger
					.send(EarnOrderFailed.create(ErrorUtil.getPrintableStackTrace(exception), offerId, orderId, true,
						EarnOrderFailed.Origin.EXTERNAL, String.valueOf(exception.getCode()), exception.getMessage()));

				if (callback != null) {
					callback.onFailure(exception);
				}
			}

		}).start();
	}

	private OrderConfirmation createOrderConfirmation(String confirmationJwt) {
		OrderConfirmation orderConfirmation = new OrderConfirmation();
		orderConfirmation.setStatus(OrderConfirmation.Status.COMPLETED);
		orderConfirmation.setJwtConfirmation(confirmationJwt);
		return orderConfirmation;
	}

	@Override
	public void addOrderObserver(@NonNull Observer<Order> observer) {
		getOrderWatcher().addObserver(observer);
	}

	@Override
	public void removeOrderObserver(@NonNull Observer<Order> observer) {
		getOrderWatcher().removeObserver(observer);
	}

	@Override
	public void isFirstSpendOrder(@NonNull final KinCallback<Boolean> callback) {
		localData.isFirstSpendOrder(new Callback<Boolean, Void>() {
			@Override
			public void onResponse(Boolean response) {
				callback.onResponse(response);
			}

			@Override
			public void onFailure(Void t) {
				callback
					.onFailure(getClientException(INTERNAL_INCONSISTENCY, new DataNotAvailableException()));
			}
		});
	}

	@Override
	public void setIsFirstSpendOrder(boolean isFirstSpendOrder) {
		localData.setIsFirstSpendOrder(isFirstSpendOrder);
	}

	@Override
	public void getExternalOrderStatus(@NonNull String offerID,
		@NonNull final KinCallback<OrderConfirmation> callback) {
		remoteData
			.getFilteredOrderHistory(Origin.EXTERNAL.getValue(), offerID, new Callback<OrderList, ApiException>() {
				@Override
				public void onResponse(OrderList response) {
					if (response != null) {
						final List<Order> orders = response.getOrders();
						if (orders != null && orders.size() > 0) {
							final Order order = orders.get(orders.size() - 1);
							OrderConfirmation orderConfirmation = new OrderConfirmation();
							OrderConfirmation.Status status = OrderConfirmation.Status
								.fromValue(order.getStatus().getValue());
							orderConfirmation.setStatus(status);
							if (status == OrderConfirmation.Status.COMPLETED) {
								try {
									orderConfirmation
										.setJwtConfirmation(
											((JWTBodyPaymentConfirmationResult) order.getResult()).getJwt());
								} catch (ClassCastException e) {
									callback.onFailure(
										getClientException(INTERNAL_INCONSISTENCY, new DataNotAvailableException()));
								}

							}
							callback.onResponse(orderConfirmation);
						} else {
							callback.onFailure(
								getClientException(INTERNAL_INCONSISTENCY, new DataNotAvailableException()));
						}
					}
				}

				@Override
				public void onFailure(ApiException e) {
					callback.onFailure(ErrorUtil.fromApiException(e));
				}
			});
	}

	private void decrementCount() {
		decrementPendingOrdersCount();
		decrementPaymentObserverCount();
	}
}
