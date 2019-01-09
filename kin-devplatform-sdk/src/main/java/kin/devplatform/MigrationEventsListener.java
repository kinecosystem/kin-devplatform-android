package kin.devplatform;

import android.util.Log;

import java.math.BigDecimal;

import kin.sdk.migration.bi.IMigrationEventsListener;
import kin.sdk.migration.interfaces.IKinVersionProvider;

public class MigrationEventsListener implements IMigrationEventsListener {

    private final String TAG = "MigrationEventsListener";

    @Override
    public void onVersionCheckStart() {
        Log.i(TAG, "onVersionCheckStart: ");
    }

    @Override
    public void onVersionReceived(IKinVersionProvider.SdkVersion sdkVersion) {
        Log.i(TAG, "onVersionReceived: " + sdkVersion);
    }

    @Override
    public void onVersionCheckFailed(Exception exception) {
        Log.i(TAG, "onVersionCheckFailed: " + exception.getMessage());
    }

    @Override
    public void onSDKSelected(boolean isNewSDK, String source) {
        Log.i(TAG, "onSDKSelected: isNewSDK=" + isNewSDK + " source=" + source);
    }

    @Override
    public void onAccountBurnStart() {
        Log.i(TAG, "onAccountBurnStart: ");
    }

    @Override
    public void onAccountBurnFailed(Exception exception, BigDecimal balance) {
        Log.e(TAG, "onAccountBurnFailed: " + exception + " balance=" + balance.toPlainString() );
    }

    @Override
    public void onAccountBurnSuccess() {
        Log.i(TAG, "onAccountBurnSuccess: ");
    }

    @Override
    public void onMigrationStart() {
        Log.i(TAG, "onMigrationStart: ");
    }

    @Override
    public void onMigrationFailed(Exception exception) {
        Log.e(TAG, "onMigrationFailed: " + exception.getMessage());
    }

    @Override
    public void onMigrationSuccess(BigDecimal balance) {
        Log.i(TAG, "onMigrationSuccess: balance=" + balance.toPlainString());
    }
}
