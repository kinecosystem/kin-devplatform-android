
package kin.devplatform.bi.events;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.UUID;


/**
 * common properties for all events
 * 
 */
public class Common implements CommonInterface {

	public static final String PLATFORM = "Android";
	public static final String SCHEMA_VERSION = "ae402f5971a1e697f0495f03ef048c01c923c971";

	/**
	 * (Required)
	 */
	@SerializedName("version")
	@Expose
	private String version;
	/**
	 * (Required)
	 */
	@SerializedName("user_id")
	@Expose
	private String userId;
	/**
	 * (Required)
	 */
	@SerializedName("timestamp")
	@Expose
	private Long timestamp;
	/**
	 * (Required)
	 */
	@SerializedName("platform")
	@Expose
	private String platform = PLATFORM;
	/**
	 * (Required)
	 */
	@SerializedName("schema_version")
	@Expose
	private String schemaVersion = SCHEMA_VERSION;
	/**
	 * (Required)
	 */
	@SerializedName("event_id")
	@Expose
	private UUID eventId;

	/**
	 * No args constructor for use in serialization
	 */
	public Common() {
	}

	/**
	 *
	 * @param eventId
	 * @param schemaVersion
	 * @param version
	 * @param userId
	 * @param timestamp
	 */
	public Common(String version, String userId, Long timestamp, String schemaVersion, UUID eventId) {
		super();
		this.version = version;
		this.userId = userId;
		this.timestamp = timestamp;
		this.schemaVersion = schemaVersion;
		this.eventId = eventId;
	}

	/**
	 * (Required)
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * (Required)
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * (Required)
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * (Required)
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * (Required)
	 */
	public Long getTimestamp() {
		return timestamp;
	}

	/**
	 * (Required)
	 */
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * (Required)
	 */
	public String getPlatform() {
		return platform;
	}

	/**
	 * (Required)
	 */
	public void setPlatform(String platform) {
		this.platform = platform;
	}

	/**
	 * (Required)
	 */
	public String getSchemaVersion() {
		return schemaVersion;
	}

	/**
	 * (Required)
	 */
	public void setSchemaVersion(String schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	/**
	 * (Required)
	 */
	public UUID getEventId() {
		return eventId;
	}

	/**
	 * (Required)
	 */
	public void setEventId(UUID eventId) {
		this.eventId = eventId;
	}

}
