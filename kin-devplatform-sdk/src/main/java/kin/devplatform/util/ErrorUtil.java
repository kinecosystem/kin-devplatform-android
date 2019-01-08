package kin.devplatform.util;

import static kin.devplatform.exception.BlockchainException.ACCOUNT_ACTIVATION_FAILED;
import static kin.devplatform.exception.BlockchainException.ACCOUNT_CREATION_FAILED;
import static kin.devplatform.exception.BlockchainException.ACCOUNT_NOT_FOUND;
import static kin.devplatform.exception.BlockchainException.INSUFFICIENT_KIN;
import static kin.devplatform.exception.BlockchainException.TRANSACTION_FAILED;
import static kin.devplatform.exception.ClientException.BAD_CONFIGURATION;
import static kin.devplatform.exception.ClientException.INTERNAL_INCONSISTENCY;
import static kin.devplatform.exception.ClientException.SDK_NOT_STARTED;
import static kin.devplatform.exception.KinEcosystemException.UNKNOWN;
import static kin.devplatform.exception.ServiceException.SERVICE_ERROR;
import static kin.devplatform.exception.ServiceException.TIMEOUT_ERROR;

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import kin.core.exception.AccountNotActivatedException;
import kin.core.exception.AccountNotFoundException;
import kin.core.exception.CreateAccountException;
import kin.core.exception.InsufficientKinException;
import kin.core.exception.TransactionFailedException;
import kin.devplatform.core.network.ApiException;
import kin.devplatform.core.network.model.Error;
import kin.devplatform.exception.BlockchainException;
import kin.devplatform.exception.ClientException;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.exception.ServiceException;
import kin.devplatform.network.model.Order;
import kin.devplatform.network.model.Order.Status;

public class ErrorUtil {

	private static final String THE_ECOSYSTEM_SERVER_RETURNED_AN_ERROR = "The Ecosystem server returned an error. See underlying Error for details";
	private static final String ECOSYSTEM_SDK_ENCOUNTERED_AN_UNEXPECTED_ERROR = "Ecosystem SDK encountered an unexpected error";
	private static final String BLOCKCHAIN_ENCOUNTERED_AN_UNEXPECTED_ERROR = "Blockchain encountered an unexpected error";
	private static final String THE_OPERATION_TIMED_OUT = "The operation timed out";
	private static final String YOU_DO_NOT_HAVE_ENOUGH_KIN = "You do not have enough Kin to perform this operation";
	private static final String THE_TRANSACTION_OPERATION_FAILED = "The transaction operation failed. This can happen for several reasons. Please see underlying Error for more info";
	private static final String FAILED_TO_CREATE_A_BLOCKCHAIN_WALLET_KEYPAIR = "Failed to create a blockchain wallet keypair";
	private static final String THE_REQUESTED_ACCOUNT_COULD_NOT_BE_FOUND = "The requested account could not be found";
	private static final String FAILED_TO_ACTIVATE_ON_THE_BLOCKCHAIN_NETWORK = "A Wallet was created locally, but failed to activate on the blockchain network";
	private static final String ECOSYSTEM_SDK_IS_NOT_STARTED = "Operation not permitted: Ecosystem SDK is not started, please call Kin.initialize(...) first.";
	private static final String BAD_OR_MISSING_PARAMETERS = "Bad or missing parameters";
	private static final String FAILED_TO_LOAD_ACCOUNT_ON_INDEX = "Failed to load blockchain wallet on index %d";
	private static final String ORDER_NOT_FOUND = "Cannot found a order";
	private static final String ACCOUNT_IS_NOT_LOGGED_IN = "Account is not logged in, please call Kin.start(...) first.";
	private static final String ACCOUNT_CREATION_TIMEOUT = "Account creation has timeout";


	private static final int REQUEST_TIMEOUT_CODE = 408;

	public static KinEcosystemException fromFailedOrder(Order order) {
		if (order != null && order.getStatus() == Status.FAILED) {
			Error orderError = order.getError();
			if (orderError != null) {
				return new ServiceException(orderError.getCode(), orderError.getMessage(),
					null);
			} else {
				return new ServiceException(SERVICE_ERROR, THE_ECOSYSTEM_SERVER_RETURNED_AN_ERROR,
					null);
			}
		}
		return null;
	}

	public static KinEcosystemException fromApiException(ApiException apiException) {
		KinEcosystemException exception;
		if (apiException == null) {
			exception = createUnknownException(null);
		} else {
			final int apiCode = apiException.getCode();
			switch (apiCode) {
				case 400:
				case 401:
				case 404:
				case 409:
				case 500:
					Error errorResponseBody = apiException.getResponseBody();
					if (errorResponseBody != null) {
						exception = new ServiceException(errorResponseBody.getCode(), errorResponseBody.getMessage(),
							apiException);
					} else {
						exception = new ServiceException(SERVICE_ERROR, THE_ECOSYSTEM_SERVER_RETURNED_AN_ERROR,
							apiException);
					}
					break;
				case REQUEST_TIMEOUT_CODE:
					exception = new ServiceException(TIMEOUT_ERROR, THE_OPERATION_TIMED_OUT, apiException);
					break;
				case INTERNAL_INCONSISTENCY:
					exception = new ClientException(INTERNAL_INCONSISTENCY, THE_OPERATION_TIMED_OUT, apiException);
					break;
				default:
					exception = createUnknownException(apiException);
					break;
			}
		}
		return exception;
	}

