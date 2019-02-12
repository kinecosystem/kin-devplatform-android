package kin.devplatform.accountmanager;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import kin.devplatform.KinCallback;
import kin.devplatform.base.Observer;
import kin.devplatform.exception.KinEcosystemException;
import kin.sdk.migration.common.interfaces.IMigrationManagerCallbacks;

public interface AccountManager {

	int REQUIRE_CREATION = 0x00000001;
	int PENDING_CREATION = 0x00000002;
	int REQUIRE_TRUSTLINE = 0x00000003;
	int CREATION_COMPLETED = 0x00000004;
	int ERROR = 0x00000005;

	KinEcosystemException getError();

	@IntDef({REQUIRE_CREATION,
		PENDING_CREATION,
		REQUIRE_TRUSTLINE,
		CREATION_COMPLETED,
		ERROR})
	@Retention(RetentionPolicy.SOURCE)
	@interface AccountState {


	}

	void start();

	void retry();

	void switchAccount(int accountIndex, @NonNull final KinCallback<Boolean> callback,
		@NonNull final IMigrationManagerCallbacks migrationManagerCallbacks);

	@AccountState
	int getAccountState();

	boolean isAccountCreated();

	void addAccountStateObserver(@NonNull final Observer<Integer> observer);

	void removeAccountStateObserver(@NonNull final Observer<Integer> observer);

	interface Local {

		int getAccountState();

		void setAccountState(@AccountState int accountState);
	}
}
