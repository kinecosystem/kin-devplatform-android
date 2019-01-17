package kin.devplatform;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.telephony.TelephonyManager;
import java.util.Locale;
import java.util.UUID;
import kin.devplatform.bi.EventsStore;
import kin.devplatform.bi.EventsStore.ClientModifier;
import kin.devplatform.bi.EventsStore.CommonModifier;
import kin.devplatform.bi.EventsStore.DynamicValue;
import kin.devplatform.bi.EventsStore.UserModifier;
import kin.devplatform.bi.events.ClientProxy;
import kin.devplatform.bi.events.CommonProxy;
import kin.devplatform.bi.events.UserProxy;
import kin.devplatform.data.auth.AuthRepository;
import kin.devplatform.data.blockchain.BlockchainSourceImpl;

class EventCommonDataUtil {

	static void setBaseData(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		final String carrierName =
			telephonyManager != null ? safeguardNullString(telephonyManager.getSimOperatorName()) : "null";
		final CommonModifier commonModifier = new CommonModifier() {
			@Override
			public void modify(CommonProxy commonProxy) {
				commonProxy.setTimestamp(new DynamicValue<Long>() {
					@Override
					public Long get() {
						return System.currentTimeMillis();
					}
				});

				commonProxy.setUserId(new DynamicValue<String>() {
					@Override
					public String get() {
						final String ecosystemUserID = AuthRepository.getInstance().getEcosystemUserID();
						return safeguardNullString(ecosystemUserID);
					}
				});

				commonProxy.setEventId(new DynamicValue<UUID>() {
					@Override
					public UUID get() {
						return UUID.randomUUID();
					}
				});

				commonProxy.setVersion(BuildConfig.VERSION_NAME);
			}
		};

		final ClientModifier clientModifier = new ClientModifier() {
			@Override
			public void modify(ClientProxy commonProxy) {
				commonProxy.setDeviceId(new DynamicValue<String>() {
					@Override
					public String get() {
						return AuthRepository.getInstance().getDeviceID();
					}
				});
				commonProxy.setCarrier(carrierName);
				commonProxy.setOs(VERSION.RELEASE);
				commonProxy.setDeviceManufacturer(Build.MANUFACTURER);
				commonProxy.setDeviceModel(Build.MODEL);
				commonProxy.setLanguage(new DynamicValue<String>() {
					@Override
					public String get() {
						return Locale.getDefault().getDisplayLanguage();
					}
				});
			}
		};

		final UserModifier userModifier = new UserModifier() {
			@Override
			public void modify(UserProxy userProxy) {
				userProxy.setBalance(new DynamicValue<Double>() {
					@Override
					public Double get() {
						return BlockchainSourceImpl.getInstance().getBalance().getAmount().doubleValue();
					}
				});

				userProxy.setDigitalServiceId(new DynamicValue<String>() {
					@Override
					public String get() {
						final String digitalServiceId = AuthRepository.getInstance().getAppID();
						return safeguardNullString(digitalServiceId);
					}
				});
				userProxy.setDigitalServiceUserId(new DynamicValue<String>() {
					@Override
					public String get() {
						final String userID = AuthRepository.getInstance().getUserID();
						return safeguardNullString(userID);
					}
				});
				userProxy.setEntryPointParam("");
				userProxy.setEarnCount(0);
				userProxy.setEarnCount(0);
				userProxy.setEarnCount(0);
				userProxy.setSpendCount(0);
				userProxy.setTotalKinEarned(0.0);
				userProxy.setTotalKinSpent(0.0);
				userProxy.setTransactionCount(0);

			}
		};

		EventsStore.init();
		EventsStore.update(userModifier);
		EventsStore.update(commonModifier);
		EventsStore.update(clientModifier);
	}

	private static String safeguardNullString(final String text) {
		return text != null ? text : "null";
	}
}
