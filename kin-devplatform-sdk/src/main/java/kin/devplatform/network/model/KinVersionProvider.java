package kin.devplatform.network.model;

import kin.devplatform.core.network.ApiException;
import kin.devplatform.network.api.KinVersionApi;
import kin.sdk.migration.exception.FailedToResolveSdkVersionException;
import kin.sdk.migration.interfaces.IKinVersionProvider;

public class KinVersionProvider implements IKinVersionProvider {

    @Override
    public SdkVersion getKinSdkVersion(String appId) throws FailedToResolveSdkVersionException {
        KinVersionApi kinVersionApi = new KinVersionApi();
        SdkVersion sdkVersion;
        try {
            String blockchainVersion = kinVersionApi.getBlockchainVersion(appId);
            sdkVersion = SdkVersion.get(blockchainVersion);
        } catch (ApiException e) {
            throw new FailedToResolveSdkVersionException(e);
        }
        return sdkVersion;
    }
}
