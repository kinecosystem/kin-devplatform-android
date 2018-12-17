package kin.devplatform.main.view;

import static kin.devplatform.main.ScreenId.MARKETPLACE;
import static kin.devplatform.main.ScreenId.ORDER_HISTORY;
import static kin.devplatform.main.Title.MARKETPLACE_TITLE;
import static kin.devplatform.main.Title.ORDER_HISTORY_TITLE;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import kin.devplatform.R;
import kin.devplatform.balance.presenter.BalancePresenter;
import kin.devplatform.balance.presenter.IBalancePresenter;
import kin.devplatform.balance.presenter.IBalancePresenter.BalanceClickListener;
import kin.devplatform.balance.view.IBalanceView;
import kin.devplatform.base.BaseToolbarActivity;
import kin.devplatform.bi.EventLoggerImpl;
import kin.devplatform.data.blockchain.BlockchainSourceImpl;
import kin.devplatform.data.offer.OfferRepository;
import kin.devplatform.data.order.OrderRepository;
import kin.devplatform.data.settings.SettingsDataSourceImpl;
import kin.devplatform.data.settings.SettingsDataSourceLocal;
import kin.devplatform.history.presenter.OrderHistoryPresenter;
import kin.devplatform.history.view.OrderHistoryFragment;
import kin.devplatform.main.INavigator;
import kin.devplatform.main.ScreenId;
import kin.devplatform.main.Title;
import kin.devplatform.main.presenter.EcosystemPresenter;
import kin.devplatform.main.presenter.IEcosystemPresenter;
import kin.devplatform.marketplace.presenter.IMarketplacePresenter;
import kin.devplatform.marketplace.presenter.MarketplacePresenter;
import kin.devplatform.marketplace.view.MarketplaceFragment;
import kin.devplatform.settings.view.SettingsActivity;


public class EcosystemActivity extends BaseToolbarActivity implements IEcosystemView, INavigator {

	public static final String ECOSYSTEM_MARKETPLACE_FRAGMENT_TAG = "ecosystem_marketplace_fragment_tag";
	public static final String ECOSYSTEM_ORDER_HISTORY_FRAGMENT_TAG = "ecosystem_order_history_fragment_tag";
	public static final String MARKETPLACE_TO_ORDER_HISTORY = "marketplace_to_order_history";

	private IBalancePresenter balancePresenter;
	private IEcosystemPresenter ecosystemPresenter;
	private IMarketplacePresenter marketplacePresenter;

	private View actionView;

	@Override
	protected int getLayoutRes() {
		return R.layout.kinecosystem_activity_main;
	}

	@Override
	protected int getTitleRes() {
		return R.string.kinecosystem_kin_marketplace;
	}

	@Override
	protected int getNavigationIcon() {
		return R.drawable.kinecosystem_ic_back_black;
	}

