package kin.devplatform.base;

import android.support.annotation.StringRes;

public interface IBottomDialog<T extends IBottomDialogPresenter> extends IBaseView<T> {

	void closeDialog();

	void setupImage(String image);

	void setupTitle(String titleText);

	void setupTitle(String titleText, int amount);

	void setupDescription(String descriptionText);

	void setUpButtonText(@StringRes int stringRes);

	void showToast(String msg);
}
