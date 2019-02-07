package kin.devplatform.settings.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import com.kin.ecosystem.recovery.BackupManager;
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

	private ISettingsPresenter settingsPresenter;

	private kin.devplatform.settings.view.SettingsItem backupItem;
	private kin.devplatform.settings.view.SettingsItem restoreItem;
	private AlertDialog migrationDialog;

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
			new BackupManager(this, BlockchainSourceImpl.getInstance().getKeyStoreProvider()),
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
	public void showMigrationStartedDialog() {
		showDialog(getString(R.string.kinecosystem_dialog_backup_migration_started_title),
			getString(R.string.kinecosystem_dialog_backup_migration_started_message));
	}

	@Override
	public void showMigrationFinishedDialog() {
		showDialog(getString(R.string.kinecosystem_dialog_backup_migration_finished_title),
			getString(R.string.kinecosystem_dialog_backup_migration_finished_message));
	}

	@Override
	public void showMigrationErrorDialog(Exception e) {
		showDialog(getString(R.string.kinecosystem_dialog_backup_migration_error_title),
			getString(R.string.kinecosystem_dialog_backup_migration_error_message, e.getCause(), e.getMessage()));
		// TODO: 07/02/2019 maybe we can add button which will let them retry the migration.
	}

	private void showDialog(String title, String message) {
		if (migrationDialog == null) {
			migrationDialog = new Builder(this, R.style.KinrecoveryAlertDialogTheme)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setCancelable(false)
				.create();
		} else {
			if (migrationDialog.isShowing()) {
				migrationDialog.dismiss();
			}
			migrationDialog.setTitle(title);
			migrationDialog.setMessage(message);
		}
		migrationDialog.show();
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
	public void showCouldNotImportAccount() {
		Toast.makeText(this, R.string.kinecosystem_could_not_restore_the_wallet, Toast.LENGTH_SHORT).show();
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
