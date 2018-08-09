package kin.devplatform.splash.view;

import kin.devplatform.base.IBaseView;
import kin.devplatform.splash.presenter.SplashPresenter;

public interface ISplashView extends IBaseView<SplashPresenter> {

	void navigateToMarketPlace();

	void navigateBack();

	void animateLoading();

	void stopLoading(boolean reset);

	void showToast(String msg);
}
