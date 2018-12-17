package kin.devplatform.main.presenter;


import static kin.devplatform.main.ScreenId.MARKETPLACE;
import static kin.devplatform.main.ScreenId.NONE;
import static kin.devplatform.main.ScreenId.ORDER_HISTORY;
import static kin.devplatform.main.Title.MARKETPLACE_TITLE;
import static kin.devplatform.main.Title.ORDER_HISTORY_TITLE;

import android.os.Bundle;
import android.support.annotation.NonNull;
import java.math.BigDecimal;
import kin.devplatform.base.BasePresenter;
import kin.devplatform.base.Observer;
import kin.devplatform.data.blockchain.BlockchainSource;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.settings.SettingsDataSource;
import kin.devplatform.main.INavigator;
import kin.devplatform.main.ScreenId;
import kin.devplatform.main.Title;
import kin.devplatform.main.view.IEcosystemView;

public class EcosystemPresenter extends BasePresenter<IEcosystemView> implements IEcosystemPresenter {

	private static final String KEY_SCREEN_ID = "screen_id";
	private @ScreenId
	int visibleScreen = NONE;
	private INavigator navigator;
	private final SettingsDataSource settingsDataSource;
	private final BlockchainSource blockchainSource;

	private Observer<Balance> balanceObserver;
	private Balance currentBalance;

	public EcosystemPresenter(@NonNull IEcosystemView view, @NonNull SettingsDataSource settingsDataSource,
		@NonNull final BlockchainSource blockchainSource,
		@NonNull INavigator navigator, Bundle savedInstanceState) {
		this.view = view;
		this.settingsDataSource = settingsDataSource;
		this.blockchainSource = blockchainSource;
		this.navigator = navigator;
		this.currentBalance = blockchainSource.getBalance();
		this.visibleScreen = getVisibleScreen(savedInstanceState);

		this.view.attachPresenter(this);
	}

	private int getVisibleScreen(Bundle savedInstanceState) {
		return savedInstanceState != null ? savedInstanceState.getInt(KEY_SCREEN_ID, NONE) : NONE;
	}

	@Override
	public void onAttach(IEcosystemView view) {
		super.onAttach(view);
		navigateToVisibleScreen(visibleScreen);
	}

	private void navigateToVisibleScreen(int visibleScreen) {
		if (view != null) {
			switch (visibleScreen) {
				case ORDER_HISTORY:
					if (navigator != null) {
						navigator.navigateToOrderHistory(false);
					}
					break;
				case MARKETPLACE:
				case NONE:
				default:
					if (navigator != null) {
						navigator.navigateToMarketplace();
					}
					break;

			}
		}
	}

	@Override
	public void onStart() {
		updateMenuSettingsIcon();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(KEY_SCREEN_ID, visibleScreen);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		removeBalanceObserver();
		navigator = null;

	}

	private void addBalanceObserver() {
		removeBalanceObserver();
		balanceObserver = new Observer<Balance>() {
			@Override
			public void onChanged(Balance value) {
				currentBalance = value;
				if (isGreaterThenZero(value)) {
					updateMenuSettingsIcon();
				}
			}
		};
		blockchainSource.addBalanceObserver(balanceObserver);
	}

	private boolean isGreaterThenZero(Balance value) {
		return value.getAmount().compareTo(BigDecimal.ZERO) == 1;
	}

	private void updateMenuSettingsIcon() {
		if (!settingsDataSource.isBackedUp()) {
			if (isGreaterThenZero(currentBalance)) {
				changeMenuTouchIndicator(true);
				removeBalanceObserver();
			} else {
				addBalanceObserver();
				changeMenuTouchIndicator(false);
			}
		} else {
			changeMenuTouchIndicator(false);
		}
	}

	private void removeBalanceObserver() {
		if (balanceObserver != null) {
			blockchainSource.removeBalanceObserver(balanceObserver);
			balanceObserver = null;
		}
	}

	@Override
	public void balanceItemClicked() {
		if (view != null && visibleScreen != ORDER_HISTORY && navigator != null) {
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

	private void changeMenuTouchIndicator(final boolean isVisible) {
		if (view != null) {
			view.showMenuTouchIndicator(isVisible);
		}
	}

	@Override
	public void settingsMenuClicked() {
		if (navigator != null) {
			navigator.navigateToSettings();
		}
	}

	@Override
	public void onMenuInitialized() {
		updateMenuSettingsIcon();
	}
}

