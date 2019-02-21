package kin.devplatform.network.api;

import static kin.devplatform.core.network.ApiClient.GET;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import kin.devplatform.ConfigurationImpl;
import kin.devplatform.core.network.ApiCallback;
import kin.devplatform.core.network.ApiClient;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.network.model.RestorableWalletRequest;
import okhttp3.Call;

public class RestorableWalletApi {

	private final ApiClient apiClient;

	public RestorableWalletApi() {
		this(ConfigurationImpl.getInstance().getDefaultApiClient());
	}

	public RestorableWalletApi(ApiClient apiClient) {
		this.apiClient = apiClient;
	}

	public void getIsRestorableWallet(String publicAddress, final ApiCallback<RestorableWalletRequest> apiCallback)
		throws ApiException {
		validateKinVersionApiParams(publicAddress);
		Call call = getRestorableApiCall(publicAddress);
		Type localVarReturnType = new TypeToken<RestorableWalletRequest>() {
		}.getType();
		apiClient.executeAsync(call, localVarReturnType, apiCallback);
	}

	private void validateKinVersionApiParams(String publicAddress) throws ApiException {
		// verify 'publicAddress'
		if (publicAddress == null || publicAddress.length() == 0) {
			throw new ApiException(
				"Missing the required parameter 'publicAddress' when calling getIsRestorableWallet(...)");
		}
	}

	private Call getRestorableApiCall(String publicAddress) throws ApiException {
		String localVarPath = "/users/me/restorable/" + publicAddress;
		Map<String, String> localVarHeaderParams = new HashMap<String, String>();
		Map<String, Object> localVarFormParams = new HashMap<String, Object>();
		return apiClient.buildCall(localVarPath, GET, null, null, null,
			localVarHeaderParams, localVarFormParams, null);

	}

}
