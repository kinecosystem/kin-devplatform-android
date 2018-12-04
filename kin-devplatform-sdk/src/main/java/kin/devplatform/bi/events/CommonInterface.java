package kin.devplatform.bi.events;

import java.util.UUID;

public interface CommonInterface extends CommonReadonly {

	void setVersion(String version);

	void setUserId(String userId);

	void setTimestamp(Long timestamp);

	void setPlatform(String platform);

	void setSchemaVersion(String schemaVersion);

	void setEventId(UUID eventId);

}
