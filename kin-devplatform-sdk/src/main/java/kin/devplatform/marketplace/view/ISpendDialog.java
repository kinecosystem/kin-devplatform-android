package kin.devplatform.marketplace.view;

import android.support.annotation.NonNull;
import kin.devplatform.base.IBottomDialog;
import kin.devplatform.marketplace.presenter.ISpendDialogPresenter;

public interface ISpendDialog extends IBottomDialog<ISpendDialogPresenter> {

	void showThankYouLayout(@NonNull final String title, @NonNull final String description);

	void navigateToOrderHistory();
}