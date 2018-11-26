
package kin.devplatform.bi.events;

// Augmented by script

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import kin.devplatform.bi.Event;
import kin.devplatform.bi.EventsStore;


/**
 * Clients tracks the OrderID on the blockchain 
 * 
 */
public class EarnOrderPaymentConfirmed implements Event {

	public static final String EVENT_NAME = "earn_order_payment_confirmed";
	public static final String EVENT_TYPE = "log";

	// Augmented by script
	public static EarnOrderPaymentConfirmed create(String transactionId, String orderId, Boolean isNative,
		EarnOrderPaymentConfirmed.Origin origin, String operationId) {
		return new EarnOrderPaymentConfirmed(
			(Common) EventsStore.common(),
			(User) EventsStore.user(),
			(Client) EventsStore.client(),
			transactionId,
			orderId,
			isNative,
			origin,
			operationId);
	}

	/**
	 * (Required)
	 */
	@SerializedName("event_name")
	@Expose
	private String eventName = EVENT_NAME;
	/**
	 * (Required)
	 */
	@SerializedName("event_type")
	@Expose
	private String eventType = EVENT_TYPE;
	/**
	 * common properties for all events (Required)
	 */
	@SerializedName("common")
	@Expose
	private Common common;
	/**
	 * common user properties (Required)
	 */
	@SerializedName("user")
	@Expose
	private User user;
	/**
	 * common properties for all client events (Required)
	 */
	@SerializedName("client")
	@Expose
	private Client client;
	/**
	 * (Required)
	 */
	@SerializedName("transaction_id")
	@Expose
	private String transactionId;
	/**
	 * (Required)
	 */
	@SerializedName("order_id")
	@Expose
	private String orderId;
	/**
	 * (Required)
	 */
	@SerializedName("is_native")
	@Expose
	private Boolean isNative;
	/**
	 * (Required)
	 */
	@SerializedName("origin")
	@Expose
	private EarnOrderPaymentConfirmed.Origin origin;
	/**
	 * (Required)
	 */
	@SerializedName("operation_id")
	@Expose
	private String operationId;

	/**
	 * No args constructor for use in serialization
	 */
	public EarnOrderPaymentConfirmed() {
	}

	/**
	 *
	 * @param common
	 * @param orderId
	 * @param origin

	 * @param client
	 * @param operationId

	 * @param user
	 * @param transactionId
	 * @param isNative
	 */
	public EarnOrderPaymentConfirmed(Common common, User user, Client client, String transactionId, String orderId,
		Boolean isNative, EarnOrderPaymentConfirmed.Origin origin, String operationId) {
		super();
		this.common = common;
		this.user = user;
		this.client = client;
		this.transactionId = transactionId;
		this.orderId = orderId;
		this.isNative = isNative;
		this.origin = origin;
		this.operationId = operationId;
	}

	/**
	 * (Required)
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * (Required)
	 */
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	/**
	 * (Required)
	 */
	public String getEventType() {
		return eventType;
	}

	/**
	 * (Required)
	 */
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	/**
	 * common properties for all events (Required)
	 */
	public Common getCommon() {
		return common;
	}

	/**
	 * common properties for all events (Required)
	 */
	public void setCommon(Common common) {
		this.common = common;
	}

	/**
	 * common user properties (Required)
	 */
	public User getUser() {
		return user;
	}

	/**
	 * common user properties (Required)
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * common properties for all client events (Required)
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * common properties for all client events (Required)
	 */
	public void setClient(Client client) {
		this.client = client;
	}

	/**
	 * (Required)
	 */
	public String getTransactionId() {
		return transactionId;
	}

	/**
	 * (Required)
	 */
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	/**
	 * (Required)
	 */
	public String getOrderId() {
		return orderId;
	}

	/**
	 * (Required)
	 */
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	/**
	 * (Required)
	 */
	public Boolean getIsNative() {
		return isNative;
	}

	/**
	 * (Required)
	 */
	public void setIsNative(Boolean isNative) {
		this.isNative = isNative;
	}

	/**
	 * (Required)
	 */
	public EarnOrderPaymentConfirmed.Origin getOrigin() {
		return origin;
	}

	/**
	 * (Required)
	 */
	public void setOrigin(EarnOrderPaymentConfirmed.Origin origin) {
		this.origin = origin;
	}

	/**
	 * (Required)
	 */
	public String getOperationId() {
		return operationId;
	}

	/**
	 * (Required)
	 */
	public void setOperationId(String operationId) {
		this.operationId = operationId;
	}

	public enum Origin {

		@SerializedName("marketplace")
		MARKETPLACE("marketplace"),
		@SerializedName("external")
		EXTERNAL("external");
		private final String value;
		private final static Map<String, EarnOrderPaymentConfirmed.Origin> CONSTANTS = new HashMap<String, EarnOrderPaymentConfirmed.Origin>();

		static {
			for (EarnOrderPaymentConfirmed.Origin c : values()) {
				CONSTANTS.put(c.value, c);
			}
		}

		private Origin(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

		public String value() {
			return this.value;
		}

		public static EarnOrderPaymentConfirmed.Origin fromValue(String value) {
			EarnOrderPaymentConfirmed.Origin constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}

}
