package kin.devplatform.network.model;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import kin.sdk.migration.exception.WhitelistTransactionFailedException;
import kin.sdk.migration.interfaces.IWhitelistService;
import kin.sdk.migration.interfaces.IWhitelistServiceCallbacks;
import kin.sdk.migration.interfaces.IWhitelistableTransaction;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WhitelistService implements IWhitelistService {

    private static final String URL_WHITELISTING_SERVICE = "http://18.206.35.110:3000/whitelist"; // TODO: 18/12/2018 need the correct url
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final Handler handler;
    private WhitelistServiceListener whitelistServiceListener;

    public WhitelistService() {
        this(null);
    }

    public WhitelistService(WhitelistServiceListener whitelistServiceListener) {
        this.whitelistServiceListener = whitelistServiceListener;
        handler = new Handler(Looper.getMainLooper());
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String whitelistTransaction(IWhitelistableTransaction whitelistableTransaction) throws WhitelistTransactionFailedException {
        String whitelistTransaction = null;
        RequestBody requestBody = null;
        try {
            requestBody = RequestBody.create(JSON, toJson(whitelistableTransaction));
        } catch (JSONException e) {
            throw new WhitelistTransactionFailedException(e);
        }
        Request request = new Request.Builder()
                .url(URL_WHITELISTING_SERVICE)
                .post(requestBody)
                .build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            if (response != null) {
                ResponseBody body = response.body();
                if (body != null) {
                    whitelistTransaction = body.string();
                }
            }
        } catch (IOException e) {
            throw new WhitelistTransactionFailedException(e);
        }
        return whitelistTransaction;
    }

    public void setWhitelistServiceListener(WhitelistServiceListener whitelistServiceListener) {
        this.whitelistServiceListener = whitelistServiceListener;
    }

    private void handleResponse(@NonNull Response response, IWhitelistServiceCallbacks callbacks) throws IOException {
        if (response.body() != null) {
            fireOnSuccess(whitelistServiceListener, response.body().string());
            if (callbacks != null) {
                callbacks.onSuccess(response.body().string());
            }
        } else {
            Exception exception = new Exception("Whitelist - no body, response code is " + response.code());
            fireOnFailure(whitelistServiceListener, exception);
            if (callbacks != null) {
                callbacks.onFailure(exception);
            }
        }
        int code = response.code();
        response.close();
        if (code != 200) {
            Exception exception = new Exception("Whitelist - response code is " + response.code());
            fireOnFailure(whitelistServiceListener, exception);
            if (callbacks != null) {
                callbacks.onFailure(exception);
            }
        }
    }

    private String toJson(IWhitelistableTransaction whitelistableTransaction) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("envelop", whitelistableTransaction.getTransactionPayload());
        jo.put("network_id", whitelistableTransaction.getNetworkPassphrase());
        return jo.toString();
    }

    private void fireOnFailure(final WhitelistServiceListener whitelistServiceListener, final Exception e) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (whitelistServiceListener != null) {
                    whitelistServiceListener.onFailure(e);
                }
            }

        });
    }

    private void fireOnSuccess(final WhitelistServiceListener whitelistServiceListener, final String response) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (whitelistServiceListener != null) {
                    whitelistServiceListener.onSuccess(response);
                }
            }
        });
    }
}




