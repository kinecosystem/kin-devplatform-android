
package kin.devplatform.bi.events;

// Augmented by script

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

import kin.devplatform.bi.Event;
import kin.devplatform.bi.EventsStore;


/**
 * Client failing to burn
 * 
 */
public class MigrationBurnFailed implements Event {

	public static final String EVENT_NAME = "migration_burn_failed";
	public static final String EVENT_TYPE = "business";

	// Augmented by script
	public static MigrationBurnFailed create(Exception exception, BigDecimal balance) {
		return new MigrationBurnFailed(
			(Common) EventsStore.common(),
			(User) EventsStore.user(),
			(Client) EventsStore.client(),
				exception,
				balance);
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
	@SerializedName("exception")
	@Expose
	private Exception exception;
	/**
	 * (Required)
	 */
	@SerializedName("balance")
	@Expose
	private BigDecimal balance;

	/**
	 *
	 * @param common
	 * @param client
	 * @param user
	 * @param exception
	 * @param balance
	 */
	public MigrationBurnFailed(Common common, User user, Client client, Exception exception, BigDecimal balance) {
		super();
		this.common = common;
		this.user = user;
		this.client = client;
		this.exception = exception;
		this.balance = balance;
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
	public Exception getException() {
		return this.exception;
	}

	/**
	 * (Required)
	 */

	public void setException(Exception exception) {
		this.exception = exception;
	}

	/**
	 * (Required)
	 */
	public BigDecimal getBalance() {
		return this.balance;
	}

	/**
	 * (Required)
	 */
	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

}
