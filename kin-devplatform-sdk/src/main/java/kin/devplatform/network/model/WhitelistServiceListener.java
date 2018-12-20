package kin.devplatform.network.model;

public interface WhitelistServiceListener {
    void onSuccess(String whitelistTransaction);
    void onFailure(Exception e);
}
