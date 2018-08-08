package kin.devplatform.history.view;

import kin.devplatform.base.IBottomDialog;
import kin.devplatform.history.presenter.ICouponDialogPresenter;

public interface ICouponDialog extends IBottomDialog<ICouponDialogPresenter> {

	void copyCouponCode(String couponCode);

	void setUpRedeemDescription(String description, String clickableText, String url);

	void setupCouponCode(String code);

	void openUrl(String url);
}
