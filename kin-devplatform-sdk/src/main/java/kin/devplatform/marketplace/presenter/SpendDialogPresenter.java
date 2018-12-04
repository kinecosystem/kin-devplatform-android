package kin.devplatform.marketplace.presenter;

import android.os.Handler;
import java.math.BigDecimal;
import kin.devplatform.KinCallback;
import kin.devplatform.base.BaseDialogPresenter;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.CloseButtonOnOfferPageTapped;
import kin.devplatform.bi.events.ConfirmPurchaseButtonTapped;
import kin.devplatform.bi.events.ConfirmPurchasePageViewed;
import kin.devplatform.bi.events.SpendOrderCancelled;
import kin.devplatform.bi.events.SpendOrderCompletionSubmitted;
import kin.devplatform.bi.events.SpendOrderCompletionSubmitted.Origin;
import kin.devplatform.bi.events.SpendOrderCreationFailed;
import kin.devplatform.bi.events.SpendOrderCreationReceived;
import kin.devplatform.bi.events.SpendOrderCreationRequested;
import kin.devplatform.bi.events.SpendThankyouPageViewed;
import kin.devplatform.data.KinCallbackAdapter;
import kin.devplatform.data.blockchain.BlockchainSource;
import kin.devplatform.data.order.OrderDataSource;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.marketplace.view.ISpendDialog;
import kin.devplatform.network.model.Offer;
import kin.devplatform.network.model.Offer.OfferType;
import kin.devplatform.network.model.OfferInfo;
import kin.devplatform.network.model.OfferInfo.Confirmation;
import kin.devplatform.network.model.OpenOrder;
import kin.devplatform.util.ErrorUtil;


public class SpendDialogPresenter extends BaseDialogPresenter<ISpendDialog> implements ISpendDialogPresenter {

	private final OrderDataSource orderRepository;
	private final BlockchainSource blockchainSource;
	private final EventLogger eventLogger;

	private final Handler handler = new Handler();

	private final OfferInfo offerInfo;
	private final Offer offer;
	private OpenOrder openOrder;

	private final BigDecimal amount;

	private boolean isUserConfirmedPurchase;
	private boolean isSubmitted;

	private static final int CLOSE_DELAY = 2000;

	SpendDialogPresenter(OfferInfo offerInfo, Offer offer, BlockchainSource blockchainSource,
		OrderDataSource orderRepository, EventLogger eventLogger) {
		this.offerInfo = offerInfo;
		this.offer = offer;
		this.orderRepository = orderRepository;
		this.blockchainSource = blockchainSource;
		this.eventLogger = eventLogger;
		this.amount = new BigDecimal(offer.getAmount());
	}

	@Override
	public void onAttach(final ISpendDialog view) {
		super.onAttach(view);
		createOrder();
		loadInfo();
		eventLogger.send(ConfirmPurchasePageViewed.create(amount.doubleValue(), offer.getId(), getOrderID()));
	}

	private void createOrder() {
		eventLogger.send(
			SpendOrderCreationRequested.create(offer.getId(), SpendOrderCreationRequested.Origin.MARKETPLACE));
		orderRepository.createOrder(offer.getId(), new KinCallback<OpenOrder>() {
			@Override
			public void onResponse(OpenOrder response) {
				openOrder = response;
				eventLogger.send(SpendOrderCreationReceived
					.create(offer.getId(), response != null ? response.getId() : null,
						SpendOrderCreationReceived.Origin.MARKETPLACE));
				if (isUserConfirmedPurchase && !isSubmitted) {
					submitAndSendTransaction();
				}
			}

			@Override
			public void onFailure(KinEcosystemException exception) {
				showToast("Oops something went wrong...");
				eventLogger.send(SpendOrderCreationFailed
					.create(ErrorUtil.getPrintableStackTrace(exception), offer.getId(),
						SpendOrderCreationFailed.Origin.MARKETPLACE, String.valueOf(exception.getCode()),
						exception.getMessage()));
			}
		});
	}

	private void loadInfo() {
		if (view != null) {
			view.setupImage(offerInfo.getImage());
			view.setupTitle(offerInfo.getTitle(), offerInfo.getAmount());
			view.setupDescription(offerInfo.getDescription());
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void closeClicked() {
		eventLogger.send(CloseButtonOnOfferPageTapped.create(offer.getId(), getOrderID()));
		closeDialog();
	}

	@Override
	public void bottomButtonClicked() {
		isUserConfirmedPurchase = true;
		eventLogger.send(ConfirmPurchaseButtonTapped.create(amount.doubleValue(), offer.getId(), getOrderID()));
		if (view != null) {
			Confirmation confirmation = offerInfo.getConfirmation();
			view.showThankYouLayout(confirmation.getTitle(), confirmation.getDescription());
			eventLogger.send(SpendThankyouPageViewed.create(amount.doubleValue(), offer.getId(), getOrderID()));
			closeDialogWithDelay(CLOSE_DELAY);
		}

		submitAndSendTransaction();
	}

	private void submitAndSendTransaction() {
		if (openOrder != null) {
			isSubmitted = true;
			final String addressee = offer.getBlockchainData().getRecipientAddress();
			final String orderID = openOrder.getId();

			submitOrder(offer.getId(), orderID);
			sendTransaction(addressee, amount, orderID);
		}
	}

	@Override
	public void dialogDismissed() {
		if (isUserConfirmedPurchase) {
			orderRepository.isFirstSpendOrder(new KinCallbackAdapter<Boolean>() {
				@Override
				public void onResponse(Boolean response) {
					if (response) {
						navigateToOrderHistory();
						orderRepository.setIsFirstSpendOrder(false);
					}
					onDetach();
				}
			});
		} else {
			if (openOrder != null) {
				final String offerId = offer.getId();
				final String orderId = openOrder.getId();
				eventLogger.send(SpendOrderCancelled.create(offerId, orderId));
				orderRepository.cancelOrder(offerId, orderId, null);
			}
		}
	}

	private void navigateToOrderHistory() {
		if (view != null) {
			view.navigateToOrderHistory();
		}
	}

	private void closeDialogWithDelay(int delayMilliseconds) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				closeDialog();
			}
		}, delayMilliseconds);
	}

	private void sendTransaction(String addressee, BigDecimal amount, String orderID) {
		blockchainSource.sendTransaction(addressee, amount, orderID, offer.getId(), OfferType.SPEND);
	}

	private void submitOrder(String offerID, String orderID) {
		eventLogger.send(SpendOrderCompletionSubmitted.create(offerID, orderID, Origin.MARKETPLACE));
		orderRepository.submitOrder(offerID, null, orderID, kin.devplatform.network.model.Origin.MARKETPLACE, null);
	}

	private void showToast(String msg) {
		if (view != null) {
			view.showToast(msg);
		}
	}

	private String getOrderID() {
		return openOrder != null ? openOrder.getId() : "null";
	}
}
