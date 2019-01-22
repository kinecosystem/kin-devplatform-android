package kin.devplatform;

import android.util.Log;
import kin.devplatform.Log.Priority;

public class Logger {

	private static final String BASE_TAG = "KinEcosystem - ";

	private static boolean shouldLog;

	private Logger() {
	}

	static boolean isEnabled() {
		return shouldLog;
	}

	public static void log(kin.devplatform.Log ecosystemLog) {
		ecosystemLog.log();
	}

	public static void log(@Priority final int priority, final String tag, final String content) {
		if (shouldLog) {
			Log.println(priority, getTag(tag), content);
		}
	}

	static void enableLogs(final boolean enableLogs) {
		Logger.shouldLog = enableLogs;
	}

	private static String getTag(String tag) {
		return BASE_TAG + tag;
	}
}
