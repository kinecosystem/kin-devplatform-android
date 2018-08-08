package kin.devplatform.base;

public interface IBottomDialogPresenter<T extends IBottomDialog> extends IBasePresenter<T> {

	void closeClicked();

	void bottomButtonClicked();
}
