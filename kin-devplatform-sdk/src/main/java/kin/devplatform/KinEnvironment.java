package kin.devplatform;

public interface KinEnvironment {

	String getOldBlockchainNetworkUrl();

	String getOldBlockchainPassphrase();

	String getNewBlockchainNetworkUrl();

	String getNewBlockchainPassphrase();

	String getOldBlockchainIssuer();

	String getMigrationServiceUrl();

	String getEcosystemServerUrl();

	String getEcosystemWebFront();

	String getBiUrl();
}


