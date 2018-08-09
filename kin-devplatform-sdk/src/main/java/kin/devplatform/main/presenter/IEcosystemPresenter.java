package kin.devplatform.main.presenter;

import kin.devplatform.base.IBasePresenter;
import kin.devplatform.main.ScreenId;
import kin.devplatform.main.view.IEcosystemView;

public interface IEcosystemPresenter extends IBasePresenter<IEcosystemView> {

	void balanceItemClicked();

	void backButtonPressed();

	void visibleScreen(@ScreenId final int id);
}
