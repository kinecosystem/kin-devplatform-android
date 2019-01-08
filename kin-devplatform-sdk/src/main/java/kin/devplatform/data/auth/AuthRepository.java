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
import kin.devplatform.exception.KinEcosystemException;
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
	private String appId;

	private AuthRepository(@NonNull AuthDataSource.Local local,
		@NonNull AuthDataSource.Remote remote, @NonNull String appId) {
		this.localData = local;
		this.remoteData = remote;
		this.cachedSignInData = local.getSignInData();
		this.cachedAuthToken = local.getAuthTokenSync();
		this.appId = appId;
	}

	public static void init(@NonNull Local localData,
							@NonNull Remote remoteData, @NonNull String appId) {
		if (instance == null) {
			synchronized (AuthRepository.class) {
				if (instance == null) {
					instance = new AuthRepository(localData, remoteData, appId);
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
		setAppId(signInData.getAppId());
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
		loadCachedAppIDIfNeeded();
		return appId;
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
		if (TextUtils.isEmpty(appId)) {
			localData.getAppId(new Callback<String, Void>() {
				@Override
				public void onResponse(String appID) {
					setAppId(appID);
				}

				@Override
				public void onFailure(Void t) {
					// No Data Available
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
		if (appId == null) { // TODO: 31/12/2018 maybe if somehow it is null then we need to throw exception instead?
			setAppId(authToken.getAppID());
		} else if (!appId.equals(authToken.getAppID())) {
			throw ErrorUtil.getClientException(INCORRECT_APP_ID, null);
		}
	}

	@Override
	public void getAuthToken(@Nullable final KinCallback<AuthToken> callback) {
		remoteData.getAuthToken(new Callback<AuthToken, Exception>() {
			@Override
			public void onResponse(AuthToken authToken) {
				try {
					setAuthToken(authToken);
					if (callback != null) {
						callback.onResponse(cachedAuthToken);
					}
				} catch (ClientException e) {
					onFailure(e);
				}

			}

			@Override
			public void onFailure(Exception exception) {
				if (callback != null) {
					if (exception instanceof ApiException) { // TODO: 31/12/2018 can change back to ApiException but feels like app id errors should be conider as ClientException.
						callback.onFailure(ErrorUtil.fromApiException((ApiException) exception));
					} else if (exception instanceof ClientException){
						callback.onFailure((ClientException) exception);
					} else {
						onFailure(ErrorUtil.fromApiException(null));
					}
				}
			}
		});
	}

	private void setAppId(@Nullable String appId) {
		this.appId = appId;
	}

	@Override
	public boolean isActivated() {
		return localData.isActivated();
	}

	@Override
	public void activateAccount(@NonNull final KinCallback<Void> callback) {
		remoteData.activateAccount(new Callback<AuthToken, Exception>() {
			@Override
			public void onResponse(AuthToken response) {
				localData.activateAccount();
				try {
					setAuthToken(response);
					if (callback != null) {
						callback.onResponse(null);
					}
				} catch (ClientException e) {
					onFailure(e);
				}
			}

			@Override
			public void onFailure(Exception e) {
				if (callback != null) {
					if (e instanceof ApiException) { // TODO: 31/12/2018 can change back to ApiException but feels like app id errors should be conider as ClientException.
						callback.onFailure(ErrorUtil.fromApiException((ApiException) e));
					} else if (e instanceof ClientException){
						callback.onFailure((ClientException) e);
					} else {
						onFailure(ErrorUtil.fromApiException(null));
					}
				}
			}
		});
	}
}
