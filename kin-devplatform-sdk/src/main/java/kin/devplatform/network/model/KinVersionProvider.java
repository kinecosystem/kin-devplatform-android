package kin.devplatform.network.model;

import kin.devplatform.core.network.ApiException;
import kin.devplatform.network.api.KinVersionApi;
import kin.sdk.migration.exception.FailedToResolveSdkVersionException;
import kin.sdk.migration.interfaces.IKinVersionProvider;

public class KinVersionProvider implements IKinVersionProvider {

    private static final String NEW_KIN_VERSION = "3";

    @Override
    public boolean isNewKinSdkVersion(String appId) throws FailedToResolveSdkVersionException {
        KinVersionApi kinVersionApi = new KinVersionApi();
        boolean isNewKinSdkVersion;
        try {
            String blockchainVersion = kinVersionApi.getBlockchainVersion(appId);
            isNewKinSdkVersion = blockchainVersion.equals(NEW_KIN_VERSION);
        } catch (ApiException e) {
            throw new FailedToResolveSdkVersionException(e);
        }
        return isNewKinSdkVersion;
    }
}
