package kin.devplatform.sample;

import android.app.Application;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import kin.devplatform.Environment;
import kin.devplatform.Kin;


public class App extends Application {


	@Override
	public void onCreate() {
		super.onCreate();

		Fabric.with(this, new Crashlytics());

		initSdk();
	}

	private void initSdk() {
		Kin.initialize(getApplicationContext(), Environment.getPlayground());
		Kin.enableLogs(true);
	}

}
