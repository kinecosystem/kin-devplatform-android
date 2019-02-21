package kin.devplatform.network.model;

import com.google.gson.annotations.SerializedName;

public class RestorableWalletRequest {

	@SerializedName("restorable")
	private boolean restorable;

	public boolean isRestorable() {
		return restorable;
	}
}
