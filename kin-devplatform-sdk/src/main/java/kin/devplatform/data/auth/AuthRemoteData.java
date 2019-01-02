package kin.devplatform.data.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.List;
import java.util.Map;
import kin.devplatform.core.network.ApiCallback;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.core.util.ExecutorsUtil;
import kin.devplatform.data.Callback;
import kin.devplatform.network.api.AuthApi;
import kin.devplatform.network.model.AuthToken;
import kin.devplatform.network.model.SignInData;
import kin.devplatform.network.model.UserProperties;

public class AuthRemoteData implements AuthDataSource.Remote {

	/**
	 * This is new api client to be different from oder apis without access token interceptor.
	 */
	private static volatile AuthRemoteData instance;

	private final AuthApi authApi;
	private final ExecutorsUtil executorsUtil;

	private SignInData signInData;

	private AuthRemoteData(@NonNull ExecutorsUtil executorsUtil) {
		this.authApi = new AuthApi();
		this.executorsUtil = executorsUtil;
	}


	public static AuthRemoteData getInstance(@NonNull ExecutorsUtil executorsUtil) {
		if (instance == null) {
			synchronized (AuthRemoteData.class) {
				if (instance == null) {
					instance = new AuthRemoteData(executorsUtil);
				}
			}
		}
		return instance;
	}

	@Override
	public void setSignInData(@NonNull SignInData signInData) {
		this.signInData = signInData;
	}

	@Override
	public void getAuthToken(@NonNull final Callback<AuthToken, Exception> callback) {
		try {
			authApi.signInAsync(signInData, "", new ApiCallback<AuthToken>() {
				@Override
				public void onFailure(final ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onFailure(e);
						}
					});
				}

				@Override
				public void onSuccess(final AuthToken result, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onResponse(result);
						}
					});
				}

			});
		} catch (final ApiException e) {
			executorsUtil.mainThread().execute(new Runnable() {
				@Override
				public void run() {
					callback.onFailure(e);
				}
			});
		}
	}

	@Override
	@Nullable
	public AuthToken getAuthTokenSync() {
		try {
			return authApi.signIn(signInData, "");
		} catch (ApiException e) {
			return null;
		}
	}

	@Override
	public void activateAccount(@NonNull final Callback<AuthToken, Exception> callback) {
		try {
			authApi.activateAcountAsync("", new ApiCallback<AuthToken>() {
				@Override
				public void onFailure(final ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onFailure(e);
						}
					});
				}

				@Override
				public void onSuccess(final AuthToken result, int statusCode,
					Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onResponse(result);
						}
					});
				}

			});
		} catch (final ApiException e) {
			executorsUtil.mainThread().execute(new Runnable() {
				@Override
				public void run() {
					callback.onFailure(e);
				}
			});
		}
	}

	@Override
	public void updateWalletAddress(@NonNull UserProperties userProperties,
		@NonNull final Callback<Void, ApiException> callback) {
		try {
			authApi.updateUserAsync(userProperties, new ApiCallback<Void>() {
				@Override
				public void onFailure(final ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onFailure(e);
						}
					});
				}

				@Override
				public void onSuccess(final Void result, int statusCode, Map<String, List<String>> responseHeaders) {
					executorsUtil.mainThread().execute(new Runnable() {
						@Override
						public void run() {
							callback.onResponse(result);
						}
					});
				}

			});
		} catch (final ApiException e) {
			executorsUtil.mainThread().execute(new Runnable() {
				@Override
				public void run() {
					callback.onFailure(e);
				}
			});
		}
	}
}

