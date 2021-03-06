package kin.devplatform.splash.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import kin.devplatform.R;
import kin.devplatform.base.KinEcosystemBaseActivity;
import kin.devplatform.bi.EventLoggerImpl;
import kin.devplatform.data.auth.AuthRepository;
import kin.devplatform.main.view.EcosystemActivity;
import kin.devplatform.splash.presenter.ISplashPresenter;
import kin.devplatform.splash.presenter.SplashPresenter;
import kin.devplatform.splash.view.SplashScreenButton.LoadAnimationListener;

public class SplashViewActivity extends KinEcosystemBaseActivity implements ISplashView {

	private ISplashPresenter splashPresenter;

	private SplashScreenButton letsGetStartedBtn;
	private TextView loadingText;

	private static final int FADE_DURATION = 250;
	private static final float ZERO_FLOAT = 0f;
	private static final float ONE_FLOAT = 1f;
	private Animation fadeIn = new AlphaAnimation(ZERO_FLOAT, ONE_FLOAT);
	private Animation fadeOutLoading = new AlphaAnimation(ONE_FLOAT, ZERO_FLOAT);

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		attachPresenter(new SplashPresenter(AuthRepository.getInstance(), EventLoggerImpl.getInstance()));
		initViews();
		initAnimations();
	}

	@Override
	protected int getLayoutRes() {
		return R.layout.kinecosystem_activity_splash;
	}

	private void initAnimations() {
		fadeIn.setDuration(FADE_DURATION);
		fadeOutLoading.setDuration(FADE_DURATION);
		fadeOutLoading.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				loadingText.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
	}

	private void initViews() {
		letsGetStartedBtn = findViewById(R.id.lets_get_started);
		loadingText = findViewById(R.id.loading_text);
		setUpBackButton();
		setUpLetsGetStartedButton();
	}

	private void setUpLetsGetStartedButton() {
		letsGetStartedBtn = findViewById(R.id.lets_get_started);
		letsGetStartedBtn.setButtonListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				splashPresenter.getStartedClicked();
			}
		});
		letsGetStartedBtn.setLoadAnimationListener(new LoadAnimationListener() {
			@Override
			public void onAnimationEnd() {
				splashPresenter.onAnimationEnded();
			}
		});
	}

	private void setUpBackButton() {
		final ImageView backButton = findViewById(R.id.back_btn);
		backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				splashPresenter.backButtonPressed();
			}
		});
	}

	@Override
	public void attachPresenter(SplashPresenter presenter) {
		splashPresenter = presenter;
		splashPresenter.onAttach(this);
	}

	@Override
	public void navigateToMarketPlace() {
		Intent marketplaceIntent = new Intent(this, EcosystemActivity.class);
		startActivity(marketplaceIntent);
		finish();
	}

	@Override
	public void navigateBack() {
		onBackPressed();
	}

	@Override
	public void animateLoading() {
		letsGetStartedBtn.animateLoading();
		fadeInView(loadingText);
	}

	@Override
	public void stopLoading(boolean reset) {
		letsGetStartedBtn.stopLoading(reset);
		fadeOutLoading();
	}

	private void fadeInView(View view) {
		view.setVisibility(View.VISIBLE);
		view.startAnimation(fadeIn);
	}

	private void fadeOutLoading() {
		loadingText.startAnimation(fadeOutLoading);
	}

	@Override
	public void showToast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(SplashViewActivity.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		splashPresenter.onDetach();
	}
}
