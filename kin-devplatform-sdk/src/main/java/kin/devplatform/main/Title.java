package kin.devplatform.main;

import static kin.devplatform.main.Title.MARKETPLACE_TITLE;
import static kin.devplatform.main.Title.ORDER_HISTORY_TITLE;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({MARKETPLACE_TITLE, ORDER_HISTORY_TITLE})
@Retention(RetentionPolicy.SOURCE)
public @interface Title {

	int MARKETPLACE_TITLE = 0x00000001;
	int ORDER_HISTORY_TITLE = 0x00000002;
}
