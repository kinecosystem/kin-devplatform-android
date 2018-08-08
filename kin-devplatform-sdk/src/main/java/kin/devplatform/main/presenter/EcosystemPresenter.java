package kin.devplatform.main.presenter;


import static kin.devplatform.main.ScreenId.MARKETPLACE;
import static kin.devplatform.main.ScreenId.NONE;
import static kin.devplatform.main.ScreenId.ORDER_HISTORY;
import static kin.devplatform.main.Title.MARKETPLACE_TITLE;
import static kin.devplatform.main.Title.ORDER_HISTORY_TITLE;

import android.support.annotation.NonNull;
import kin.devplatform.base.BasePresenter;
import kin.devplatform.main.INavigator;
import kin.devplatform.main.ScreenId;
import kin.devplatform.main.Title;
import kin.devplatform.main.view.IEcosystemView;

public class EcosystemPresenter extends BasePresenter<IEcosystemView> implements IEcosystemPresenter {

	private @ScreenId
	int visibleScreen = NONE;
	private final INavigator navigator;

	public EcosystemPresenter(@NonNull IEcosystemView view, @NonNull INavigator navigator) {
		this.view = view;
		this.navigator = navigator;
		this.view.attachPresenter(this);
	}

	@Override
	public void onAttach(IEcosystemView view) {
		super.onAttach(view);
		if (this.view != null && visibleScreen != MARKETPLACE) {
			navigator.navigateToMarketplace();
		}
	}

	@Override
	public void balanceItemClicked() {
		if (view != null && visibleScreen != ORDER_HISTORY) {
			navigator.navigateToOrderHistory(false);
		}
	}

	@Override
	public void backButtonPressed() {
		if (view != null) {
			view.navigateBack();
		}
	}

	@Override
	public void visibleScreen(@ScreenId final int id) {
		visibleScreen = id;
		@Title final int title;
		switch (id) {
			case ORDER_HISTORY:
				title = ORDER_HISTORY_TITLE;
				break;
			case MARKETPLACE:
			default:
				title = MARKETPLACE_TITLE;
				break;
		}

		if (view != null) {
			view.updateTitle(title);
		}
	}
}
