package kin.devplatform.exception;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ServiceException extends KinEcosystemException {

	@IntDef({SERVICE_ERROR, NETWORK_ERROR, TIMEOUT_ERROR, USER_NOT_FOUND_ERROR, USER_NOT_ACTIVATED,
		WALLET_WAS_NOT_CREATED_IN_THIS_APP})
	@Retention(RetentionPolicy.SOURCE)
	public @interface ServiceErrorCodes {

	}

	public static final int SERVICE_ERROR = 5001;
	public static final int NETWORK_ERROR = 5002;
	public static final int TIMEOUT_ERROR = 5003;
	public static final int USER_NOT_FOUND_ERROR = 4046;
	public static final int USER_NOT_ACTIVATED = 4015;
	public static final int WALLET_WAS_NOT_CREATED_IN_THIS_APP = 40010;


	public ServiceException(@ServiceErrorCodes int code, String message, Throwable cause) {
		super(code, message, cause);
	}
}
