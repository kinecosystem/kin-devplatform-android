package kin.devplatform.network.model;

import com.google.gson.annotations.SerializedName;

public class WhitelistTransactionRequest {

    @SerializedName("tx_envelope")
    private String txEnvelope;

    @SerializedName("network_id")
    private String networkId;

    public WhitelistTransactionRequest(String txEnvelope, String networkId) {
        this.txEnvelope = txEnvelope;
        this.networkId = networkId;
    }
}
