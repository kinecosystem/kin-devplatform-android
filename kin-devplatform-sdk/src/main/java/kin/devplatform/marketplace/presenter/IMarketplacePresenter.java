package kin.devplatform.marketplace.presenter;

import kin.devplatform.base.IBasePresenter;
import kin.devplatform.main.INavigator;
import kin.devplatform.marketplace.view.IMarketplaceView;
import kin.devplatform.network.model.Offer.OfferType;

public interface IMarketplacePresenter extends IBasePresenter<IMarketplaceView> {

	void getOffers();

	void onItemClicked(int position, OfferType offerType);

	void showOfferActivityFailed();

	void backButtonPressed();

	INavigator getNavigator();
}
