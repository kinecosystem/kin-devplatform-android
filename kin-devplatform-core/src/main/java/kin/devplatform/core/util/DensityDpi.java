package kin.devplatform.core.util;


import static kin.devplatform.core.util.DensityDpi.HDPI;
import static kin.devplatform.core.util.DensityDpi.XHDPI;
import static kin.devplatform.core.util.DensityDpi.XXHDPI;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({HDPI, XHDPI, XXHDPI})
@Retention(RetentionPolicy.SOURCE)
public @interface DensityDpi {

	int HDPI = 0x00000240;
	int XHDPI = 0x00000320;
	int XXHDPI = 0x00000480;
}
