package kin.devplatform.data.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import java.util.Calendar;
import java.util.Date;
import kin.devplatform.KinCallback;
import kin.devplatform.Log;
import kin.devplatform.Logger;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.core.util.DateUtil;
import kin.devplatform.data.Callback;
import kin.devplatform.exception.ClientException;
import kin.devplatform.network.model.AuthToken;
import kin.devplatform.network.model.SignInData;
import kin.devplatform.network.model.UserProperties;
import kin.devplatform.util.ErrorUtil;

import static kin.devplatform.Log.ERROR;
import static kin.devplatform.exception.ClientException.INCORRECT_APP_ID;

public class AuthRepository implements AuthDataSource {

	private final static String TAG = AuthRepository.class.getSimpleName();
	private static AuthRepository instance = null;

	private final AuthDataSource.Local localData;
	private final AuthDataSource.Remote remoteData;

	private SignInData cachedSignInData;
	private AuthToken cachedAuthToken;

	private AuthRepository(@NonNull AuthDataSource.Local local,
		@NonNull AuthDataSource.Remote remote) {
		this.localData = local;
		this.remoteData = remote;
		this.cachedSignInData = local.getSignInData();
		this.cachedAuthToken = local.getAuthTokenSync();
	}

	public static void init(@NonNull Local localData,
							@NonNull Remote remoteData) {
		if (instance == null) {
			synchronized (AuthRepository.class) {
				if (instance == null) {
					instance = new AuthRepository(localData, remoteData);
				}
			}
		}
	}

	public static AuthRepository getInstance() {
		return instance;
	}

	@Override
	public void setSignInData(@NonNull SignInData signInData) {
		cachedSignInData = signInData;
		localData.setSignInData(signInData);
		remoteData.setSignInData(signInData);
	}

	@Override
	public void updateWalletAddress(final String address, @NonNull final KinCallback<Boolean> callback) {
		final UserProperties userProperties = new UserProperties().walletAddress(address);
		remoteData.updateWalletAddress(userProperties, new Callback<Void, ApiException>() {
			@Override
			public void onResponse(Void response) {
				cachedSignInData.setWalletAddress(address);
				setSignInData(cachedSignInData);
				callback.onResponse(true);
			}

			@Override
			public void onFailure(ApiException exception) {
				callback.onFailure(ErrorUtil.fromApiException(exception));
			}
		});
	}

	@Override
	public String getAppID() {
		loadCachedAppIDIfNeeded(); // TODO: 13/01/2019 do we still need this method?
		return cachedSignInData != null ? cachedSignInData.getAppId() : null;
	}

	@Override
	public String getDeviceID() {
		return localData.getDeviceID();
	}

	@Override
	public String getUserID() {
		return localData.getUserID();
	}

	@Override
	public String getEcosystemUserID() {
		return localData.getEcosystemUserID();
	}

	private void loadCachedAppIDIfNeeded() {
		if (cachedSignInData != null && TextUtils.isEmpty(cachedSignInData.getAppId())) {
			localData.getAppId(new Callback<String, Void>() {
				@Override
				public void onResponse(String appID) {
					int i = 0;
					i++; // TODO: 14/01/2019 delete after test
				}

				@Override
				public void onFailure(Void t) {
					// No Data Available
					int i = 0;
					i++; // TODO: 14/01/2019 delete after test
				}
			});
		}
	}

	@Override
	@Nullable
	public AuthToken getCachedAuthToken() {
		return cachedAuthToken;
	}

	@Override
	public AuthToken getAuthTokenSync() {
		if (cachedAuthToken != null) {
			return cachedAuthToken;
		} else {
			if (cachedSignInData != null) {
				try {
					AuthToken authToken = localData.getAuthTokenSync();
					if (authToken != null && !isAuthTokenExpired(authToken)) {
						setAuthToken(authToken);
					} else {
						refreshTokenSync();
					}
				} catch (ClientException e) {
					Logger.log(new Log().priority(ERROR).withTag(TAG).text("incorrect app id"));
					return null;
				}
				return cachedAuthToken;
			} else {
				return null;
			}
		}
	}


	private boolean isAuthTokenExpired(AuthToken authToken) {
		if (authToken == null) {
			return true;
		} else {
			Date expirationDate = DateUtil.getDateFromUTCString(authToken.getExpirationDate());
			if (expirationDate != null) {
				return Calendar.getInstance().getTimeInMillis() > expirationDate.getTime();
			} else {
				return true;
			}
		}
	}

	private void refreshTokenSync() {
		AuthToken authToken = remoteData.getAuthTokenSync();
		if (authToken != null) {
			try {
				setAuthToken(authToken);
			} catch (ClientException e) {
				Logger.log(new Log().priority(ERROR).withTag(TAG).text("incorrect app id"));
			}
		}
	}

	@Override
	public void setAuthToken(@NonNull AuthToken authToken) throws ClientException {
		cachedAuthToken = authToken;
		localData.setAuthToken(authToken);
		if (!cachedSignInData.getAppId().equals(authToken.getAppID())) {
			throw ErrorUtil.getClientException(INCORRECT_APP_ID, null);
		}
	}

	@Override
	public void getAuthToken(@Nullable final KinCallback<AuthToken> callback) {
		remoteData.getAuthToken(new Callback<AuthToken, ApiException>() {
			@Override
			public void onResponse(AuthToken authToken) {
				try {
					setAuthToken(authToken);
					if (callback != null) {
						callback.onResponse(cachedAuthToken);
					}
				} catch (ClientException e) {
					onFailure(new ApiException(INCORRECT_APP_ID, e));
				}

			}

			@Override
			public void onFailure(ApiException exception) {
				if (callback != null) {
					callback.onFailure(ErrorUtil.fromApiException(exception));
				}
			}
		});
	}

	@Override
	public boolean isActivated() {
		return localData.isActivated();
	}

	@Override
	public void activateAccount(@NonNull final KinCallback<Void> callback) {
		remoteData.activateAccount(new Callback<AuthToken, ApiException>() {
			@Override
			public void onResponse(AuthToken response) {
				localData.activateAccount();
				try {
					setAuthToken(response);
					if (callback != null) {
						callback.onResponse(null);
					}
				} catch (ClientException e) {
					onFailure(new ApiException(INCORRECT_APP_ID, e));
				}
			}

			@Override
			public void onFailure(ApiException e) {
				if (callback != null) {
					callback.onFailure(ErrorUtil.fromApiException(e));
				}
			}
		});
	}
}
