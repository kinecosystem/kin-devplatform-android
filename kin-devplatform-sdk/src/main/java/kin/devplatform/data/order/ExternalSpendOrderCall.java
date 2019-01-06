package kin.devplatform.data.order;

import android.support.annotation.NonNull;
import kin.devplatform.bi.EventLogger;
import kin.devplatform.data.blockchain.BlockchainSource;
import kin.devplatform.data.order.OrderDataSource.Remote;

class ExternalSpendOrderCall extends CreateExternalOrderCall {

	ExternalSpendOrderCall(
		@NonNull Remote remote,
		@NonNull BlockchainSource blockchainSource,
		@NonNull String orderJwt,
		@NonNull EventLogger eventLogger,
		long paymentListenerTimeout,
		@NonNull ExternalSpendOrderCallbacks externalSpendOrderCallbacks
	) {
		super(remote, blockchainSource, orderJwt, eventLogger, externalSpendOrderCallbacks, paymentListenerTimeout);
	}
}
