package kin.devplatform.main.presenter;

import android.os.Bundle;
import kin.devplatform.base.IBasePresenter;
import kin.devplatform.main.ScreenId;
import kin.devplatform.main.view.IEcosystemView;

public interface IEcosystemPresenter extends IBasePresenter<IEcosystemView> {

	void balanceItemClicked();

	void backButtonPressed();

	void visibleScreen(@ScreenId final int id);

	void settingsMenuClicked();

	void onMenuInitialized();

	void onStart();

	void onStop();

	void onSaveInstanceState(Bundle outState);
}
