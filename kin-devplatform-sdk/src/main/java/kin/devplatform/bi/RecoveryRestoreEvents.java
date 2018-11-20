package kin.devplatform.bi;

import android.support.annotation.NonNull;
import kin.devplatform.bi.events.RestoreAreYouSureCancelButtonTapped;
import kin.devplatform.bi.events.RestoreAreYouSureOkButtonTapped;
import kin.devplatform.bi.events.RestorePasswordDoneButtonTapped;
import kin.devplatform.bi.events.RestorePasswordEntryBackButtonTapped;
import kin.devplatform.bi.events.RestorePasswordEntryPageViewed;
import kin.devplatform.bi.events.RestoreUploadQrCodeBackButtonTapped;
import kin.devplatform.bi.events.RestoreUploadQrCodeButtonTapped;
import com.kin.ecosystem.recovery.RestoreEvents;
import kin.devplatform.bi.events.RestoreUploadQrCodePageViewed;

public class RecoveryRestoreEvents implements RestoreEvents {

	private final EventLogger eventLogger;

	public RecoveryRestoreEvents(@NonNull EventLogger eventLogger) {
		this.eventLogger = eventLogger;
	}

	@Override
	public void onRestoreUploadQrCodePageViewed() {
		eventLogger.send(RestoreUploadQrCodePageViewed.create());
	}

	@Override
	public void onRestoreUploadQrCodeBackButtonTapped() {
		eventLogger.send(RestoreUploadQrCodeBackButtonTapped.create());
	}

	@Override
	public void onRestoreUploadQrCodeButtonTapped() {
		eventLogger.send(RestoreUploadQrCodeButtonTapped.create());
	}

	@Override
	public void onRestoreAreYouSureOkButtonTapped() {
		eventLogger.send(RestoreAreYouSureOkButtonTapped.create());
	}

	@Override
	public void onRestoreAreYouSureCancelButtonTapped() {
		eventLogger.send(RestoreAreYouSureCancelButtonTapped.create());
	}

	@Override
	public void onRestorePasswordEntryPageViewed() {
		eventLogger.send(RestorePasswordEntryPageViewed.create());
	}

	@Override
	public void onRestorePasswordEntryBackButtonTapped() {
		eventLogger.send(RestorePasswordEntryBackButtonTapped.create());
	}

	@Override
	public void onRestorePasswordDoneButtonTapped() {
		eventLogger.send(RestorePasswordDoneButtonTapped.create());
	}
}
