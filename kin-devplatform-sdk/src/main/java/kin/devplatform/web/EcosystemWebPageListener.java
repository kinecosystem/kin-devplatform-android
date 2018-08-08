package kin.devplatform.web;

public interface EcosystemWebPageListener {

	void onPageLoaded();

	void onPageCancel();

	void onPageResult(String result);

	void onPageClosed();

	void showToolbar();

	void hideToolbar();
}
