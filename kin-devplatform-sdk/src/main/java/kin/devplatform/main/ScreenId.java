package kin.devplatform.main;

import static kin.devplatform.main.ScreenId.MARKETPLACE;
import static kin.devplatform.main.ScreenId.ORDER_HISTORY;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({MARKETPLACE, ORDER_HISTORY})
@Retention(RetentionPolicy.SOURCE)
public @interface ScreenId {

	int NONE = 0x00000000;
	int MARKETPLACE = 0x00000001;
	int ORDER_HISTORY = 0x00000002;
}