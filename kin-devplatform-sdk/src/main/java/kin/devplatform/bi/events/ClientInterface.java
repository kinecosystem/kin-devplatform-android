package kin.devplatform.bi.events;

public interface ClientInterface extends ClientReadonly {

	void setOs(String os);

	void setLanguage(String language);

	void setDeviceManufacturer(String deviceManufacturer);

	void setCarrier(String carrier);

	void setDeviceId(String deviceId);

	void setDeviceModel(String deviceModel);

}
