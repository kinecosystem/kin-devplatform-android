
package kin.devplatform.bi.events;

// Augmented by script

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import kin.devplatform.bi.Event;
import kin.devplatform.bi.EventsStore;


/**
 * Ready callback is triggered, sdk is ready
 */
public class MigrationCallbackReady implements Event {

	public static final String EVENT_NAME = "migration_callback_ready";
	public static final String EVENT_TYPE = "log";

	// Augmented by script
	public static MigrationCallbackReady create(MigrationCallbackReady.SelectedSdkReason selectedSdkReason,
		MigrationCallbackReady.SdkVersion sdkVersion) {
		return new MigrationCallbackReady(
			(Common) EventsStore.common(),
			(User) EventsStore.user(),
			(Client) EventsStore.client(),
			selectedSdkReason,
			sdkVersion);
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
	@SerializedName("selected_sdk_reason")
	@Expose
	private MigrationCallbackReady.SelectedSdkReason selectedSdkReason;
	/**
	 * (Required)
	 */
	@SerializedName("sdk_version")
	@Expose
	private MigrationCallbackReady.SdkVersion sdkVersion;

	/**
	 * No args constructor for use in serialization
	 */
	public MigrationCallbackReady() {
	}

	/**
	 *
	 * @param common

	 * @param client
	 * @param sdkVersion

	 * @param user
	 * @param selectedSdkReason
	 */
	public MigrationCallbackReady(Common common, User user, Client client,
		MigrationCallbackReady.SelectedSdkReason selectedSdkReason, MigrationCallbackReady.SdkVersion sdkVersion) {
		super();
		this.common = common;
		this.user = user;
		this.client = client;
		this.selectedSdkReason = selectedSdkReason;
		this.sdkVersion = sdkVersion;
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
	public MigrationCallbackReady.SelectedSdkReason getSelectedSdkReason() {
		return selectedSdkReason;
	}

	/**
	 * (Required)
	 */
	public void setSelectedSdkReason(MigrationCallbackReady.SelectedSdkReason selectedSdkReason) {
		this.selectedSdkReason = selectedSdkReason;
	}

	/**
	 * (Required)
	 */
	public MigrationCallbackReady.SdkVersion getSdkVersion() {
		return sdkVersion;
	}

	/**
	 * (Required)
	 */
	public void setSdkVersion(MigrationCallbackReady.SdkVersion sdkVersion) {
		this.sdkVersion = sdkVersion;
	}

	public enum SdkVersion {

		@SerializedName("2")
		_2("2"),
		@SerializedName("3")
		_3("3");
		private final String value;
		private final static Map<String, MigrationCallbackReady.SdkVersion> CONSTANTS = new HashMap<String, MigrationCallbackReady.SdkVersion>();

		static {
			for (MigrationCallbackReady.SdkVersion c : values()) {
				CONSTANTS.put(c.value, c);
			}
		}

		private SdkVersion(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

		public String value() {
			return this.value;
		}

		public static MigrationCallbackReady.SdkVersion fromValue(String value) {
			MigrationCallbackReady.SdkVersion constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}

	public enum SelectedSdkReason {

		@SerializedName("migrated")
		MIGRATED("migrated"),
		@SerializedName("already_migrated")
		ALREADY_MIGRATED("already_migrated"),
		@SerializedName("no_account_to_migrate")
		NO_ACCOUNT_TO_MIGRATE("no_account_to_migrate"),
		@SerializedName("api_check")
		API_CHECK("api_check");
		private final String value;
		private final static Map<String, MigrationCallbackReady.SelectedSdkReason> CONSTANTS = new HashMap<String, MigrationCallbackReady.SelectedSdkReason>();

		static {
			for (MigrationCallbackReady.SelectedSdkReason c : values()) {
				CONSTANTS.put(c.value, c);
			}
		}

		private SelectedSdkReason(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

		public String value() {
			return this.value;
		}

		public static MigrationCallbackReady.SelectedSdkReason fromValue(String value) {
			MigrationCallbackReady.SelectedSdkReason constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}

}
