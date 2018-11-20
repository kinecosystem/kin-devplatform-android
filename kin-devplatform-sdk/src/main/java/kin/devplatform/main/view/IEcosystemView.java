package kin.devplatform.main.view;


import kin.devplatform.base.IBaseView;
import kin.devplatform.main.Title;
import kin.devplatform.main.presenter.IEcosystemPresenter;

public interface IEcosystemView extends IBaseView<IEcosystemPresenter> {

	void updateTitle(@Title final int title);

	void navigateBack();

	void showMenuTouchIndicator(boolean isVisible);
}
