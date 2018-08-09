package kin.devplatform.data.blockchain;

import android.support.annotation.NonNull;
import java.math.BigDecimal;
import kin.devplatform.KinCallback;
import kin.devplatform.base.Observer;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.model.Payment;

public interface BlockchainSource {

	/**
	 * @param appID - appID - will be included in the memo for each transaction.
	 */
	void setAppID(String appID);

	/**
	 * Send transaction to the network
	 *
	 * @param publicAddress the recipient address
	 * @param amount the amount to send
	 * @param orderID the orderID to be included in the memo of the transaction
	 */
	void sendTransaction(@NonNull String publicAddress, @NonNull BigDecimal amount, @NonNull String orderID,
		@NonNull String offerID);

	/**
	 * @return the cached balance.
	 */
	Balance getBalance();

	/**
	 * Get balance from network
	 */
	void getBalance(@NonNull final KinCallback<Balance> callback);

	/**
	 * Add balance observer in order to start receive balance updates
	 */
	void addBalanceObserver(@NonNull final Observer<Balance> observer);

	/**
	 * Add balance observer that will keep a connection on account balance updates from the blockchain network.
	 */
	void addBalanceObserverAndStartListen(@NonNull final Observer<Balance> observer);

	/**
	 * Remove the balance observer in order to stop receiving balance updates.
	 */
	void removeBalanceObserver(@NonNull final Observer<Balance> observer);

	/**
	 * Remove the balance observer, and close the connection if no other observers.
	 */
	void removeBalanceObserverAndStopListen(@NonNull final Observer<Balance> observer);

	/**
	 * @return the public address of the initiated account
	 */
	String getPublicAddress();

	/**
	 * Add {@link Payment} completed observer.
	 */
	void addPaymentObservable(Observer<Payment> observer);

	/**
	 * Remove the payment observer to stop listening for completed payments.
	 */
	void removePaymentObserver(Observer<Payment> observer);

	interface Local {

		int getBalance();

		void setBalance(int balance);

		boolean hasTrustLine();

		void setHasTrustline(boolean hasTrustline);

	}
}
