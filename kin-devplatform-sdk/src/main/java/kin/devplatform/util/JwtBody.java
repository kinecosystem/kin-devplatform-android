package kin.devplatform.util;

public class JwtBody {

	private String appId;
	private String userId;

	public JwtBody(String appId, String userId) {
		this.appId = appId;
		this.userId = userId;
	}

	public String getAppId() {
		return appId;
	}

	public String getUserId() {
		return userId;
	}

}
