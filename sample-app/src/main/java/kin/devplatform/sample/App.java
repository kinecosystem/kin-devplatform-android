package kin.devplatform.sample;

import android.app.Application;
import com.crashlytics.android.Crashlytics;
import com.squareup.leakcanary.LeakCanary;
import io.fabric.sdk.android.Fabric;


public class App extends Application {


	@Override
	public void onCreate() {
		super.onCreate();
		if (LeakCanary.isInAnalyzerProcess(this)) {
			// This process is dedicated to LeakCanary for heap analysis.
			// You should not init your app in this process.
			return;
		}
		LeakCanary.install(this);

		Fabric.with(this, new Crashlytics());
	}

}
