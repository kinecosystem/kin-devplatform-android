package kin.devplatform.balance.view;

import kin.devplatform.balance.presenter.BalancePresenter.OrderStatus;
import kin.devplatform.balance.presenter.BalancePresenter.OrderType;
import kin.devplatform.balance.presenter.IBalancePresenter;
import kin.devplatform.base.IBaseView;

public interface IBalanceView extends IBaseView<IBalancePresenter> {

	void updateBalance(int balance);

	void setWelcomeSubtitle();

	void updateSubTitle(final int amount, @OrderStatus final int status, @OrderType final int orderType);

	void clearSubTitle();

	void animateArrow(boolean showArrow);
}
