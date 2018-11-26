package kin.devplatform.sample;

import android.app.Application;
import android.support.annotation.NonNull;
import com.crashlytics.android.Crashlytics;
import com.squareup.leakcanary.LeakCanary;

import io.fabric.sdk.android.Fabric;
import kin.devplatform.Environment;
import kin.devplatform.Kin;
import kin.devplatform.KinEnvironment;
import kin.devplatform.data.model.WhitelistData;
import kin.devplatform.exception.BlockchainException;
import kin.devplatform.exception.ClientException;
import kin.devplatform.sample.model.SignInRepo;


public class App extends Application {


	@Override
	public void onCreate() {
		super.onCreate();

		Fabric.with(this, new Crashlytics());

		KinEnvironment environment = Environment.getPlayground();

		if (BuildConfig.IS_JWT_REGISTRATION) {
			/**
			 * SignInData should be created with registration JWT {see https://jwt.io/} created securely by server side
			 * In the the this example {@link SignInRepo#getJWT} generate the JWT locally.
			 * DO NOT!!!! use this approach in your real app.
			 * */
			String jwt = SignInRepo.getJWT(this);

			try {
				Kin.start(getApplicationContext(), jwt, environment);
			} catch (ClientException | BlockchainException e) {
				e.printStackTrace();
			}
		} else {
			/** Use {@link WhitelistData} for small scale testing */
			WhitelistData whitelistData = SignInRepo.getWhitelistSignInData(this, getAppId(), getApiKey());
			try {
				Kin.start(getApplicationContext(), whitelistData, environment);
			} catch (ClientException | BlockchainException e) {
				e.printStackTrace();
			}
		}

		Kin.enableLogs(true);

		if (LeakCanary.isInAnalyzerProcess(this)) {
			// This process is dedicated to LeakCanary for heap analysis.
			// You should not init your app in this process.
			return;
		}
		LeakCanary.install(this);
	}

	@NonNull
	private static String getAppId() {
		return BuildConfig.SAMPLE_APP_ID;
	}

	@NonNull
	private static String getApiKey() {
		return BuildConfig.SAMPLE_API_KEY;
	}

}
