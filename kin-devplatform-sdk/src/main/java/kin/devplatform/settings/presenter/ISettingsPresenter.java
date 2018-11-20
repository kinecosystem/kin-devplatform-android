package kin.devplatform.settings.presenter;

import android.content.Intent;
import kin.devplatform.base.IBasePresenter;
import kin.devplatform.settings.view.ISettingsView;

public interface ISettingsPresenter extends IBasePresenter<ISettingsView> {

	void backupClicked();

	void restoreClicked();

	void onActivityResult(int requestCode, int resultCode, Intent data);

	void backClicked();
}
