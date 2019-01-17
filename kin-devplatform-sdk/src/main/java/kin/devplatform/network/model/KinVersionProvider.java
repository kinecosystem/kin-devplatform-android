package kin.devplatform.network.model;

import kin.devplatform.core.network.ApiException;
import kin.devplatform.network.api.KinVersionApi;
import kin.sdk.migration.KinSdkVersion;
import kin.sdk.migration.exception.FailedToResolveSdkVersionException;
import kin.sdk.migration.interfaces.IKinVersionProvider;

public class KinVersionProvider implements IKinVersionProvider {

    private final String appId;

    public KinVersionProvider(String appId) {
        this.appId = appId;
    }

    @Override
    public KinSdkVersion getKinSdkVersion() throws FailedToResolveSdkVersionException {
        KinVersionApi kinVersionApi = new KinVersionApi();
        KinSdkVersion sdkVersion;
        try {
            String blockchainVersion = kinVersionApi.getBlockchainVersion(appId);
            sdkVersion = KinSdkVersion.get(blockchainVersion);
        } catch (ApiException e) {
            throw new FailedToResolveSdkVersionException(e);
        }
        return sdkVersion;
    }
}
