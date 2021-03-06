
package kin.devplatform.bi.events;

// Augmented by script

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import kin.devplatform.bi.Event;
import kin.devplatform.bi.EventsStore;


/**
 * Checking if account is burned
 */
public class MigrationCheckBurnStarted implements Event {

	public static final String EVENT_NAME = "migration_check_burn_started";
	public static final String EVENT_TYPE = "log";

	// Augmented by script
	public static MigrationCheckBurnStarted create(String publicAddress) {
		return new MigrationCheckBurnStarted(
			(Common) EventsStore.common(),
			(User) EventsStore.user(),
			(Client) EventsStore.client(),
			publicAddress);
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
	@SerializedName("public_address")
	@Expose
	private String publicAddress;

	/**
	 * No args constructor for use in serialization
	 */
	public MigrationCheckBurnStarted() {
	}

	/**
	 *
	 * @param common

	 * @param client
	 * @param publicAddress

	 * @param user
	 */
	public MigrationCheckBurnStarted(Common common, User user, Client client, String publicAddress) {
		super();
		this.common = common;
		this.user = user;
		this.client = client;
		this.publicAddress = publicAddress;
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
	public String getPublicAddress() {
		return publicAddress;
	}

	/**
	 * (Required)
	 */
	public void setPublicAddress(String publicAddress) {
		this.publicAddress = publicAddress;
	}

}
