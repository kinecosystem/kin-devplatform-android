package kin.devplatform.data.order;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import kin.devplatform.KinCallback;
import kin.devplatform.base.ObservableData;
import kin.devplatform.base.Observer;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.data.Callback;
import kin.devplatform.data.model.OrderConfirmation;
import kin.devplatform.network.model.OpenOrder;
import kin.devplatform.network.model.Order;
import kin.devplatform.network.model.OrderList;

public interface OrderDataSource {

	OrderList getAllCachedOrderHistory();

	void getAllOrderHistory(@NonNull final KinCallback<OrderList> callback);

	void createOrder(@NonNull final String offerID, final KinCallback<OpenOrder> callback);

	void submitOrder(@NonNull final String offerID, @Nullable String content, @NonNull String orderID,
		final KinCallback<Order> callback);

	void cancelOrder(@NonNull final String offerID, @NonNull final String orderID, final KinCallback<Void> callback);

	ObservableData<OpenOrder> getOpenOrder();

	void purchase(String offerJwt, @Nullable final KinCallback<OrderConfirmation> callback);

	void requestPayment(String offerJwt, KinCallback<OrderConfirmation> callback);

	void addOrderObserver(@NonNull final Observer<Order> observer);

	void removeOrderObserver(@NonNull final Observer<Order> observer);

	void isFirstSpendOrder(@NonNull final KinCallback<Boolean> callback);

	void setIsFirstSpendOrder(boolean isFirstSpendOrder);

	void getExternalOrderStatus(@NonNull String offerID, @NonNull final KinCallback<OrderConfirmation> callback);

	interface Local {

		void isFirstSpendOrder(@NonNull final Callback<Boolean, Void> callback);

		void setIsFirstSpendOrder(boolean isFirstSpendOrder);
	}

	interface Remote {

		void getAllOrderHistory(@NonNull final Callback<OrderList, ApiException> callback);

		void createOrder(@NonNull final String offerID, final Callback<OpenOrder, ApiException> callback);

		void submitOrder(@Nullable String content, @NonNull String orderID,
			final Callback<Order, ApiException> callback);

		void cancelOrder(@NonNull final String orderID, final Callback<Void, ApiException> callback);

		void cancelOrderSync(@NonNull final String orderID);

		void getOrder(String orderID, Callback<Order, ApiException> callback);

		Order getOrderSync(String orderID);

		OpenOrder createExternalOrderSync(String orderJwt) throws ApiException;

		void getFilteredOrderHistory(@Nullable String origin, @NonNull String offerID,
			@NonNull final Callback<OrderList, ApiException> callback);
	}
}
