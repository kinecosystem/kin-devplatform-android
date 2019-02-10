package kin.devplatform.settings.presenter;

import static kin.devplatform.settings.view.ISettingsView.BLUE;
import static kin.devplatform.settings.view.ISettingsView.GRAY;
import static kin.devplatform.settings.view.ISettingsView.ITEM_BACKUP;

import android.content.Intent;
import android.support.annotation.NonNull;
import com.kin.ecosystem.recovery.BackupCallback;
import com.kin.ecosystem.recovery.BackupManager;
import com.kin.ecosystem.recovery.RestoreCallback;
import java.math.BigDecimal;
import kin.devplatform.KinCallback;
import kin.devplatform.Log;
import kin.devplatform.Logger;
import kin.devplatform.accountmanager.AccountManager;
import kin.devplatform.base.BasePresenter;
import kin.devplatform.base.Observer;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.RecoveryBackupEvents;
import kin.devplatform.bi.RecoveryRestoreEvents;
import kin.devplatform.bi.events.BackupWalletCompleted;
import kin.devplatform.bi.events.GeneralEcosystemSdkError;
import kin.devplatform.bi.events.RestoreWalletCompleted;
import kin.devplatform.data.blockchain.BlockchainSource;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.settings.SettingsDataSource;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.settings.view.ISettingsView;
import kin.devplatform.settings.view.ISettingsView.IconColor;
import kin.devplatform.settings.view.ISettingsView.Item;
import kin.devplatform.util.ErrorUtil;
import kin.sdk.migration.interfaces.IKinClient;
import kin.sdk.migration.interfaces.IMigrationManagerCallbacks;

public class SettingsPresenter extends BasePresenter<ISettingsView> implements ISettingsPresenter {

	private static final String TAG = SettingsPresenter.class.getSimpleName();
	private final BackupManager backupManager;
	private final SettingsDataSource settingsDataSource;
	private final BlockchainSource blockchainSource;
	private final EventLogger eventLogger;
	private final AccountManager accountManager;

	private Observer<Balance> balanceObserver;
	private Balance currentBalance;

	public SettingsPresenter(@NonNull final ISettingsView view, @NonNull final SettingsDataSource settingsDataSource,
		@NonNull final BlockchainSource blockchainSource, @NonNull final BackupManager backupManager,
		@NonNull final EventLogger eventLogger, AccountManager accountManager) {
		this.view = view;
		this.backupManager = backupManager;
		this.settingsDataSource = settingsDataSource;
		this.blockchainSource = blockchainSource;
		this.eventLogger = eventLogger;
		this.accountManager = accountManager;
		this.currentBalance = blockchainSource.getBalance();
		registerToEvents();
		registerToCallbacks();
		this.view.attachPresenter(this);
	}

	@Override
	public void onAttach(ISettingsView view) {
		super.onAttach(view);
		updateSettingsIcon();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		removeBalanceObserver();
		backupManager.release();
	}

	private void addBalanceObserver() {
		balanceObserver = new Observer<Balance>() {
			@Override
			public void onChanged(Balance value) {
				currentBalance = value;
				if (isGreaterThenZero(value)) {
					updateSettingsIcon();
				}
			}
		};
		blockchainSource.addBalanceObserver(balanceObserver, false);
	}

	private boolean isGreaterThenZero(Balance value) {
		return value.getAmount().compareTo(BigDecimal.ZERO) == 1;
	}

	private void updateSettingsIcon() {
		if (!settingsDataSource.isBackedUp()) {
			changeIconColor(ITEM_BACKUP, GRAY);
			if (isGreaterThenZero(currentBalance)) {
				changeTouchIndicator(ITEM_BACKUP, true);
				removeBalanceObserver();
			} else {
				addBalanceObserver();
				changeTouchIndicator(ITEM_BACKUP, false);
			}
		} else {
			changeIconColor(ITEM_BACKUP, BLUE);
			changeTouchIndicator(ITEM_BACKUP, false);
		}
	}

	private void removeBalanceObserver() {
		if (balanceObserver != null) {
			blockchainSource.removeBalanceObserver(balanceObserver, false);
		}
	}

