package kin.devplatform.base;

public interface IBaseView<T extends IBasePresenter> {

	void attachPresenter(T presenter);
}
