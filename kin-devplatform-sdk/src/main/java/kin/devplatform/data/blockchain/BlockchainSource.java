package kin.devplatform.data.blockchain;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.kin.ecosystem.recovery.KeyStoreProvider;
import java.math.BigDecimal;
import kin.devplatform.KinCallback;
import kin.devplatform.base.Observer;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.model.Payment;
import kin.devplatform.exception.BlockchainException;
import kin.devplatform.network.model.OpenOrder;
import kin.sdk.migration.common.KinSdkVersion;
import kin.sdk.migration.common.exception.DeleteAccountException;
import kin.sdk.migration.common.exception.OperationFailedException;
import kin.sdk.migration.common.interfaces.IKinAccount;
import kin.sdk.migration.common.interfaces.IKinClient;

public interface BlockchainSource {

	/**
	 * Getting the current account.
	 */
	@Nullable
	IKinAccount getKinAccount();

	/**
	 * @param appID - appID - will be included in the memo for each transaction.
	 */
	void setAppID(String appID);

	/**
	 * @return the app id
	 */
	String getAppId();

	/**
	 * Send transaction to the network
	 *
	 * @param publicAddress the recipient address
	 * @param amount the amount to send
	 */
	@NonNull
	void sendTransaction(@NonNull String publicAddress, @NonNull BigDecimal amount,
		@NonNull OpenOrder order) throws OperationFailedException;

	/**
	 * @return the cached balance.
	 */
	Balance getBalance();

	/**
	 * Get balance from network
	 */
	void getBalance(@Nullable final KinCallback<Balance> callback);

	/**
	 * @return true if the account is on the new kin blockchain, meaning we are using the new kin sdk.
	 */
	KinSdkVersion getKinSdkVersion();

	/**
	 * Reconnect the balance connection, due to connection lose.
	 */
	void reconnectBalanceConnection();

	/**
	 * Add balance observer in order to start receive balance updates
	 *
	 * @param startSSE true will keep a connection on account balance updates from the blockchain network
	 */
	void addBalanceObserver(@NonNull final Observer<Balance> observer, boolean startSSE);

	/**
	 * Remove the balance observer in order to stop receiving balance updates.
	 * @param stopSSE true will close the connection if no other observers
	 */
	void removeBalanceObserver(@NonNull final Observer<Balance> observer, boolean stopSSE);

	/**
	 * @return the public address of the initiated account
	 */
	String getPublicAddress();

	/**
	 * @return the public address of the account with {@param accountIndex}
	 */
	String getPublicAddress(final int accountIndex);

	/**
	 * Add {@link Payment} completed observer.
	 */
	void addPaymentObservable(Observer<Payment> observer);

	/**
	 * Remove the payment observer to stop listening for completed payments.
	 */
	void removePaymentObserver(Observer<Payment> observer);

	/**
	 * Create trustline polling call, so it will try few time before failure.
	 */
	void createTrustLine(@NonNull final KinCallback<Void> callback);

	/**
	 * Creates the {@link KeyStoreProvider} to use in backup and restore flow.
	 *
	 * @return {@link KeyStoreProvider}
	 */
	KeyStoreProvider getKeyStoreProvider();

	void updateActiveAccount(IKinClient kinClient, int accountIndex) throws BlockchainException;

	void deleteAccount(int accountIndex) throws DeleteAccountException;

	interface Local {

		int getBalance();

		void setBalance(int balance);

		int getAccountIndex();

		void setAccountIndex(int index);

		String getAppId();

		void setAppId(String appId);
	}
}
