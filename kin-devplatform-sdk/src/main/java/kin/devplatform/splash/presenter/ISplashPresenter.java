package kin.devplatform.splash.presenter;

import kin.devplatform.base.IBasePresenter;
import kin.devplatform.splash.view.ISplashView;

public interface ISplashPresenter extends IBasePresenter<ISplashView> {

	void getStartedClicked();

	void backButtonPressed();

	void onAnimationEnded();
}
