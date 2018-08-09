package kin.devplatform.base.transformation;

import static kin.devplatform.base.transformation.CornerType.ALL;
import static kin.devplatform.base.transformation.CornerType.BOTTOM;
import static kin.devplatform.base.transformation.CornerType.BOTTOM_LEFT;
import static kin.devplatform.base.transformation.CornerType.BOTTOM_RIGHT;
import static kin.devplatform.base.transformation.CornerType.LEFT;
import static kin.devplatform.base.transformation.CornerType.RIGHT;
import static kin.devplatform.base.transformation.CornerType.TOP;
import static kin.devplatform.base.transformation.CornerType.TOP_LEFT;
import static kin.devplatform.base.transformation.CornerType.TOP_RIGHT;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({ALL, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT})
@Retention(RetentionPolicy.SOURCE)
public @interface CornerType {

	int ALL = 0;
	int TOP_LEFT = 1;
	int TOP_RIGHT = 2;
	int BOTTOM_LEFT = 3;
	int BOTTOM_RIGHT = 4;
	int TOP = 5;
	int BOTTOM = 6;
	int LEFT = 7;
	int RIGHT = 8;
}