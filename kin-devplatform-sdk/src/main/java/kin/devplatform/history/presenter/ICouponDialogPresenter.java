package kin.devplatform.history.presenter;

import kin.devplatform.base.IBottomDialogPresenter;
import kin.devplatform.history.view.ICouponDialog;


public interface ICouponDialogPresenter extends IBottomDialogPresenter<ICouponDialog> {

	void redeemUrlClicked();
}
