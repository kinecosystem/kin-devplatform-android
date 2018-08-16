package kin.devplatform;

public class Environment implements KinEnvironment {

	private static final Environment PLAYGROUND = new Environment(
		"https://horizon-playground.kininfrastructure.com/",
		"Kin Playground Network ; June 2018",
		"GBC3SG6NGTSZ2OMH3FFGB7UVRQWILW367U4GSOOF4TFSZONV42UJXUH7",
		"https://api.developers.kinecosystem.com/v1",
		"https://s3.amazonaws.com/assets.kinplayground.com/web-offers/cards-based/index.html",
		"https://kin-bi.appspot.com/devp_play_");

	private final String blockchainNetworkUrl;
	private final String blockchainPassphrase;
	private final String issuer;
	private final String ecosystemServerUrl;
	private final String ecosystemWebFront;
	private final String biUrl;

	public Environment(String blockchainNetworkUrl, String blockchainPassphrase,
		String issuer, String ecosystemServerUrl, String ecosystemWebFront, String biUrl) {
		this.blockchainNetworkUrl = blockchainNetworkUrl;
		this.blockchainPassphrase = blockchainPassphrase;
		this.issuer = issuer;
		this.ecosystemServerUrl = ecosystemServerUrl;
		this.ecosystemWebFront = ecosystemWebFront;
		this.biUrl = biUrl;
	}

	@Override
	public String getBlockchainNetworkUrl() {
		return blockchainNetworkUrl;
	}

	@Override
	public String getBlockchainPassphrase() {
		return blockchainPassphrase;
	}

	@Override
	public String getIssuer() {
		return issuer;
	}

	@Override
	public String getEcosystemServerUrl() {
		return ecosystemServerUrl;
	}

	@Override
	public String getEcosystemWebFront() {
		return ecosystemWebFront;
	}

	@Override
	public String getBiUrl() {
		return biUrl;
	}

	public static KinEnvironment getProduction() {
		return null;
	}

	public static KinEnvironment getPlayground() {
		return PLAYGROUND;
	}
}
