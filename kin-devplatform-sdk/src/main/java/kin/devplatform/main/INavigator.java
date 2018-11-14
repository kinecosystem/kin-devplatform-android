package kin.devplatform.main;

public interface INavigator {

	void navigateToMarketplace();

	void navigateToOrderHistory(boolean isFirstSpendOrder);

	void navigateToSettings();

	void close();
}
