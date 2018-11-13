package kin.devplatform;

public class Environment implements KinEnvironment {

	private static Environment PLAYGROUND;
	private static Environment PRODUCTION;

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

	public static KinEnvironment getPlayground() {
		if (PLAYGROUND == null) {
			PLAYGROUND = new Environment(
				"https://horizon-playground.kininfrastructure.com/",
				"Kin Playground Network ; June 2018",
				"GBC3SG6NGTSZ2OMH3FFGB7UVRQWILW367U4GSOOF4TFSZONV42UJXUH7",
				"https://api.developers.kinecosystem.com/v1",
				"https://s3.amazonaws.com/assets.kinplayground.com/web-offers/cards-based/index.html",
				"https://kin-bi.appspot.com/devp_play_");
		}
		return PLAYGROUND;
	}

	public static KinEnvironment getProduction() {
		if (PRODUCTION == null) {
			PRODUCTION = new Environment(
				"https://horizon-ecosystem.kininfrastructure.com/",
				"Public Global Kin Ecosystem Network ; June 2018",
				"GDF42M3IPERQCBLWFEZKQRK77JQ65SCKTU3CW36HZVCX7XX5A5QXZIVK",
				"https://api-prod.developers.kinecosystem.com/v1",
				"https://s3.amazonaws.com/assets.developers.kinecosystem.com/web-offers/cards-based/index.html",
					"https://kin-bi.appspot.com/devp_");
		}
		return PRODUCTION;
	}

	public static KinEnvironment getLocal() {
		if (PLAYGROUND == null) {
			PLAYGROUND = new Environment(
				"https://horizon-playground.kininfrastructure.com/",
				"Kin Playground Network ; June 2018",
				"GBC3SG6NGTSZ2OMH3FFGB7UVRQWILW367U4GSOOF4TFSZONV42UJXUH7",
				"http://10.0.2.2:3000/v1",
				"https://s3.amazonaws.com/assets.kinplayground.com/web-offers/cards-based/index.html",
				"https://kin-bi.appspot.com/devp_play_");
		}
		return PLAYGROUND;
	}

}
