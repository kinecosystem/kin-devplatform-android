package kin.devplatform;

import android.util.Log;

import java.math.BigDecimal;

import kin.devplatform.bi.EventLogger;
import kin.devplatform.bi.events.MigrationBurnFailed;
import kin.devplatform.bi.events.MigrationBurnStart;
import kin.devplatform.bi.events.MigrationBurnSuccess;
import kin.devplatform.bi.events.MigrationMigrationFailed;
import kin.devplatform.bi.events.MigrationMigrationStart;
import kin.devplatform.bi.events.MigrationMigrationSuccess;
import kin.devplatform.bi.events.MigrationSelectedSDK;
import kin.devplatform.bi.events.MigrationVersionCheckFailed;
import kin.devplatform.bi.events.MigrationVersionCheckReceived;
import kin.devplatform.bi.events.MigrationVersionCheckStart;
import kin.sdk.migration.bi.IMigrationEventsListener;
import kin.sdk.migration.interfaces.IKinVersionProvider;

public class MigrationEventsListener implements IMigrationEventsListener {

    private final String TAG = "MigrationEventsListener";
    final EventLogger eventLogger;

    MigrationEventsListener(final EventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    @Override
    public void onVersionCheckStart() {
        Log.i(TAG, "onVersionCheckStart: ");
        eventLogger.send(MigrationVersionCheckStart.create());
    }

    @Override
    public void onVersionReceived(IKinVersionProvider.SdkVersion sdkVersion) {
        Log.i(TAG, "onVersionReceived: " + sdkVersion);
        eventLogger.send(MigrationVersionCheckReceived.create(sdkVersion));
    }

    @Override
    public void onVersionCheckFailed(Exception exception) {
        Log.i(TAG, "onVersionCheckFailed: " + exception.getMessage());
        eventLogger.send(MigrationVersionCheckFailed.create(exception));
    }

    @Override
    public void onSDKSelected(boolean isNewSDK, String source) {
        Log.i(TAG, "onSDKSelected: isNewSDK=" + isNewSDK + " source=" + source);
        eventLogger.send(MigrationSelectedSDK.create(isNewSDK, source));
    }

    @Override
    public void onAccountBurnStart() {
        Log.i(TAG, "onAccountBurnStart: ");
        eventLogger.send(MigrationBurnStart.create());
    }

    @Override
    public void onAccountBurnFailed(Exception exception, BigDecimal balance) {
        Log.e(TAG, "onAccountBurnFailed: " + exception + " balance=" + balance.toPlainString() );
        eventLogger.send(MigrationBurnFailed.create(exception, balance));
    }

    @Override
    public void onAccountBurnSuccess() {
        Log.i(TAG, "onAccountBurnSuccess: ");
        eventLogger.send(MigrationBurnSuccess.create());
    }

    @Override
    public void onMigrationStart() {
        Log.i(TAG, "onMigrationStart: ");
        eventLogger.send(MigrationMigrationStart.create());
    }

    @Override
    public void onMigrationFailed(Exception exception) {
        Log.e(TAG, "onMigrationFailed: " + exception.getMessage());
        eventLogger.send(MigrationMigrationFailed.create(exception));
    }

    @Override
    public void onMigrationSuccess(BigDecimal balance) {
        Log.i(TAG, "onMigrationSuccess: balance=" + balance.toPlainString());
        eventLogger.send(MigrationMigrationSuccess.create());
    }
}
