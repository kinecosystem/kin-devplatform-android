package kin.devplatform.network.api;

import static kin.devplatform.core.network.ApiClient.POST;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import kin.devplatform.ConfigurationImpl;
import kin.devplatform.core.network.ApiClient;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.core.network.ApiResponse;
import kin.devplatform.network.model.WhitelistTransactionRequest;
import kin.sdk.migration.common.interfaces.IWhitelistableTransaction;
import okhttp3.Call;

public class WhitelistApi {

    private ApiClient apiClient;

    public WhitelistApi() {
        this(ConfigurationImpl.getInstance().getDefaultApiClient());
    }

    public WhitelistApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public String whitelistTransaction(String orderId, IWhitelistableTransaction whitelistableTransaction) throws ApiException {
        validateWhitelistTransactionParams(orderId, whitelistableTransaction);
        Call call = getWhitelistTransactionCall(orderId, whitelistableTransaction);
        Type localVarReturnType = new TypeToken<String>() { }.getType();
        ApiResponse<String> response = apiClient.execute(call, localVarReturnType);
        return response.getData();
    }

    private void validateWhitelistTransactionParams(String orderId, IWhitelistableTransaction whitelistableTransaction) throws ApiException {
        // verify 'orderId'
        if (orderId == null) {
            throw new ApiException("Missing the required parameter 'orderId' when calling whitelistTransaction(...)");
        }

        // verify 'whitelistableTransaction'
        if (whitelistableTransaction == null) {
            throw new ApiException("Missing the required parameter 'whitelistableTransaction' when calling whitelistTransaction(...)");
        }
    }

    private Call getWhitelistTransactionCall(String orderId, IWhitelistableTransaction whitelistableTransaction) throws ApiException {
        String localVarPath = "/orders/" + orderId + "/whitelist";
        WhitelistTransactionRequest body;
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();
        body = new WhitelistTransactionRequest(whitelistableTransaction.getTransactionPayload(), whitelistableTransaction.getNetworkPassphrase());
        return apiClient.buildCall(localVarPath, POST, null, null, body,
                localVarHeaderParams, localVarFormParams, null);
    }
}