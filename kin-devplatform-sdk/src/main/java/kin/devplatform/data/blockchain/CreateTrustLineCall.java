package kin.devplatform.data.blockchain;

import kin.sdk.migration.IKinAccount;
import kin.sdk.migration.exception.OperationFailedException;

class CreateTrustLineCall extends Thread {

	private final IKinAccount account;
	private final TrustlineCallback trustlineCallback;

	private static final int MAX_TRIES = 10;

	CreateTrustLineCall(IKinAccount account, TrustlineCallback trustlineCallback) {
		this.account = account;
		this.trustlineCallback = trustlineCallback;
	}

	@Override
	public void run() {
		super.run();
		createTrustline(0);
	}

	private void createTrustline(int tries) {
		try {
			account.activateSync();
			trustlineCallback.onSuccess();
		} catch (OperationFailedException e) {
			if (tries < MAX_TRIES) {
				createTrustline(++tries);
			} else {
				trustlineCallback.onFailure(e);
			}
		}
	}

	interface TrustlineCallback {

		void onSuccess();

		void onFailure(OperationFailedException e);
	}
}
