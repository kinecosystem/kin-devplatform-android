
package kin.devplatform.bi.events;

// Augmented by script

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import kin.devplatform.bi.Event;
import kin.devplatform.bi.EventsStore;


/**
 * Client request OrderID for purchase an offer
 * 
 */
public class PayToUserOrderCreationRequested implements Event {

	public static final String EVENT_NAME = "pay_to_user_order_creation_requested";
	public static final String EVENT_TYPE = "business";

	// Augmented by script
	public static PayToUserOrderCreationRequested create(String offerId,
		PayToUserOrderCreationRequested.Origin origin) {
		return new PayToUserOrderCreationRequested(
			(Common) EventsStore.common(),
			(User) EventsStore.user(),
			(Client) EventsStore.client(),
			offerId,
			origin);
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
	@SerializedName("offer_id")
	@Expose
	private String offerId;
	/**
	 * (Required)
	 */
	@SerializedName("origin")
	@Expose
	private PayToUserOrderCreationRequested.Origin origin;

	/**
	 * No args constructor for use in serialization
	 */
	public PayToUserOrderCreationRequested() {
	}

	/**
	 *
	 * @param common
	 * @param origin

	 * @param client
	 * @param offerId

	 * @param user
	 */
	public PayToUserOrderCreationRequested(Common common, User user, Client client, String offerId,
		PayToUserOrderCreationRequested.Origin origin) {
		super();
		this.common = common;
		this.user = user;
		this.client = client;
		this.offerId = offerId;
		this.origin = origin;
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
	public String getOfferId() {
		return offerId;
	}

	/**
	 * (Required)
	 */
	public void setOfferId(String offerId) {
		this.offerId = offerId;
	}

	/**
	 * (Required)
	 */
	public PayToUserOrderCreationRequested.Origin getOrigin() {
		return origin;
	}

	/**
	 * (Required)
	 */
	public void setOrigin(PayToUserOrderCreationRequested.Origin origin) {
		this.origin = origin;
	}

	public enum Origin {

		@SerializedName("marketplace")
		MARKETPLACE("marketplace"),
		@SerializedName("external")
		EXTERNAL("external");
		private final String value;
		private final static Map<String, PayToUserOrderCreationRequested.Origin> CONSTANTS = new HashMap<String, PayToUserOrderCreationRequested.Origin>();

		static {
			for (PayToUserOrderCreationRequested.Origin c : values()) {
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

		public static PayToUserOrderCreationRequested.Origin fromValue(String value) {
			PayToUserOrderCreationRequested.Origin constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}

}
