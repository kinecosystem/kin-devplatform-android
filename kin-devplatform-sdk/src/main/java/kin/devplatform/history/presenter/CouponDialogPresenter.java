package kin.devplatform.history.presenter;

import android.support.annotation.NonNull;
import kin.devplatform.R;
import kin.devplatform.base.BaseDialogPresenter;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.RedeemUrlTapped;
import kin.devplatform.bi.events.SpendRedeemButtonTapped;
import kin.devplatform.bi.events.SpendRedeemPageViewed;
import kin.devplatform.bi.events.SpendRedeemPageViewed.RedeemTrigger;
import kin.devplatform.data.model.Coupon;
import kin.devplatform.data.model.Coupon.CouponInfo;
import kin.devplatform.history.view.ICouponDialog;
import kin.devplatform.network.model.Order;

public class CouponDialogPresenter extends BaseDialogPresenter<ICouponDialog> implements ICouponDialogPresenter {

	private final EventLogger eventLogger;
	private final Coupon coupon;
	private final Order order;
	private final RedeemTrigger redeemTrigger;
	private static final String HTTP_URL_PATTERN = "http://";
	private static final String HTTPS_URL_PATTERN = "https://";

	CouponDialogPresenter(@NonNull final Coupon coupon, @NonNull Order order, RedeemTrigger redeemTrigger,
		@NonNull EventLogger eventLogger) {
		this.eventLogger = eventLogger;
		this.coupon = coupon;
		this.order = order;
		this.redeemTrigger = redeemTrigger;
	}

	@Override
	public void onAttach(ICouponDialog view) {
		super.onAttach(view);
		loadInfo();
		eventLogger.send(SpendRedeemPageViewed
			.create(redeemTrigger, (double) order.getAmount(), order.getOfferId(), order.getOrderId()));
	}

	private void loadInfo() {
		if (view != null && coupon != null) {
			CouponInfo info = coupon.getCouponInfo();
			view.setupImage(info.getImage());
			view.setupTitle(info.getTitle());
			view.setUpRedeemDescription(info.getDescription(), info.getLink(), createUrl(info.getLink()));
			if (coupon.getCouponCode() != null) {
				view.setupCouponCode(coupon.getCouponCode().getCode());
			}
			view.setUpButtonText(R.string.kinecosystem_copy_code);
		}
	}

	private String createUrl(String link) {
		if (link.contains(HTTP_URL_PATTERN) || link.contains(HTTPS_URL_PATTERN)) {
			return link;
		} else {
			return HTTP_URL_PATTERN + link;
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	@Override
	public void closeClicked() {
		closeDialog();
	}

	@Override
	public void bottomButtonClicked() {
		eventLogger
			.send(SpendRedeemButtonTapped.create((double) order.getAmount(), order.getOfferId(), order.getOrderId()));
		copyCouponCodeToClipboard();
	}

	private void copyCouponCodeToClipboard() {
		if (view != null && coupon.getCouponCode() != null) {
			view.copyCouponCode(coupon.getCouponCode().getCode());
		}
	}

	@Override
	public void redeemUrlClicked() {
		eventLogger.send(RedeemUrlTapped.create());
		if (view != null) {
			String url = createUrl(coupon.getCouponInfo().getLink());
			view.openUrl(url);
		}
	}
}
