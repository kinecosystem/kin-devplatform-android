package kin.devplatform;

public interface KinEnvironment {

	String getOldBlockchainNetworkUrl();

	String getOldBlockchainPassphrase();

	String getNewBlockchainNetworkUrl();

	String getNewBlockchainPassphrase();

	String getIssuer();

	String getEcosystemServerUrl();

	String getEcosystemWebFront();

	String getBiUrl();
}


