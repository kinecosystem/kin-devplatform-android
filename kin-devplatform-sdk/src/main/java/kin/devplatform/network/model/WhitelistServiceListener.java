package kin.sdk.migration.sample;

public interface WhitelistServiceListener {
    void onSuccess(String whitelistTransaction);
    void onFailure(Exception e);
}
