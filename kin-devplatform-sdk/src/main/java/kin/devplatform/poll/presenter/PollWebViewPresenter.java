package kin.devplatform.poll.presenter;


import static kin.devplatform.poll.view.IPollWebView.ORDER_SUBMISSION_FAILED;
import static kin.devplatform.poll.view.IPollWebView.SOMETHING_WENT_WRONG;

import android.support.annotation.NonNull;
import kin.devplatform.KinCallback;
import kin.devplatform.base.BasePresenter;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.CloseButtonOnOfferPageTapped;
import kin.devplatform.bi.events.EarnOrderCancelled;
import kin.devplatform.bi.events.EarnOrderCompletionSubmitted;
import kin.devplatform.bi.events.EarnOrderCreationFailed;
import kin.devplatform.bi.events.EarnOrderCreationReceived;
import kin.devplatform.bi.events.EarnOrderCreationRequested;
import kin.devplatform.bi.events.EarnOrderCreationRequested.Origin;
import kin.devplatform.bi.events.EarnOrderFailed;
import kin.devplatform.bi.events.EarnPageLoaded;
import kin.devplatform.data.order.OrderDataSource;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.network.model.OpenOrder;
import kin.devplatform.network.model.Order;
import kin.devplatform.poll.view.IPollWebView;
import kin.devplatform.poll.view.IPollWebView.Message;
import kin.devplatform.util.ErrorUtil;


public class PollWebViewPresenter extends BasePresenter<IPollWebView> implements IPollWebViewPresenter {

	private final OrderDataSource orderRepository;
	private final EventLogger eventLogger;

	private final String pollJsonString;
	private final String offerID;
	private final String contentType;
	private final int amount;
	private final String title;

	private Observer<OpenOrder> openOrderObserver;
	private OpenOrder openOrder;
	private boolean isOrderSubmitted = false;

	public PollWebViewPresenter(@NonNull final String pollJsonString, @NonNull final String offerID,
		@NonNull final String contentType, final int amount,
		String title, @NonNull final OrderDataSource orderRepository, @NonNull EventLogger eventLogger) {
		this.pollJsonString = pollJsonString;
		this.offerID = offerID;
		this.contentType = contentType;
		this.amount = amount;
		this.title = title;
		this.orderRepository = orderRepository;
		this.eventLogger = eventLogger;
	}

	@Override
	public void onAttach(IPollWebView view) {
		super.onAttach(view);
		loadUrl();
		setTitle(title);
		listenToOpenOrders();
		createOrder();
	}

	private void loadUrl() {
		if (view != null) {
			view.loadUrl();
		}
	}

	private void setTitle(String title) {
		if (view != null) {
			view.setTitle(title);
		}
	}

	private void createOrder() {
		try {
			eventLogger.send(EarnOrderCreationRequested
				.create(EarnOrderCreationRequested.OfferType.fromValue(contentType), (double) amount, offerID, false,
					Origin.MARKETPLACE));
		} catch (IllegalArgumentException ex) {
			//TODO: add general error event
		}
		orderRepository.createOrder(offerID, new KinCallback<OpenOrder>() {
			@Override
			public void onResponse(OpenOrder response) {
				eventLogger.send(EarnOrderCreationReceived
					.create(offerID, response != null ? response.getId() : null, false,
						EarnOrderCreationReceived.Origin.MARKETPLACE));
				// we are listening to open orders.
			}

			@Override
			public void onFailure(KinEcosystemException exception) {
				showToast(SOMETHING_WENT_WRONG);
				eventLogger
					.send(EarnOrderCreationFailed.create(ErrorUtil.getPrintableStackTrace(exception), offerID,
						false, EarnOrderCreationFailed.Origin.MARKETPLACE, String.valueOf(exception.getCode()), ""));
				closeView();
			}
		});
	}

	@Override
	public void onDetach() {
		super.onDetach();
		release();
	}

	private void release() {
		if (openOrderObserver != null) {
			orderRepository.getOpenOrder().removeObserver(openOrderObserver);
		}
	}

	@Override
	public void onPageLoaded() {
		try {
			eventLogger.send(EarnPageLoaded.create(EarnPageLoaded.OfferType.fromValue(contentType)));

		} catch (IllegalArgumentException ex) {
			//TODO: add general error event
		}
		if (view != null) {
			view.renderJson(pollJsonString);
		}
	}


	@Override
	public void closeClicked() {
		eventLogger.send(CloseButtonOnOfferPageTapped.create(offerID, getOrderId()));
		cancelOrderAndClose();
	}

	@Override
	public void onPageCancel() {
		cancelOrderAndClose();
	}

	private void cancelOrderAndClose() {
		if (openOrder != null && !isOrderSubmitted) {
			String orderID = openOrder.getId();
			orderRepository.cancelOrder(offerID, orderID, null);
			eventLogger.send(EarnOrderCancelled.create(offerID, orderID));
		}
		closeView();
	}

	@Override
	public void onPageResult(String result) {
		if (openOrder != null) {
			isOrderSubmitted = true;
			final String orderId = openOrder.getId();
			eventLogger.send(EarnOrderCompletionSubmitted
				.create(offerID, orderId, false, EarnOrderCompletionSubmitted.Origin.MARKETPLACE));
			orderRepository.submitOrder(offerID, result, orderId, kin.devplatform.network.model.Origin.MARKETPLACE,
				new KinCallback<Order>() {
					@Override
					public void onResponse(Order response) {

					}

					@Override
					public void onFailure(KinEcosystemException exception) {
						EarnOrderFailed.create(ErrorUtil.getPrintableStackTrace(exception), offerID, orderId, false,
							EarnOrderFailed.Origin.MARKETPLACE, String.valueOf(exception.getCode()), "");
						showToast(ORDER_SUBMISSION_FAILED);
					}
				});
		}
	}

	private String getOrderId() {
		return openOrder != null ? openOrder.getId() : "null";
	}

	@Override
	public void onPageClosed() {
		closeView();
	}

	private void listenToOpenOrders() {
		openOrderObserver = new Observer<OpenOrder>() {
			@Override
			public void onChanged(OpenOrder value) {
				openOrder = value;
			}
		};
		orderRepository.getOpenOrder().addObserver(openOrderObserver);
	}

	private void showToast(@Message final int msg) {
		if (view != null) {
			view.showToast(msg);
		}
	}

	private void closeView() {
		if (view != null) {
			view.close();
		}
	}

	@Override
	public void showToolbar() {
		if (view != null) {
			view.showToolbar();
		}
	}

	@Override
	public void hideToolbar() {
		if (view != null) {
			view.hideToolbar();
		}
	}
}
