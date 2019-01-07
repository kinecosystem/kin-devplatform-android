package kin.devplatform.network.model;

import kin.devplatform.core.network.ApiException;
import kin.devplatform.network.api.WhitelistApi;
import kin.sdk.migration.exception.WhitelistTransactionFailedException;
import kin.sdk.migration.interfaces.IWhitelistService;
import kin.sdk.migration.interfaces.IWhitelistableTransaction;
import kin.sdk.migration.sdk_related.WhitelistResult;

public class WhitelistService implements IWhitelistService {

    private final String orderId;
    private WhitelistServiceListener whitelistServiceListener;

    public WhitelistService(String orderId) {
        this(orderId, null);
    }

    public WhitelistService(String orderId, WhitelistServiceListener whitelistServiceListener) {
        this.orderId = orderId;
    }

    @Override
    public WhitelistResult onWhitelistableTransactionReady(IWhitelistableTransaction whitelistableTransaction) throws WhitelistTransactionFailedException {
        WhitelistApi whitelistApi = new WhitelistApi();
        try {
            String whitelistedTransaction = whitelistApi.whitelistTransaction(orderId, whitelistableTransaction);
            return new WhitelistResult(whitelistedTransaction, true);
        } catch (ApiException e) {
            throw new WhitelistTransactionFailedException(e);
        }
    }
}




