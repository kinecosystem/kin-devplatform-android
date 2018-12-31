package kin.devplatform;

public interface Configuration {

	KinEnvironment getEnvironment();

	void setEnvironment(KinEnvironment environment);

	interface Local {

		KinEnvironment getEnvironment();

		void setEnvironment(KinEnvironment kinEnvironment);
	}

}
