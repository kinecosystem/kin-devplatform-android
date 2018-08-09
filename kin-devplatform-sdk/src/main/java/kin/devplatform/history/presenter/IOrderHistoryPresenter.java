package kin.devplatform.history.presenter;

import kin.devplatform.base.IBasePresenter;
import kin.devplatform.history.view.IOrderHistoryView;

public interface IOrderHistoryPresenter extends IBasePresenter<IOrderHistoryView> {

	void onItemCLicked(int position);
}
