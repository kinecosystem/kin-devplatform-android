
package kin.devplatform.bi.events;

// Augmented by script

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import kin.devplatform.bi.Event;
import kin.devplatform.bi.EventsStore;


/**
 * checking burn succeed
 */
public class MigrationCheckBurnSucceeded implements Event {

	public static final String EVENT_NAME = "migration_check_burn_succeeded";
	public static final String EVENT_TYPE = "log";

	// Augmented by script
	public static MigrationCheckBurnSucceeded create(MigrationCheckBurnSucceeded.CheckBurnReason checkBurnReason,
		String publicAddress) {
		return new MigrationCheckBurnSucceeded(
			(Common) EventsStore.common(),
			(User) EventsStore.user(),
			(Client) EventsStore.client(),
			checkBurnReason,
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
	@SerializedName("check_burn_reason")
	@Expose
	private MigrationCheckBurnSucceeded.CheckBurnReason checkBurnReason;
	/**
	 * (Required)
	 */
	@SerializedName("public_address")
	@Expose
	private String publicAddress;

	/**
	 * No args constructor for use in serialization
	 */
	public MigrationCheckBurnSucceeded() {
	}

	/**
	 *
	 * @param common
	 * @param checkBurnReason

	 * @param client
	 * @param publicAddress

	 * @param user
	 */
	public MigrationCheckBurnSucceeded(Common common, User user, Client client,
		MigrationCheckBurnSucceeded.CheckBurnReason checkBurnReason, String publicAddress) {
		super();
		this.common = common;
		this.user = user;
		this.client = client;
		this.checkBurnReason = checkBurnReason;
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
	public MigrationCheckBurnSucceeded.CheckBurnReason getCheckBurnReason() {
		return checkBurnReason;
	}

	/**
	 * (Required)
	 */
	public void setCheckBurnReason(MigrationCheckBurnSucceeded.CheckBurnReason checkBurnReason) {
		this.checkBurnReason = checkBurnReason;
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

	public enum CheckBurnReason {

		@SerializedName("not_burned")
		NOT_BURNED("not_burned"),
		@SerializedName("already_burned")
		ALREADY_BURNED("already_burned"),
		@SerializedName("no_account")
		NO_ACCOUNT("no_account"),
		@SerializedName("no_trustline")
		NO_TRUSTLINE("no_trustline");
		private final String value;
		private final static Map<String, MigrationCheckBurnSucceeded.CheckBurnReason> CONSTANTS = new HashMap<String, MigrationCheckBurnSucceeded.CheckBurnReason>();

		static {
			for (MigrationCheckBurnSucceeded.CheckBurnReason c : values()) {
				CONSTANTS.put(c.value, c);
			}
		}

		private CheckBurnReason(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

		public String value() {
			return this.value;
		}

		public static MigrationCheckBurnSucceeded.CheckBurnReason fromValue(String value) {
			MigrationCheckBurnSucceeded.CheckBurnReason constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}

}
