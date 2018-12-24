package kin.devplatform.data.order;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.List;
import java.util.Map;
import kin.devplatform.Log;
import kin.devplatform.Logger;
import kin.devplatform.core.network.ApiCallback;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.core.util.ExecutorsUtil;
import kin.devplatform.data.Callback;
import kin.devplatform.network.api.OrdersApi;
import kin.devplatform.network.model.EarnSubmission;
import kin.devplatform.network.model.ExternalOrderRequest;
import kin.devplatform.network.model.OpenOrder;
import kin.devplatform.network.model.Order;
import kin.devplatform.network.model.OrderList;

public class OrderRemoteData implements OrderDataSource.Remote {

	private static final String TAG = OrderRemoteData.class.getSimpleName();

	private static final int ORDERS_ITEMS_LIMIT = 100;
	private static final int ONE_ORDER_LIMIT = 1;

	private static volatile OrderRemoteData instance;

	private final OrdersApi ordersApi;
	private final ExecutorsUtil executorsUtil;

	private OrderRemoteData(@NonNull ExecutorsUtil executorsUtil) {
		this.ordersApi = new OrdersApi();
		this.executorsUtil = executorsUtil;
	}

	public static OrderRemoteData getInstance(@NonNull ExecutorsUtil executorsUtil) {
		if (instance == null) {
			synchronized (OrderRemoteData.class) {
				if (instance == null) {
					instance = new OrderRemoteData(executorsUtil);
				}
			}
		}
		return instance;
	}

	@Override
	public void getAllOrderHistory(@NonNull final Callback<OrderList, ApiException> callback) {
		getHistory(null, null, ORDERS_ITEMS_LIMIT, callback);
	}

	@Override
	public void createOrder(@NonNull final String offerID, @NonNull final Callback<OpenOrder, ApiException> callback) {
		try {
			ordersApi.createOrderAsync(offerID, "", new ApiCallback<OpenOrder>() {
				@Override
				public void onFailure(final ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onFailure(e);
						}
					});
				}

				@Override
				public void onSuccess(final OpenOrder result, int statusCode,
					Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onResponse(result);
						}
					});
				}

			});
		} catch (final ApiException e) {
			executorsUtil.mainThread().execute(new Runnable() {
				@Override
				public void run() {
					callback.onFailure(e);
				}
			});
		}
	}

	@Override
	public void submitOrder(@NonNull String content, @NonNull String orderID,
		@NonNull final Callback<Order, ApiException> callback) {
		try {
			ordersApi.submitOrderAsync(new EarnSubmission().content(content), orderID, "", new ApiCallback<Order>() {
				@Override
				public void onFailure(final ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onFailure(e);
						}
					});
				}

				@Override
				public void onSuccess(final Order result, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onResponse(result);
						}
					});
				}

			});
		} catch (final ApiException e) {
			executorsUtil.mainThread().execute(new Runnable() {
				@Override
				public void run() {
					callback.onFailure(e);
				}
			});
		}
	}

	@Override
	public void cancelOrder(@NonNull final String orderID, @Nullable final Callback<Void, ApiException> callback) {
		try {
			ordersApi.cancelOrderAsync(orderID, "", new ApiCallback<Void>() {
				@Override
				public void onFailure(final ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							if (callback != null) {
								callback.onFailure(e);
							}
						}
					});
				}

				@Override
				public void onSuccess(final Void result, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							if (callback != null) {
								callback.onResponse(result);
							}
						}
					});
				}

			});
		} catch (final ApiException e) {
			executorsUtil.mainThread().execute(new Runnable() {
				@Override
				public void run() {
					callback.onFailure(e);
				}
			});
		}
	}

	@Override
	public void cancelOrderSync(@NonNull String orderID) {
		try {
			ordersApi.cancelOrder(orderID, "");
		} catch (ApiException e) {
			Logger.log(new Log().withTag(TAG).priority(Log.ERROR).put("Cancel order", orderID)
				.put("sync failed, code", e.getCode()));
		}
	}

	@Override
	public void getOrder(String orderID, final Callback<Order, ApiException> callback) {
		new GetOrderPollingCall(this, orderID, new Callback<Order, ApiException>() {
			@Override
			public void onResponse(final Order result) {
				executorsUtil.mainThread().execute(new Runnable() {
					@Override
					public void run() {
						callback.onResponse(result);
					}
				});
			}

			@Override
			public void onFailure(final ApiException e) {
				executorsUtil.mainThread().execute(new Runnable() {
					@Override
					public void run() {
						callback.onFailure(e);
					}
				});
			}
		}).start();
	}

	@Override
	public Order getOrderSync(String orderID) {
		Order order = null;
		try {
			order = ordersApi.getOrder(orderID, "");
		} catch (ApiException e) {
			Logger.log(new Log().withTag(TAG).priority(Log.ERROR).put("Get order", orderID)
				.put("sync failed, code", e.getCode()));
		}
		return order;
	}

	public OpenOrder createExternalOrderSync(String orderJwt) throws ApiException {
		return ordersApi.createExternalOrder(new ExternalOrderRequest().jwt(orderJwt), "");
	}

	@Override
	public void getFilteredOrderHistory(@Nullable String origin, @NonNull String offerID,
		@NonNull Callback<OrderList, ApiException> callback) {
		getHistory(origin, offerID, ONE_ORDER_LIMIT, callback);
	}

	private void getHistory(@Nullable String origin, @Nullable String offerID, int limit,
		@NonNull final Callback<OrderList, ApiException> callback) {
		try {
			ordersApi.getHistoryAsync("", origin, offerID, limit, null, null, new ApiCallback<OrderList>() {
				@Override
				public void onFailure(final ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onFailure(e);
						}
					});
				}

				@Override
				public void onSuccess(final OrderList result, int statusCode,
					Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onResponse(result);
						}
					});
				}

			});
		} catch (final ApiException e) {
			executorsUtil.mainThread().execute(new Runnable() {
				@Override
				public void run() {
					callback.onFailure(e);
				}
			});
		}
	}
}
