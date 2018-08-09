package kin.devplatform.history.presenter;


import android.support.annotation.NonNull;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import kin.devplatform.KinCallback;
import kin.devplatform.base.BasePresenter;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.OrderHistoryItemTapped;
import kin.devplatform.bi.events.OrderHistoryPageViewed;
import kin.devplatform.bi.events.SpendRedeemPageViewed.RedeemTrigger;
import kin.devplatform.data.model.Coupon;
import kin.devplatform.data.model.Coupon.CouponInfo;
import kin.devplatform.data.order.OrderDataSource;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.history.view.IOrderHistoryView;
import kin.devplatform.network.model.CouponCodeResult;
import kin.devplatform.network.model.Order;
import kin.devplatform.network.model.Order.Status;
import kin.devplatform.network.model.OrderList;

public class OrderHistoryPresenter extends BasePresenter<IOrderHistoryView> implements IOrderHistoryPresenter {

	private static final int NOT_FOUND = -1;
	private final OrderDataSource orderRepository;
	private final EventLogger eventLogger;

	private List<Order> orderHistoryList = new ArrayList<>();
	private Observer<Order> completedOrderObserver;
	private final Gson gson;

	private boolean isFirstSpendOrder;

	public OrderHistoryPresenter(@NonNull IOrderHistoryView view,
		@NonNull final OrderDataSource orderRepository,
		@NonNull final EventLogger eventLogger,
		boolean isFirstSpendOrder) {
		this.view = view;
		this.orderRepository = orderRepository;
		this.eventLogger = eventLogger;
		this.isFirstSpendOrder = isFirstSpendOrder;
		this.gson = new Gson();

		view.attachPresenter(this);
	}

	@Override
	public void onAttach(IOrderHistoryView view) {
		super.onAttach(view);
		eventLogger.send(OrderHistoryPageViewed.create());
		getOrderHistoryList();
		listenToCompletedOrders();
	}

	private void getOrderHistoryList() {
		OrderList cachedOrderListObj = orderRepository.getAllCachedOrderHistory();
		List<Order> cachedList = removePendingOrders(cachedOrderListObj);
		setOrderHistoryList(cachedList);
		orderRepository.getAllOrderHistory(new KinCallback<OrderList>() {
			@Override
			public void onResponse(OrderList orderHistoryList) {
				syncNewOrders(orderHistoryList);
			}

			@Override
			public void onFailure(KinEcosystemException exception) {

			}
		});
	}

	private void syncNewOrders(OrderList newOrdersListObj) {
		List<Order> newList = removePendingOrders(newOrdersListObj);
		if (orderHistoryList.size() > 0) {
			//the oldest order is the last one, so we'll go from the last and add the top
			//we will end with newest order at the top.
			for (int i = newList.size() - 1; i >= 0; i--) {
				Order order = newList.get(i);
				int index = orderHistoryList.indexOf(order);
				if (index == NOT_FOUND) {
					//add at top (ui orientation)
					orderHistoryList.add(0, order);
					notifyItemInserted();
				} else {
					//Update
					orderHistoryList.set(index, order);
					notifyItemUpdated(index);
				}
			}

		} else {
			setOrderHistoryList(newList);
		}
	}

	private void setOrderHistoryList(List<Order> orders) {
		orderHistoryList = orders;
		if (view != null) {
			view.updateOrderHistoryList(orderHistoryList);
		}
	}

	private List<Order> removePendingOrders(OrderList orderListObj) {
		List<Order> orderList = new ArrayList<>();
		if (orderListObj != null && orderListObj.getOrders() != null) {
			for (Order order : orderListObj.getOrders()) {
				if (order.getStatus() != Status.PENDING) {
					orderList.add(order);
				}
			}
		}
		return orderList;
	}

	private void listenToCompletedOrders() {
		completedOrderObserver = new Observer<Order>() {
			@Override
			public void onChanged(Order order) {
				Status status = order.getStatus();
				if (status == Status.FAILED || status == Status.COMPLETED) {
					addOrderOrUpdate(order);
					if (status == Status.COMPLETED && isFirstSpendOrder) {
						showCouponDialog(RedeemTrigger.SYSTEM_INIT, order);
					}
				}
			}
		};
		orderRepository.addOrderObserver(completedOrderObserver);
	}

	private void addOrderOrUpdate(Order order) {
		int index = orderHistoryList.indexOf(order);
		if (index == NOT_FOUND) {
			orderHistoryList.add(0, order);
			notifyItemInserted();
		} else {
			orderHistoryList.set(index, order);
			notifyItemUpdated(index);
		}
	}

	private void notifyItemInserted() {
		if (view != null) {
			view.onItemInserted();
		}
	}

	private void notifyItemUpdated(int index) {
		if (view != null) {
			view.onItemUpdated(index);
		}
	}

	@Override
	public void onItemCLicked(int position) {
		Order order = orderHistoryList.get(position);
		if (order != null) {
			eventLogger.send(OrderHistoryItemTapped.create(order.getOfferId(), order.getOrderId()));
			showCouponDialog(RedeemTrigger.USER_INIT, order);
		}
	}

	private void showCouponDialog(RedeemTrigger redeemTrigger, Order order) {
		Coupon coupon = deserializeCoupon(order);
		if (view != null && coupon != null) {
			view.showCouponDialog(createCouponDialogPresenter(coupon, order, redeemTrigger));
		}
	}

	private ICouponDialogPresenter createCouponDialogPresenter(Coupon coupon, Order order,
		RedeemTrigger redeemTrigger) {
		return new CouponDialogPresenter(coupon, order, redeemTrigger, eventLogger);
	}

	private Coupon deserializeCoupon(Order order) {
		try {
			CouponInfo couponInfo = gson.fromJson(order.getContent(), CouponInfo.class);
			CouponCodeResult couponCodeResult = (CouponCodeResult) order.getResult();
			if (couponCodeResult.getCode() != null) {
				return new Coupon(couponInfo, couponCodeResult);
			} else {
				return null;
			}
		} catch (Exception t) {
			return null;
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		orderRepository.removeOrderObserver(completedOrderObserver);
	}
}
