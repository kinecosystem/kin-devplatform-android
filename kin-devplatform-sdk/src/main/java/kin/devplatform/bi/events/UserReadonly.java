package kin.devplatform.bi.events;



public interface UserReadonly {

	String getDigitalServiceUserId();

	Integer getTransactionCount();

	Double getBalance();

	Integer getEarnCount();

	Double getTotalKinSpent();

	String getDigitalServiceId();

	String getEntryPointParam();

	Integer getSpendCount();

	Double getTotalKinEarned();

}
