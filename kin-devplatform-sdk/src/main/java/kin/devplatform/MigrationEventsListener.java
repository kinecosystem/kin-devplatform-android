package kin.devplatform;

import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.MigrationBurnFailed;
import kin.devplatform.bi.events.MigrationBurnStarted;
import kin.devplatform.bi.events.MigrationBurnSucceeded;
import kin.devplatform.bi.events.MigrationCallbackFailed;
import kin.devplatform.bi.events.MigrationCallbackReady;
import kin.devplatform.bi.events.MigrationCallbackStart;
import kin.devplatform.bi.events.MigrationCheckBurnFailed;
import kin.devplatform.bi.events.MigrationCheckBurnStarted;
import kin.devplatform.bi.events.MigrationCheckBurnSucceeded;
import kin.devplatform.bi.events.MigrationMethodStarted;
import kin.devplatform.bi.events.MigrationRequestAccountMigrationFailed;
import kin.devplatform.bi.events.MigrationRequestAccountMigrationStarted;
import kin.devplatform.bi.events.MigrationRequestAccountMigrationSucceeded;
import kin.devplatform.bi.events.MigrationRequestAccountMigrationSucceeded.MigrationReason;
import kin.devplatform.bi.events.MigrationVersionCheckFailed;
import kin.devplatform.bi.events.MigrationVersionCheckStarted;
import kin.devplatform.bi.events.MigrationVersionCheckSucceeded;
import kin.devplatform.bi.events.MigrationVersionCheckSucceeded.SdkVersion;
import kin.devplatform.util.ErrorUtil;
import kin.sdk.migration.bi.IMigrationEventsListener;
import kin.sdk.migration.common.KinSdkVersion;

public class MigrationEventsListener implements IMigrationEventsListener {

	private final EventLogger eventLogger;

	public MigrationEventsListener(EventLogger eventLogger) {
		this.eventLogger = eventLogger;
	}

	@Override
	public void onMethodStarted() {
		eventLogger.send(MigrationMethodStarted.create());
	}

	@Override
	public void onVersionCheckStarted() {
		eventLogger.send(MigrationVersionCheckStarted.create());
	}

	@Override
	public void onVersionCheckSucceeded(KinSdkVersion sdkVersion) {
		eventLogger.send(MigrationVersionCheckSucceeded.create(SdkVersion.fromValue(sdkVersion.getVersion())));
	}

	@Override
	public void onVersionCheckFailed(Exception exception) {
		eventLogger.send(MigrationVersionCheckFailed
			.create(ErrorUtil.getPrintableStackTrace(exception), "", exception.getMessage()));
	}

	@Override
	public void onCallbackStart() {
		eventLogger.send(MigrationCallbackStart.create());
	}

	@Override
	public void onCheckBurnStarted(String publicAddress) {
		eventLogger.send(MigrationCheckBurnStarted.create(publicAddress));
	}

	@Override
	public void onCheckBurnSucceeded(String publicAddress, CheckBurnReason reason) {
		eventLogger.send(MigrationCheckBurnSucceeded
			.create(MigrationCheckBurnSucceeded.CheckBurnReason.fromValue(reason.value()), publicAddress));
	}

	@Override
	public void onCheckBurnFailed(String publicAddress, Exception exception) {
		eventLogger.send(MigrationCheckBurnFailed
			.create(publicAddress, ErrorUtil.getPrintableStackTrace(exception), "", exception.getMessage()));
	}

	@Override
	public void onBurnStarted(String publicAddress) {
		eventLogger.send(MigrationBurnStarted.create(publicAddress));
	}

	@Override
	public void onBurnSucceeded(String publicAddress, BurnReason reason) {
		eventLogger.send(
			MigrationBurnSucceeded.create(MigrationBurnSucceeded.BurnReason.fromValue(reason.value()), publicAddress));
	}

	@Override
	public void onBurnFailed(String publicAddress, Exception exception) {
		eventLogger.send(MigrationBurnFailed
			.create(publicAddress, ErrorUtil.getPrintableStackTrace(exception), "", exception.getMessage()));
	}

	@Override
	public void onRequestAccountMigrationStarted(String publicAddress) {
		eventLogger.send(MigrationRequestAccountMigrationStarted.create(publicAddress));
	}

	@Override
	public void onRequestAccountMigrationSucceeded(String publicAddress, RequestAccountMigrationSuccessReason reason) {
		eventLogger.send(
			MigrationRequestAccountMigrationSucceeded.create(MigrationReason.fromValue(reason.value()), publicAddress));
	}

	@Override
	public void onRequestAccountMigrationFailed(String publicAddress, Exception exception) {
		eventLogger.send(MigrationRequestAccountMigrationFailed
			.create(publicAddress, ErrorUtil.getPrintableStackTrace(exception), "", exception.getMessage()));
	}

	@Override
	public void onCallbackReady(KinSdkVersion sdkVersion, SelectedSdkReason selectedSdkReason) {
		eventLogger.send(MigrationCallbackReady
			.create(MigrationCallbackReady.SelectedSdkReason.fromValue(selectedSdkReason.value()),
				MigrationCallbackReady.SdkVersion
					.fromValue(sdkVersion.getVersion())));
	}

	@Override
	public void onCallbackFailed(Exception exception) {
		eventLogger.send(
			MigrationCallbackFailed.create(ErrorUtil.getPrintableStackTrace(exception), "", exception.getMessage()));
	}
}
