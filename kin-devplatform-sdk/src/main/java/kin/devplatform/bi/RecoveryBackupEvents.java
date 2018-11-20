package kin.devplatform.bi;

import android.support.annotation.NonNull;
import kin.devplatform.bi.events.BackupCompletedPageViewed;
import kin.devplatform.bi.events.BackupCreatePasswordBackButtonTapped;
import kin.devplatform.bi.events.BackupCreatePasswordNextButtonTapped;
import kin.devplatform.bi.events.BackupCreatePasswordPageViewed;
import kin.devplatform.bi.events.BackupPopupButtonTapped;
import kin.devplatform.bi.events.BackupPopupLaterButtonTapped;
import kin.devplatform.bi.events.BackupPopupPageViewed;
import kin.devplatform.bi.events.BackupQrCodeBackButtonTapped;
import kin.devplatform.bi.events.BackupQrCodeMyqrcodeButtonTapped;
import kin.devplatform.bi.events.BackupQrCodePageViewed;
import kin.devplatform.bi.events.BackupQrCodeSendButtonTapped;
import kin.devplatform.bi.events.BackupStartButtonTapped;
import kin.devplatform.bi.events.BackupWelcomePageBackButtonTapped;
import com.kin.ecosystem.recovery.BackupEvents;
import kin.devplatform.bi.events.BackupWelcomePageViewed;

public class RecoveryBackupEvents implements BackupEvents {

	private final EventLogger eventLogger;

	public RecoveryBackupEvents(@NonNull EventLogger eventLogger) {
		this.eventLogger = eventLogger;
	}

	@Override
	public void onBackupWelcomePageViewed() {
		eventLogger.send(BackupWelcomePageViewed.create());
	}

	@Override
	public void onBackupWelcomePageBackButtonTapped() {
		eventLogger.send(BackupWelcomePageBackButtonTapped.create());
	}

	@Override
	public void onBackupStartButtonTapped() {
		eventLogger.send(BackupStartButtonTapped.create());
	}

	@Override
	public void onBackupCreatePasswordPageViewed() {
		eventLogger.send(BackupCreatePasswordPageViewed.create());
	}

	@Override
	public void onBackupCreatePasswordBackButtonTapped() {
		eventLogger.send(BackupCreatePasswordBackButtonTapped.create());
	}

	@Override
	public void onBackupCreatePasswordNextButtonTapped() {
		eventLogger.send(BackupCreatePasswordNextButtonTapped.create());
	}

	@Override
	public void onBackupQrCodePageViewed() {
		eventLogger.send(BackupQrCodePageViewed.create());
	}

	@Override
	public void onBackupQrCodeBackButtonTapped() {
		eventLogger.send(BackupQrCodeBackButtonTapped.create());
	}

	@Override
	public void onBackupQrCodeSendButtonTapped() {
		eventLogger.send(BackupQrCodeSendButtonTapped.create());
	}

	@Override
	public void onBackupQrCodeMyQrCodeButtonTapped() {
		eventLogger.send(BackupQrCodeMyqrcodeButtonTapped.create());
	}

	@Override
	public void onBackupCompletedPageViewed() {
		eventLogger.send(BackupCompletedPageViewed.create());
	}

	@Override
	public void onBackupPopupPageViewed() {
		eventLogger.send(BackupPopupPageViewed.create());
	}

	@Override
	public void onBackupPopupButtonTapped() {
		eventLogger.send(BackupPopupButtonTapped.create());
	}

	@Override
	public void onBackupPopupLaterButtonTapped() {
		eventLogger.send(BackupPopupLaterButtonTapped.create());
	}
}
