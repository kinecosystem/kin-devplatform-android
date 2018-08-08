package kin.devplatform.base;

public interface IBasePresenter<T extends IBaseView> {

	void onAttach(T view);

	void onDetach();

	T getView();
}
