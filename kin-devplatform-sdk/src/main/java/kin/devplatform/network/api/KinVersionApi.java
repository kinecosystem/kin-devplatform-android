package kin.devplatform.network.api;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import kin.devplatform.ConfigurationImpl;
import kin.devplatform.core.network.ApiClient;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.core.network.ApiResponse;
import kin.devplatform.network.model.Order;
import okhttp3.Call;

import static kin.devplatform.core.network.ApiClient.GET;

public class KinVersionApi {

    private ApiClient apiClient;

    public KinVersionApi() {
        this(ConfigurationImpl.getInstance().getDefaultApiClient());
    }

    public KinVersionApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public String getBlockchainVersion(String appId) throws ApiException {
        validateKinVersionApiParams(appId);
        Call call = getKinVersionApiCall(appId);
        Type localVarReturnType = new TypeToken<String>() {
        }.getType();
        ApiResponse<String> response = apiClient.execute(call, localVarReturnType);
        return response.getData();
    }

    private void validateKinVersionApiParams(String appId) throws ApiException {
        // verify 'appId'
        if (appId == null) {
            throw new ApiException("Missing the required parameter 'appId' when calling getBlockchainVersion(...)");
        }
    }

    private Call getKinVersionApiCall(String appId) throws ApiException {
        String localVarPath = "/config/blockchain/" + appId;
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();
        return apiClient.buildCall(localVarPath, GET, null, null, null,
                localVarHeaderParams, localVarFormParams, null);

    }


}
