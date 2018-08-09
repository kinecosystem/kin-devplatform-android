package kin.devplatform.data;

import kin.devplatform.KinCallback;
import kin.devplatform.exception.KinEcosystemException;

public abstract class KinCallbackAdapter<T> implements KinCallback<T> {

	@Override
	public void onResponse(T response) {

	}

	@Override
	public void onFailure(KinEcosystemException exception) {

	}
}
