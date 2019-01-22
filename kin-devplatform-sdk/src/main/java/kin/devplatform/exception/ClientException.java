package kin.devplatform.exception;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ClientException extends KinEcosystemException {

	@IntDef({SDK_NOT_STARTED, BAD_CONFIGURATION, INTERNAL_INCONSISTENCY,
		ORDER_NOT_FOUND, INCORRECT_APP_ID, BAD_JWT})
	@Retention(RetentionPolicy.SOURCE)
	public @interface ClientErrorCodes {

	}

	public static final int SDK_NOT_STARTED = 4001;
	public static final int BAD_CONFIGURATION = 4002;
	public static final int INTERNAL_INCONSISTENCY = 4003;
	public static final int ORDER_NOT_FOUND = 4004;
	public static final int INCORRECT_APP_ID = 4006; // user appId is not equals to the appId we got from server.
	public static final int BAD_JWT = 4007;

	public ClientException(@ClientErrorCodes int code, String message, Throwable cause) {
		super(code, message, cause);
	}
}