	@Override
	protected View.OnClickListener getNavigationClickListener() {
		return new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		};
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		IBalanceView balanceView = findViewById(R.id.balance_view);
		balancePresenter = new BalancePresenter(balanceView, EventLoggerImpl.getInstance(),
			BlockchainSourceImpl.getInstance(), OrderRepository.getInstance());
		balancePresenter.setClickListener(new BalanceClickListener() {
			@Override
			public void onClick() {
				ecosystemPresenter.balanceItemClicked();
			}
		});
		ecosystemPresenter = new EcosystemPresenter(this,
			new SettingsDataSourceImpl(new SettingsDataSourceLocal(getApplicationContext())),
			BlockchainSourceImpl.getInstance(), this, savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		ecosystemPresenter.onStart();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		ecosystemPresenter.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.kinecosystem_menu_marketplace, menu);
		setupActionView(menu);
		ecosystemPresenter.onMenuInitialized();
		return true;
	}

	private void setupActionView(final Menu menu) {
		final MenuItem settingsItem = menu.findItem(R.id.menu_settings);
		if (settingsItem != null) {
			actionView = MenuItemCompat.getActionView(settingsItem);
			actionView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ecosystemPresenter.settingsMenuClicked();
				}
			});
		}
	}

	@Override
	public void showMenuTouchIndicator(boolean isVisible) {
		if (actionView != null) {
			ImageView infoBadge = actionView.findViewById(R.id.ic_info_dot);
			if (infoBadge != null) {
				if (isVisible) {
					infoBadge.setVisibility(View.VISIBLE);
				} else {
					infoBadge.setVisibility(View.GONE);
				}
			}
		}
	}

	@Override
	public void updateTitle(@Title final int title) {
		final int titleResId;
		switch (title) {
			case ORDER_HISTORY_TITLE:
				titleResId = R.string.kinecosystem_transaction_history;
				break;
			case MARKETPLACE_TITLE:
			default:
				titleResId = R.string.kinecosystem_kin_marketplace;
				break;
		}
		getToolbar().setTitle(titleResId);
	}

	@Override
	public void navigateToMarketplace() {
		MarketplaceFragment marketplaceFragment = getSavedMarketplaceFragment();
		if (marketplaceFragment == null) {
			marketplaceFragment = MarketplaceFragment.newInstance();
		}

		marketplacePresenter = getMarketplacePresenter(marketplaceFragment);

		getSupportFragmentManager().beginTransaction()
			.replace(R.id.fragment_frame, marketplaceFragment, ECOSYSTEM_MARKETPLACE_FRAGMENT_TAG)
			.commit();

		setVisibleScreen(MARKETPLACE);
	}

	private MarketplaceFragment getSavedMarketplaceFragment() {
		return (MarketplaceFragment) getSupportFragmentManager()
			.findFragmentByTag(ECOSYSTEM_MARKETPLACE_FRAGMENT_TAG);
	}

	private IMarketplacePresenter getMarketplacePresenter(MarketplaceFragment marketplaceFragment) {
		if (marketplacePresenter == null) {
			marketplacePresenter = new MarketplacePresenter(marketplaceFragment, OfferRepository.getInstance(),
				OrderRepository.getInstance(),
				BlockchainSourceImpl.getInstance(),
				this,
				EventLoggerImpl.getInstance());
		}

		return marketplacePresenter;
	}

	private void setVisibleScreen(@ScreenId final int id) {
		ecosystemPresenter.visibleScreen(id);
		balancePresenter.visibleScreen(id);
	}

	@Override
	public void navigateToOrderHistory(boolean isFirstSpendOrder) {
		OrderHistoryFragment orderHistoryFragment = (OrderHistoryFragment) getSupportFragmentManager()
			.findFragmentByTag(ECOSYSTEM_ORDER_HISTORY_FRAGMENT_TAG);
		boolean shouldAddToBackStack = true;
		if (orderHistoryFragment == null) {
			orderHistoryFragment = OrderHistoryFragment.newInstance();
		} else {
			shouldAddToBackStack = false;
		}

		new OrderHistoryPresenter(orderHistoryFragment,
			OrderRepository.getInstance(),
			EventLoggerImpl.getInstance(),
			isFirstSpendOrder);

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
			.setCustomAnimations(
				R.anim.kinecosystem_slide_in_right,
				R.anim.kinecosystem_slide_out_left,
				R.anim.kinrecovery_slide_in_left,
				R.anim.kinecosystem_slide_out_right)
			.replace(R.id.fragment_frame, orderHistoryFragment, ECOSYSTEM_ORDER_HISTORY_FRAGMENT_TAG);

		if (shouldAddToBackStack) {
			transaction.addToBackStack(MARKETPLACE_TO_ORDER_HISTORY);
		}

		transaction.commit();

		setVisibleScreen(ORDER_HISTORY);
	}

	@Override
	public void navigateToSettings() {
		Intent settingsIntent = new Intent(this, SettingsActivity.class);
		startActivity(settingsIntent);
		overridePendingTransition(R.anim.kinecosystem_slide_in_right, R.anim.kinecosystem_slide_out_left);
	}

	@Override
	public void close() {
		finish();
	}

	@Override
	public void attachPresenter(IEcosystemPresenter presenter) {
		ecosystemPresenter = presenter;
		ecosystemPresenter.onAttach(this);
	}

	@Override
	protected void initViews() {

	}

	@Override
	public void onBackPressed() {
		ecosystemPresenter.backButtonPressed();
	}

	@Override
	public void navigateBack() {
		int count = getSupportFragmentManager().getBackStackEntryCount();
		if (count == 0) {
			marketplacePresenter.backButtonPressed();
			super.onBackPressed();
			overridePendingTransition(0, R.anim.kinecosystem_slide_out_right);
		} else {
			BackStackEntry entry = getSupportFragmentManager().getBackStackEntryAt(count - 1);
			if (entry != null && entry.getName().equals(MARKETPLACE_TO_ORDER_HISTORY)) {
				// After pressing back from OrderHistory, should put the attrs again.
				// This is the only fragment that should set presenter again on back.
				MarketplaceFragment marketplaceFragment = getSavedMarketplaceFragment();
				if (marketplaceFragment != null) {
					if (marketplacePresenter == null) {
						getMarketplacePresenter(marketplaceFragment);
					} else {
						marketplaceFragment.attachPresenter(marketplacePresenter);
					}
				}
			}
			getSupportFragmentManager().popBackStackImmediate();
			setVisibleScreen(MARKETPLACE);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ecosystemPresenter.onDetach();
		if (balancePresenter != null) {
			balancePresenter.onDetach();
		}
	}
}