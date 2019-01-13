
package kin.devplatform.bi.events;

// Augmented by script

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import kin.devplatform.bi.Event;
import kin.devplatform.bi.EventsStore;


/**
 * Client failing to get the blockchain version
 * 
 */
public class MigrationSelectedSDK implements Event {

	public static final String EVENT_NAME = "migration_selected_sdk";
	public static final String EVENT_TYPE = "business";

	// Augmented by script
	public static MigrationSelectedSDK create(boolean isNewSDK, String source) {
		return new MigrationSelectedSDK(
			(Common) EventsStore.common(),
			(User) EventsStore.user(),
			(Client) EventsStore.client(),
				isNewSDK,
				source);
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
	@SerializedName("is_new_SDK")
	@Expose
	private boolean isNewSdk;
	/**
	 * (Required)
	 */
	@SerializedName("source")
	@Expose
	private String source;

	/**
	 *
	 * @param common
	 * @param client
	 * @param user
	 * @param isNewSDK
	 * @param source
	 */
	public MigrationSelectedSDK(Common common, User user, Client client, boolean isNewSDK, String source) {
		super();
		this.common = common;
		this.user = user;
		this.client = client;
		this.isNewSdk = isNewSDK;
		this.source = source;
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
	public boolean getIsNewSdk() {
		return this.isNewSdk;
	}

	/**
	 * (Required)
	 */
	public void setIsNewSdk(boolean isNewSDK) {
		this.isNewSdk = isNewSDK;
	}

	/**
	 * (Required)
	 */
	public String getSource() {
		return this.source;
	}

	/**
	 * (Required)
	 */
	public void setSource(String source) {
		this.source = source;
	}

}
