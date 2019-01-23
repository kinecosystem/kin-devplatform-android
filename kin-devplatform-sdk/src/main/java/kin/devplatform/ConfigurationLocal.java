package kin.devplatform;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.annotation.NonNull;

public class ConfigurationLocal implements Configuration.Local {

	private static volatile ConfigurationLocal instance;

	private static final String CONFIGURATION_PREF_NAME = "com.kin.ecosystem.sdk_configuration_pref";

	private static final String OLD_BLOCKCHAIN_NETWORK_URL_KEY = "old_blockchain_network_url";
	private static final String OLD_BLOCKCHAIN_PASSPHRASE_KEY = "old_blockchain_passphrase";
	private static final String NEW_BLOCKCHAIN_NETWORK_URL_KEY = "new_blockchain_network_url";
	private static final String NEW_BLOCKCHAIN_PASSPHRASE_KEY = "new_blockchain_passphrase";
	private static final String OLD_BLOCKCHAIN_ISSUER_KEY = "old_blockchain_issuer";
	private static final String MIGRATION_SERVICE_URL_KEY = "migration_service_url";
	private static final String ECOSYSTEM_SERVER_URL_KEY = "ecosystem_server_url";
	private static final String ECOSYSTEM_WEB_FRONT_URL_KEY = "ecosystem_web_front_url";
	private static final String BI_URL_KEY = "bi_url";


	private final SharedPreferences configurationSharedPreferences;

	private ConfigurationLocal(Context context) {
		this.configurationSharedPreferences = context
			.getSharedPreferences(CONFIGURATION_PREF_NAME, Context.MODE_PRIVATE);
	}

	public static ConfigurationLocal getInstance(@NonNull Context context) {
		if (instance == null) {
			synchronized (ConfigurationLocal.class) {
				if (instance == null) {
					instance = new ConfigurationLocal(context);
				}
			}
		}

		return instance;
	}

	@Override
	public KinEnvironment getEnvironment() {
		final String oldBlockchainNetworkUrl = configurationSharedPreferences.getString(OLD_BLOCKCHAIN_NETWORK_URL_KEY, null);
		final String oldBlockchainPassphrase = configurationSharedPreferences.getString(OLD_BLOCKCHAIN_PASSPHRASE_KEY, null);
		final String newBlockchainNetworkUrl = configurationSharedPreferences.getString(NEW_BLOCKCHAIN_NETWORK_URL_KEY, null);
		final String newBlockchainPassphrase = configurationSharedPreferences.getString(NEW_BLOCKCHAIN_PASSPHRASE_KEY, null);
		final String issuer = configurationSharedPreferences.getString(OLD_BLOCKCHAIN_ISSUER_KEY, null);
		final String migrationServiceUrl = configurationSharedPreferences.getString(MIGRATION_SERVICE_URL_KEY, null);
		final String ecosystemServerUrl = configurationSharedPreferences.getString(ECOSYSTEM_SERVER_URL_KEY, null);
		final String ecosystemWebFrontUrl = configurationSharedPreferences.getString(ECOSYSTEM_WEB_FRONT_URL_KEY, null);
		final String biUrl = configurationSharedPreferences.getString(BI_URL_KEY, null);
		if (oldBlockchainNetworkUrl == null || oldBlockchainPassphrase == null || newBlockchainNetworkUrl  == null || newBlockchainPassphrase == null ||
			issuer == null || ecosystemServerUrl == null || ecosystemWebFrontUrl == null || biUrl == null) {
			return null;
		} else {
			return new Environment(oldBlockchainNetworkUrl, oldBlockchainPassphrase, newBlockchainNetworkUrl, newBlockchainPassphrase,
				issuer, migrationServiceUrl, ecosystemServerUrl, ecosystemWebFrontUrl, biUrl);
		}

	}

	@Override
	public void setEnvironment(@NonNull KinEnvironment kinEnvironment) {
		Editor editor = configurationSharedPreferences.edit();
		editor.putString(OLD_BLOCKCHAIN_NETWORK_URL_KEY, kinEnvironment.getOldBlockchainNetworkUrl());
		editor.putString(OLD_BLOCKCHAIN_PASSPHRASE_KEY, kinEnvironment.getOldBlockchainPassphrase());
		editor.putString(NEW_BLOCKCHAIN_NETWORK_URL_KEY, kinEnvironment.getNewBlockchainNetworkUrl());
		editor.putString(NEW_BLOCKCHAIN_PASSPHRASE_KEY, kinEnvironment.getNewBlockchainPassphrase());
		editor.putString(OLD_BLOCKCHAIN_ISSUER_KEY, kinEnvironment.getOldBlockchainIssuer());
		editor.putString(MIGRATION_SERVICE_URL_KEY, kinEnvironment.getMigrationServiceUrl());
		editor.putString(ECOSYSTEM_SERVER_URL_KEY, kinEnvironment.getEcosystemServerUrl());
		editor.putString(ECOSYSTEM_WEB_FRONT_URL_KEY, kinEnvironment.getEcosystemWebFront());
		editor.putString(BI_URL_KEY, kinEnvironment.getBiUrl());
		editor.apply();
	}
}
