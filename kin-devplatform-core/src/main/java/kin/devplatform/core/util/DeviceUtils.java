package kin.devplatform.core.util;

import static android.util.DisplayMetrics.DENSITY_HIGH;
import static android.util.DisplayMetrics.DENSITY_XHIGH;
import static android.util.DisplayMetrics.DENSITY_XXHIGH;
import static kin.devplatform.core.util.DensityDpi.HDPI;
import static kin.devplatform.core.util.DensityDpi.XHDPI;
import static kin.devplatform.core.util.DensityDpi.XXHDPI;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

public class DeviceUtils {

	private static @DensityDpi
	int densityDpi = HDPI;

	private static int screenHeight;
	private static int screenWidth;

	public static void init(Context context) {
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		int orientation = context.getResources().getConfiguration().orientation;
		checkDensityDpi(displayMetrics);
		checkScreenSize(orientation, displayMetrics);
	}

	private static void checkDensityDpi(DisplayMetrics displayMetrics) {
		int dpi = displayMetrics.densityDpi;
		if (dpi <= DENSITY_HIGH) {
			densityDpi = HDPI;
		} else if (dpi >= DENSITY_XHIGH && dpi <= DENSITY_XXHIGH) {
			densityDpi = XHDPI;
		} else {
			densityDpi = XXHDPI;
		}
	}

	private static void checkScreenSize(int orientation, DisplayMetrics displayMetrics) {
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			screenHeight = displayMetrics.widthPixels;
			screenWidth = displayMetrics.heightPixels;
		} else {
			screenHeight = displayMetrics.heightPixels;
			screenWidth = displayMetrics.widthPixels;
		}
	}


	public static boolean isDensity(@DensityDpi int dpi) {
		return densityDpi == dpi;
	}

	public static int getScreenHeight() {
		return screenHeight;
	}

	public static int getScreenWidth() {
		return screenWidth;
	}

}
