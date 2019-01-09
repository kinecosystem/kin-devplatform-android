package kin.devplatform.network.model;

import android.support.annotation.NonNull;

import kin.devplatform.core.network.ApiException;
import kin.devplatform.network.api.KinVersionApi;
import kin.sdk.migration.bi.IMigrationEventsListener;
import kin.sdk.migration.exception.FailedToResolveSdkVersionException;
import kin.sdk.migration.interfaces.IKinVersionProvider;

public class KinVersionProvider implements IKinVersionProvider {

    private IMigrationEventsListener migrationEventsListener;

    public KinVersionProvider(@NonNull IMigrationEventsListener migrationEventsListener) {
        this.migrationEventsListener = migrationEventsListener;
    }

    @Override
    public SdkVersion getKinSdkVersion(String appId) throws FailedToResolveSdkVersionException {
        KinVersionApi kinVersionApi = new KinVersionApi();
        SdkVersion sdkVersion;
        try {
            migrationEventsListener.onVersionCheckStart();
            String blockchainVersion = kinVersionApi.getBlockchainVersion(appId);
            sdkVersion = SdkVersion.get(blockchainVersion);
        } catch (ApiException e) {
            migrationEventsListener.onVersionCheckFailed(e);
            throw new FailedToResolveSdkVersionException(e);
        }
        migrationEventsListener.onVersionReceived(sdkVersion);
        return sdkVersion;
    }
}
