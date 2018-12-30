package kin.devplatform.base;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import kin.devplatform.KinEcosystemInitiator;

public abstract class KinEcosystemBaseActivity extends AppCompatActivity {

	protected abstract @LayoutRes
	int getLayoutRes();

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutRes());

		KinEcosystemInitiator.getInstance().internalInit(this);
	}
}