	private static KinEcosystemException createUnknownException(@Nullable Throwable throwable) {
		return new KinEcosystemException(UNKNOWN, ECOSYSTEM_SDK_ENCOUNTERED_AN_UNEXPECTED_ERROR, throwable);
	}

	public static ApiException createOrderTimeoutException() {
		final String errorTitle = "Time out";
		final String errorMsg = "order timed out";
		final int apiCode = REQUEST_TIMEOUT_CODE;
		ApiException apiException = new ApiException(apiCode, errorTitle);
		apiException.setResponseBody(new Error(errorTitle, errorMsg, TIMEOUT_ERROR));
		return apiException;
	}

	public static BlockchainException getBlockchainException(Exception error) {
		final BlockchainException exception;
		if (error instanceof InsufficientKinException) {
			exception = new BlockchainException(INSUFFICIENT_KIN, YOU_DO_NOT_HAVE_ENOUGH_KIN, error);
		} else if (error instanceof TransactionFailedException) {
			exception = new BlockchainException(TRANSACTION_FAILED, THE_TRANSACTION_OPERATION_FAILED, error);
		} else if (error instanceof CreateAccountException) {
			exception = new BlockchainException(ACCOUNT_CREATION_FAILED, FAILED_TO_CREATE_A_BLOCKCHAIN_WALLET_KEYPAIR,
				error);
		} else if (error instanceof AccountNotFoundException) {
			exception = new BlockchainException(ACCOUNT_NOT_FOUND, THE_REQUESTED_ACCOUNT_COULD_NOT_BE_FOUND, error);
		} else if (error instanceof AccountNotActivatedException) {
			exception = new BlockchainException(ACCOUNT_ACTIVATION_FAILED,
				FAILED_TO_ACTIVATE_ON_THE_BLOCKCHAIN_NETWORK, error);
		} else {
			exception = new BlockchainException(UNKNOWN, BLOCKCHAIN_ENCOUNTERED_AN_UNEXPECTED_ERROR, error);
		}

		return exception;
	}

	public static ClientException getClientException(final int code, @Nullable Exception e) {
		final ClientException exception;
		switch (code) {
			case ClientException.SDK_NOT_STARTED:
				exception = new ClientException(SDK_NOT_STARTED, ECOSYSTEM_SDK_IS_NOT_STARTED, e);
				break;
			case ClientException.BAD_CONFIGURATION:
				exception = new ClientException(BAD_CONFIGURATION, BAD_OR_MISSING_PARAMETERS, e);
				break;
			case ClientException.ORDER_NOT_FOUND:
				exception = new ClientException(ClientException.ORDER_NOT_FOUND, ORDER_NOT_FOUND, e);
				break;
			case ClientException.ACCOUNT_NOT_LOGGED_IN:
				exception = new ClientException(ClientException.ACCOUNT_NOT_LOGGED_IN, ACCOUNT_IS_NOT_LOGGED_IN, e);
				break;
			case INTERNAL_INCONSISTENCY:
			default:
				exception = new ClientException(INTERNAL_INCONSISTENCY, ECOSYSTEM_SDK_ENCOUNTERED_AN_UNEXPECTED_ERROR,
					e);
		}

		return exception;
	}

	@SuppressLint("DefaultLocale")
	public static BlockchainException createAccountCannotLoadedExcpetion(int accountIndex) {
		return new BlockchainException(BlockchainException.ACCOUNT_LOADING_FAILED,
			String.format(FAILED_TO_LOAD_ACCOUNT_ON_INDEX, accountIndex), null);
	}

	@SuppressLint("DefaultLocale")
	public static BlockchainException createCreateAccountTimeoutException(Exception e) {
		return new BlockchainException(BlockchainException.ACCOUNT_CREATION_TIMEOUT,
			ACCOUNT_CREATION_TIMEOUT, e);
	}

	public static String getPrintableStackTrace(Throwable t) {
		String stackTrace = Log.getStackTraceString(t);
		//Handle Unknown Host Exception errors that are swallowed by Android Log
		if (stackTrace == null || stackTrace.isEmpty()) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			stackTrace = sw.toString();
		}
		return stackTrace;
	}

	@SuppressLint("DefaultLocale")
	public static BlockchainException createAccountCannotLoadedException(int accountIndex) {
		return new BlockchainException(BlockchainException.ACCOUNT_LOADING_FAILED,
			String.format(FAILED_TO_LOAD_ACCOUNT_ON_INDEX, accountIndex), null);
	}
}
