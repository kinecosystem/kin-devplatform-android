
package kin.devplatform.bi.events;

// Augmented by script

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import kin.devplatform.bi.Event;
import kin.devplatform.bi.EventsStore;


/**
 * Client fails to create spend order
 * 
 */
public class SpendOrderCreationFailed implements Event {
    public static final String EVENT_NAME = "spend_order_creation_failed";
    public static final String EVENT_TYPE = "log";

    // Augmented by script
	public static SpendOrderCreationFailed create(String errorReason, String offerId,
		SpendOrderCreationFailed.Origin origin, String errorCode, String errorMessage) {
        return new SpendOrderCreationFailed(
            (Common) EventsStore.common(),
            (User) EventsStore.user(),
            (Client) EventsStore.client(),
            errorReason,
            offerId,
            origin,
            errorCode,
            errorMessage);
    }

    /**
	 *
     * (Required)
	 *
     */
    @SerializedName("event_name")
    @Expose
    private String eventName = EVENT_NAME;
    /**
	 *
     * (Required)
	 *
     */
    @SerializedName("event_type")
    @Expose
    private String eventType = EVENT_TYPE;
    /**
	 * common properties for all events
	 * (Required)
	 *
     */
    @SerializedName("common")
    @Expose
    private Common common;
    /**
	 * common user properties
	 * (Required)
	 *
     */
    @SerializedName("user")
    @Expose
    private User user;
    /**
	 * common properties for all client events
	 * (Required)
	 *
     */
    @SerializedName("client")
    @Expose
    private Client client;
    /**
	 *
     * (Required)
	 *
     */
    @SerializedName("error_reason")
    @Expose
    private String errorReason;
    /**
	 *
     * (Required)
	 *
     */
    @SerializedName("offer_id")
    @Expose
    private String offerId;
    /**
	 *
     * (Required)
	 *
     */
    @SerializedName("origin")
    @Expose
    private SpendOrderCreationFailed.Origin origin;
    /**
	 *
     * (Required)
	 *
     */
    @SerializedName("error_code")
    @Expose
    private String errorCode;
    /**
	 *
     * (Required)
	 *
     */
    @SerializedName("error_message")
    @Expose
    private String errorMessage;

    /**
     * No args constructor for use in serialization
	 *
     */
    public SpendOrderCreationFailed() {
    }

    /**
	 *
     * @param common
     * @param errorReason
     * @param origin
     * @param errorMessage

     * @param client
     * @param offerId
     * @param errorCode

     * @param user
     */
	public SpendOrderCreationFailed(Common common, User user, Client client, String errorReason, String offerId,
		SpendOrderCreationFailed.Origin origin, String errorCode, String errorMessage) {
        super();
        this.common = common;
        this.user = user;
        this.client = client;
        this.errorReason = errorReason;
        this.offerId = offerId;
        this.origin = origin;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
	 *
     * (Required)
	 *
     */
    public String getEventName() {
        return eventName;
    }

    /**
	 *
     * (Required)
	 *
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
	 *
     * (Required)
	 *
     */
    public String getEventType() {
        return eventType;
    }

    /**
	 *
     * (Required)
	 *
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
	 * common properties for all events
	 * (Required)
	 *
     */
    public Common getCommon() {
        return common;
    }

    /**
	 * common properties for all events
	 * (Required)
	 *
     */
    public void setCommon(Common common) {
        this.common = common;
    }

    /**
	 * common user properties
	 * (Required)
	 *
     */
    public User getUser() {
        return user;
    }

    /**
	 * common user properties
	 * (Required)
	 *
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
	 * common properties for all client events
	 * (Required)
	 *
     */
    public Client getClient() {
        return client;
    }

    /**
	 * common properties for all client events
	 * (Required)
	 *
     */
    public void setClient(Client client) {
        this.client = client;
    }

    /**
	 *
     * (Required)
	 *
     */
    public String getErrorReason() {
        return errorReason;
    }

    /**
	 *
     * (Required)
	 *
     */
    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    /**
	 *
     * (Required)
	 *
     */
    public String getOfferId() {
        return offerId;
    }

    /**
	 *
     * (Required)
	 *
     */
    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    /**
	 *
     * (Required)
	 *
     */
    public SpendOrderCreationFailed.Origin getOrigin() {
        return origin;
    }

    /**
	 *
     * (Required)
	 *
     */
    public void setOrigin(SpendOrderCreationFailed.Origin origin) {
        this.origin = origin;
    }

    /**
	 *
     * (Required)
	 *
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
	 *
     * (Required)
	 *
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
	 *
     * (Required)
	 *
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
	 *
     * (Required)
	 *
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public enum Origin {

        @SerializedName("marketplace")
        MARKETPLACE("marketplace"),
        @SerializedName("external")
        EXTERNAL("external");
        private final String value;
        private final static Map<String, SpendOrderCreationFailed.Origin> CONSTANTS = new HashMap<String, SpendOrderCreationFailed.Origin>();

        static {
			for (SpendOrderCreationFailed.Origin c : values()) {
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

        public static SpendOrderCreationFailed.Origin fromValue(String value) {
            SpendOrderCreationFailed.Origin constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
