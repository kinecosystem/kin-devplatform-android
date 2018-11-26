package kin.devplatform.bi.events;

import java.util.UUID;

public interface CommonReadonly {

	String getVersion();

	String getUserId();

	Long getTimestamp();

	String getPlatform();

	String getSchemaVersion();

	UUID getEventId();

}
