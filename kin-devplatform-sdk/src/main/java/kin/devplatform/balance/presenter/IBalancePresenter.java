package kin.devplatform.balance.presenter;

import kin.devplatform.balance.view.IBalanceView;
import kin.devplatform.base.IBasePresenter;
import kin.devplatform.main.ScreenId;

public interface IBalancePresenter extends IBasePresenter<IBalanceView> {

	interface BalanceClickListener {

		void onClick();
	}

	void onStart();

	void onStop();

	void balanceClicked();

	void setClickListener(BalanceClickListener balanceClickListener);

	void visibleScreen(@ScreenId final int id);
}
