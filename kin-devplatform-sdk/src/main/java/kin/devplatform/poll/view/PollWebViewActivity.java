package kin.devplatform.poll.view;

import static kin.devplatform.exception.ClientException.INTERNAL_INCONSISTENCY;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import kin.devplatform.R;
import kin.devplatform.base.BaseToolbarActivity;
import kin.devplatform.bi.EventLoggerImpl;
import kin.devplatform.data.order.OrderRepository;
import kin.devplatform.exception.ClientException;
import kin.devplatform.poll.presenter.IPollWebViewPresenter;
import kin.devplatform.poll.presenter.PollWebViewPresenter;
import kin.devplatform.util.ErrorUtil;
import kin.devplatform.web.EcosystemWebView;

public class PollWebViewActivity extends BaseToolbarActivity implements IPollWebView {

	public static Intent createIntent(final Context context, @NonNull PollBundle bundle) throws ClientException {
		final Intent intent = new Intent(context, PollWebViewActivity.class);
		intent.putExtras(bundle.build());
		return intent;
	}

	private IPollWebViewPresenter pollWebViewPresenter;
	private EcosystemWebView webView;
	private LinearLayout webViewContainer;

	@Override
	protected int getLayoutRes() {
		return R.layout.kinecosystem_activity_poll;
	}

	@Override
	protected int getTitleRes() {
		return EMPTY_TITLE;
	}

	@Override
	protected int getNavigationIcon() {
		return R.drawable.kinecosystem_ic_back_black;
	}

	@Override
	protected View.OnClickListener getNavigationClickListener() {
		return new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pollWebViewPresenter.closeClicked();
			}
		};
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PollBundle pollBundle = new PollBundle(getIntent().getExtras());
		attachPresenter(
			new PollWebViewPresenter(pollBundle.getJsonData(),
				pollBundle.getOfferID(),
				pollBundle.getContentType(),
				pollBundle.getAmount(),
				pollBundle.getTitle(),
				OrderRepository.getInstance(),
				EventLoggerImpl.getInstance()));
	}

	@Override
	public void onBackPressed() {
		pollWebViewPresenter.closeClicked();
	}

	@Override
	public void attachPresenter(PollWebViewPresenter presenter) {
		pollWebViewPresenter = presenter;
		pollWebViewPresenter.onAttach(this);
	}

	@Override
	protected void initViews() {
		webView = findViewById(R.id.webview);
		webViewContainer = findViewById(R.id.webview_container);
	}

	@Override
	public void showToast(@Message final int msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(PollWebViewActivity.this, getMessageResId(msg), Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void showMigrationErrorDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				removeAndReleaseWebView();
				final AlertDialog dialog = new Builder(PollWebViewActivity.this)
					.setTitle(getString(R.string.kinecosystem_dialog_migration_is_needed_title))
					.setMessage(getString(R.string.kinecosystem_dialog_migration_is_needed_message))
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}
					})
					.setCancelable(false)
					.create();
				dialog.show();
			}
		});
	}

	private @StringRes
	int getMessageResId(@Message final int msg) {
		switch (msg) {
			case ORDER_SUBMISSION_FAILED:
				return R.string.kinecosystem_order_submission_failed;
			default:
			case SOMETHING_WENT_WRONG:
				return R.string.kinecosystem_something_went_wrong;
		}
	}

	@Override
	public void loadUrl() {
		webView.setListener(pollWebViewPresenter);
		webView.load();
	}

	@Override
	public void renderJson(@NonNull final String pollJsonString) {
		webView.render(pollJsonString);
	}

	@Override
	public void showToolbar() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getToolbar().setVisibility(View.VISIBLE);
			}
		});
	}

	@Override
	public void hideToolbar() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getToolbar().setVisibility(View.GONE);
			}
		});
	}

	@Override
	public void setTitle(String title) {
		getToolbar().setTitle(title);
	}

	@Override
	public void close() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				removeAndReleaseWebView();
			}
		});
		finish();
	}

	private void removeAndReleaseWebView() {
		webViewContainer.removeView(webView);
		webView.release();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		close();
		pollWebViewPresenter.onDetach();
	}

	public static class PollBundle {

		private Bundle bundle;

		private static final int FIELD_COUNT = 3;

		private static final String EXTRA_JSON_DATA_KEY = "jsondata";
		private static final String EXTRA_OFFER_ID_KEY = "offer_id";
		private static final String EXTRA_CONTENT_TYPE_KEY = "content_type";
		private static final String EXTRA_AMOUNT_KEY = "amount";
		private static final String EXTRA_TITLE_KEY = "title";

		public PollBundle() {
			this.bundle = new Bundle();
		}

		public PollBundle(Bundle bundle) {
			this.bundle = bundle;
		}

		public PollBundle setJsonData(String jsonData) {
			this.bundle.putString(EXTRA_JSON_DATA_KEY, jsonData);
			return this;
		}

		public String getJsonData() {
			return bundle.getString(EXTRA_JSON_DATA_KEY);
		}

		public PollBundle setOfferID(String offerID) {
			this.bundle.putString(EXTRA_OFFER_ID_KEY, offerID);
			return this;
		}

		public String getOfferID() {
			return bundle.getString(EXTRA_OFFER_ID_KEY);
		}

		public PollBundle setTitle(String title) {
			this.bundle.putString(EXTRA_TITLE_KEY, title);
			return this;
		}

		public String getTitle() {
			return bundle.getString(EXTRA_TITLE_KEY);
		}

		public PollBundle setContentType(String contentType) {
			this.bundle.putString(EXTRA_CONTENT_TYPE_KEY, contentType);
			return this;
		}

		public String getContentType() {
			return bundle.getString(EXTRA_CONTENT_TYPE_KEY);
		}

		public PollBundle setAmount(int amount) {
			this.bundle.putInt(EXTRA_AMOUNT_KEY, amount);
			return this;
		}

		public int getAmount() {
			return bundle.getInt(EXTRA_AMOUNT_KEY);
		}

		public Bundle build() throws ClientException {
			if (bundle.size() < FIELD_COUNT) {
				throw ErrorUtil.getClientException(INTERNAL_INCONSISTENCY,
					new IllegalArgumentException("You must specified all the fields."));
			}
			return bundle;
		}

	}
}
