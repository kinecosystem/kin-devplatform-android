package kin.devplatform.data.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import kin.devplatform.KinCallback;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.data.Callback;
import kin.devplatform.exception.ClientException;
import kin.devplatform.network.model.AuthToken;
import kin.devplatform.network.model.SignInData;
import kin.devplatform.network.model.UserProperties;

public interface AuthDataSource {

	void setSignInData(@NonNull final SignInData signInData);

	void updateWalletAddress(String address, @NonNull KinCallback<Boolean> callback);

	String getAppID();

	String getDeviceID();

	String getUserID();

	String getEcosystemUserID();

	void setAuthToken(@NonNull final AuthToken authToken) throws ClientException;

	void getAuthToken(@Nullable final KinCallback<AuthToken> callback);

	@Nullable
	AuthToken getCachedAuthToken();

	AuthToken getAuthTokenSync();

	boolean isActivated();

	void activateAccount(@NonNull final KinCallback<Void> callback);

	interface Local {

		void setSignInData(@NonNull final SignInData signInData);

		SignInData getSignInData();

		void setAuthToken(@NonNull final AuthToken authToken);

		String getAppId();

		String getDeviceID();

		String getUserID();

		String getEcosystemUserID();

		AuthToken getAuthTokenSync();

		boolean isActivated();

		void activateAccount();

	}

	interface Remote {

		void getAuthToken(@NonNull final Callback<AuthToken, ApiException> callback);

		void setSignInData(@NonNull final SignInData signInData);

		AuthToken getAuthTokenSync();

		void activateAccount(@NonNull final Callback<AuthToken, ApiException> callback);

		void updateWalletAddress(@NonNull UserProperties userProperties, @NonNull final Callback<Void, ApiException> callback);
	}
}
