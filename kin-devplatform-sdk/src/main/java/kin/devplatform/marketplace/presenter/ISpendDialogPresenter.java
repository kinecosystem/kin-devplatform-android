package kin.devplatform.marketplace.presenter;

import kin.devplatform.base.IBottomDialogPresenter;
import kin.devplatform.marketplace.view.ISpendDialog;

public interface ISpendDialogPresenter extends IBottomDialogPresenter<ISpendDialog> {

	void dialogDismissed();
}