	@Override
	public void backupClicked() {
		backupManager.backupFlow();
	}

	@Override
	public void restoreClicked() {
		backupManager.restoreFlow();
	}

	@Override
	public void backClicked() {
		if (view != null) {
			view.navigateBack();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		backupManager.onActivityResult(requestCode, resultCode, data);
	}

	private void registerToEvents() {
		backupManager.registerBackupEvents(new RecoveryBackupEvents(eventLogger));
		backupManager.registerRestoreEvents(new RecoveryRestoreEvents(eventLogger));
	}

	private void registerToCallbacks() {
		backupManager.registerBackupCallback(new BackupCallback() {
			@Override
			public void onSuccess() {
				Logger.log(new Log().withTag(TAG).put("BackupCallback", "onSuccess"));
				onBackupSuccess();
			}

			@Override
			public void onCancel() {
				Logger.log(new Log().withTag(TAG).put("BackupCallback", "onCancel"));
			}

			@Override
			public void onFailure(Throwable throwable) {
				Logger.log(new Log().withTag(TAG).put("BackupCallback", "onFailure"));
			}
		});

		backupManager.registerRestoreCallback(new RestoreCallback() {
			@Override
			public void onSuccess(int accountIndex) {
				Logger.log(new Log().withTag(TAG).put("RestoreCallback", "onSuccess"));
				if (view != null) {
					view.startWaiting();
				}
				switchAccount(accountIndex);
			}

			@Override
			public void onCancel() {
				Logger.log(new Log().withTag(TAG).put("RestoreCallback", "onCancel"));
			}

			@Override
			public void onFailure(Throwable throwable) {
				Logger.log(new Log().withTag(TAG).put("RestoreCallback", "onFailure"));
			}
		});
	}

	private void switchAccount(int accountIndex) {
		accountManager.switchAccount(accountIndex, new KinCallback<Boolean>() {
			@Override
			public void onResponse(Boolean response) {
				view.stopWaiting();
				eventLogger.send(RestoreWalletCompleted.create());
			}

			@Override
			public void onFailure(KinEcosystemException exception) {
				view.stopWaiting();
				eventLogger.send(GeneralEcosystemSdkError
					.create(ErrorUtil.getPrintableStackTrace(exception), String.valueOf(exception.getCode()),
						"SettingsPresenter.switchAccount onFailure"));
				showCouldNotImportAccountError();
			}
		}, new IMigrationManagerCallbacks() {

			@Override
			public void onMigrationStart() {
				if (view != null) {
					// TODO: 07/02/2019 confirm with Ayelet
					view.showMigrationStartedDialog();
				}
			}

			@Override
			public void onReady(IKinClient kinClient) {
				if (view != null) {
					// TODO: 07/02/2019 Maybe if it is not started then show different message because there were no actual migration.
					// TODO: 07/02/2019 Although it can start even if it was migrated, for example if the account was migrated but currently not saved locally in the device.
					// TODO: 07/02/2019 It can be solved only if we will add a bit complicate method which checks if an account is already migrated.

					// TODO: 07/02/2019 Also maybe it will be correct to update the user that he can go back and use the app only after the wallet address is finished and not here...
					view.showMigrationFinishedDialog();
				}
			}

			@Override
			public void onError(Exception e) {
				// TODO: 07/02/2019 confirm with Ayelet
				if (view != null) {
					view.stopWaiting();
					view.showMigrationErrorDialog(e);
				}
			}
		});
	}

	private void showCouldNotImportAccountError() {
		if (view != null) {
			view.showCouldNotImportAccount();
		}
	}

	private void onBackupSuccess() {
		eventLogger.send(BackupWalletCompleted.create());
		settingsDataSource.setIsBackedUp(true);
		updateSettingsIcon();
	}

	private void changeTouchIndicator(@Item final int item, final boolean isVisible) {
		if (view != null) {
			view.changeTouchIndicatorVisibility(item, isVisible);
		}
	}

	private void changeIconColor(@Item final int item, @IconColor final int color) {
		if (view != null) {
			view.setIconColor(item, color);
		}
	}
}
