package kin.devplatform.settings.view;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.kin.ecosystem.recovery.BackupManager;
import kin.devplatform.Log;
import kin.devplatform.Logger;
import kin.devplatform.R;
import kin.devplatform.accountmanager.AccountManagerImpl;
import kin.devplatform.base.BaseToolbarActivity;
import kin.devplatform.bi.EventLoggerImpl;
import kin.devplatform.data.blockchain.BlockchainSourceImpl;
import kin.devplatform.data.settings.SettingsDataSourceImpl;
import kin.devplatform.data.settings.SettingsDataSourceLocal;
import kin.devplatform.settings.presenter.ISettingsPresenter;
import kin.devplatform.settings.presenter.SettingsPresenter;

public class SettingsActivity extends BaseToolbarActivity implements kin.devplatform.settings.view.ISettingsView,
	OnClickListener {

	private static final String TAG = SettingsActivity.class.getSimpleName();

	private ISettingsPresenter settingsPresenter;
	private kin.devplatform.settings.view.SettingsItem backupItem;
	private kin.devplatform.settings.view.SettingsItem restoreItem;
	private ProgressDialog progressDialog;
	private Button dialogButton;
	private ProgressBar progressBar;

	@Override
	protected int getLayoutRes() {
		return R.layout.kinecosystem_activity_settings;
	}

	@Override
	protected int getTitleRes() {
		return R.string.kinecosystem_settings;
	}

	@Override
	protected int getNavigationIcon() {
		return R.drawable.kinecosystem_ic_back_black;
	}

	@Override
	protected OnClickListener getNavigationClickListener() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				settingsPresenter.backClicked();
			}
		};
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		backupItem = findViewById(R.id.keep_your_kin_safe);
		restoreItem = findViewById(R.id.restore_prev_wallet);
		backupItem.setOnClickListener(this);
		restoreItem.setOnClickListener(this);

		settingsPresenter = new SettingsPresenter(this,
			new SettingsDataSourceImpl(new SettingsDataSourceLocal(getApplicationContext())),
			BlockchainSourceImpl.getInstance(),
			getBackupManager(),
			EventLoggerImpl.getInstance(), AccountManagerImpl.getInstance());
	}

	@Override
	protected void initViews() {

	}

	@Override
	public void attachPresenter(ISettingsPresenter presenter) {
		settingsPresenter = presenter;
		settingsPresenter.onAttach(this);
	}

	@Override
	public void onClick(View v) {
		final int vId = v.getId();
		if (vId == R.id.keep_your_kin_safe) {
			settingsPresenter.backupClicked();
		} else if (vId == R.id.restore_prev_wallet) {
			settingsPresenter.restoreClicked();
		}
	}

	@Override
	public void showMigrationStarted() {
		progressDialog.setMessage(getString(R.string.kinecosystem_dialog_backup_migration_started_message));
	}

	@Override
	public void showMigrationError(Exception e) {
		Logger.log(new Log().withTag(TAG)
			.put("showMigrationError", "cause = " + e.getCause() + ", message = " + e.getMessage()));
		progressDialog.setMessage(getString(R.string.kinecosystem_dialog_backup_migration_error_message));
		addDismissButtonToProgressDialog();
	}

	@Override
	public void showUpdateWalletAddressFinished(boolean didMigrationStarted) {
		String migrationFinishedMessage = getString(
			didMigrationStarted ? R.string.kinecosystem_dialog_migration_and_wallet_address_finished_message :
				R.string.kinecosystem_dialog_wallet_address_finished_message);
		progressDialog.setMessage(migrationFinishedMessage);
		addDismissButtonToProgressDialog();
	}

	@Override
	public void showUpdateWalletAddressError() {
		progressDialog.setMessage(getString(R.string.kinecosystem_dialog_wallet_address_error_message));
		addDismissButtonToProgressDialog();
	}

	@Override
	public void showUWalletWasNotCreatedInThisAppError() {
		progressDialog
			.setMessage(getString(R.string.kinecosystem_dialog_wallet_was_not_created_in_this_app_error_message));
		addDismissButtonToProgressDialog();
	}

	public void startWaiting() {
		progressDialog = new ProgressDialog(this) {
			@Override
			protected void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);
				ProgressBar progress = findViewById(android.R.id.progress);
				LinearLayout bodyLayout = (LinearLayout) progress.getParent();
				progressBar = (ProgressBar) bodyLayout.getChildAt(0);
				TextView messageText = (TextView) bodyLayout.getChildAt(1);
				messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
					getResources().getDimension(R.dimen.kinecosystem_dialog_after_restore_process));
				LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) progressBar.getLayoutParams();
				params.gravity = Gravity.CENTER_VERTICAL;
				progress.setLayoutParams(params);
			}
		};

		progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.kinecosystem_ok),
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});

		progressDialog.setCancelable(false);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage(getString(R.string.kinecosystem_dialog_wallet_address_start_update_message));
		progressDialog.show();
		dialogButton = progressDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		dialogButton.setVisibility(View.GONE);
	}

	private void addDismissButtonToProgressDialog() {
		if (dialogButton != null) {
			dialogButton.setVisibility(View.VISIBLE);
		}
		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}
	}

	@Override
	public void navigateBack() {
		onBackPressed();
		overridePendingTransition(0, R.anim.kinecosystem_slide_out_right);
	}

	@Override
	public void setIconColor(@Item final int item, @IconColor int color) {
		final kin.devplatform.settings.view.SettingsItem settingsItem = getSettingsItem(item);
		if (settingsItem != null) {
			final @ColorRes int colorRes = getColorRes(color);
			if (colorRes != -1) {
				settingsItem.changeIconColor(colorRes);
			}
		}
	}

	@Override
	public void changeTouchIndicatorVisibility(@Item final int item, final boolean isVisible) {
		final kin.devplatform.settings.view.SettingsItem settingsItem = getSettingsItem(item);
		if (settingsItem != null) {
			settingsItem.setTouchIndicatorVisibility(isVisible);
		}
	}

	@Override
	public BackupManager getBackupManager() {
		return new BackupManager(this, BlockchainSourceImpl.getInstance().getKeyStoreProvider());
	}

	private kin.devplatform.settings.view.SettingsItem getSettingsItem(@Item final int item) {
		switch (item) {
			case ITEM_BACKUP:
				return backupItem;
			case ITEM_RESTORE:
				return restoreItem;
			default:
				return null;
		}
	}

	private int getColorRes(@IconColor final int color) {
		switch (color) {
			case BLUE:
				return R.color.kinecosystem_hot_blue;
			case GRAY:
				return R.color.kinecosystem_gray_dark;
			default:
				return -1;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		settingsPresenter.onDetach();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		settingsPresenter.onActivityResult(requestCode, resultCode, data);
	}
}
