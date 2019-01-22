package kin.devplatform.sample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;
import kin.devplatform.Environment;
import kin.devplatform.Kin;
import kin.devplatform.KinCallback;
import kin.devplatform.KinMigrationListener;
import kin.devplatform.base.Observer;
import kin.devplatform.data.model.Balance;
import kin.devplatform.data.model.OrderConfirmation;
import kin.devplatform.exception.ClientException;
import kin.devplatform.exception.KinEcosystemException;
import kin.devplatform.marketplace.model.NativeSpendOffer;
import kin.devplatform.sample.model.SignInRepo;
import kin.devplatform.util.ErrorUtil;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "Ecosystem - SampleApp";

	private TextView balanceView;
	private Button nativeSpendButton;
	private Button nativeEarnButton;
	private Button showPublicAddressButton;
	private Button payToUserButton;
	private Button getOrderConfirmationButton;
	private boolean isInitialized;

	private TextView publicAddressTextArea;
	private KinCallback<OrderConfirmation> nativeSpendOrderConfirmationCallback;
	private KinCallback<OrderConfirmation> nativePayToUserOrderConfirmationCallback;
	private KinCallback<OrderConfirmation> nativeEarnOrderConfirmationCallback;
	private Observer<NativeSpendOffer> nativeSpendOfferClickedObserver;

	private Observer<Balance> balanceObserver;

	private String publicAddress;
	int randomID = new Random().nextInt((9999 - 1) + 1) + 1;
	NativeSpendOffer nativeSpendOffer =
		new NativeSpendOffer(String.valueOf(randomID))
			.title("Get Themes")
			.description("Personalize your chat")
			.amount(100)
			.image("https://s3.amazonaws.com/kinmarketplace-assets/version1/kik_theme_offer+2.png");
	private String payToUserRecipientUserId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		balanceView = findViewById(R.id.get_balance);
		nativeSpendButton = findViewById(R.id.native_spend_button);
		nativeEarnButton = findViewById(R.id.native_earn_button);
		payToUserButton = findViewById(R.id.pay_to_user_button);
		showPublicAddressButton = findViewById(R.id.show_public_address);
		publicAddressTextArea = findViewById(R.id.public_text_area);
		getOrderConfirmationButton = findViewById(R.id.order_confirmation_button);
		showPublicAddressButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (publicAddress == null) {
					getPublicAddress();
				} else {
					copyToClipboard(publicAddress);
				}
			}
		});
		balanceView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enableView(v, false);
				getBalance();
			}
		});
		nativeSpendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showToast("Native spend flow started");
				enableView(v, false);
				createNativeSpendOffer();
			}
		});
		nativeEarnButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showToast("Native earn flow started");
				enableView(v, false);
				createNativeEarnOffer();
			}
		});
		getOrderConfirmationButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enableView(v, false);
				getOrderConfirmation(JwtUtil.lastId);
			}
		});
		payToUserButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				showPayToUserDialog(v);
			}
		});
		findViewById(R.id.launch_marketplace).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openKinMarketplace();
			}
		});

		startSdk();
	}

	private void startSdk() {
		/**
		 * SignInData should be created with registration JWT {see https://jwt.io/} created securely by server side
		 * In the the this example {@link SignInRepo#getJWT} generate the JWT locally.
		 * DO NOT!!!! use this approach in your real app.
		 * */
		String jwt = SignInRepo.getJWT(this);
		Kin.enableLogs(true);
		Kin.start(getApplicationContext(), jwt, Environment.getPlayground(),
			new KinCallback<Void>() {
			@Override
			public void onResponse(Void response) {
				Toast.makeText(MainActivity.this, "Starting SDK succeeded", Toast.LENGTH_LONG).show();
				addNativeSpendOffer(nativeSpendOffer);
				addNativeOfferClickedObserver();
				addBalanceObserver();
				isInitialized = true;
			}

			@Override
			public void onFailure(KinEcosystemException error) {
				Toast.makeText(MainActivity.this, "Starting SDK failed", Toast.LENGTH_LONG).show();
				Log.e(TAG, "Kin.start() failed with =  " + ErrorUtil.getPrintableStackTrace(error));
			}
		}, new KinMigrationListener() {
			@Override
			public void onStart() {
				Toast.makeText(MainActivity.this, "Migration started", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onFinish() {
				Toast.makeText(MainActivity.this, "Migration finished successfully!", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onError(Exception e) {
				Log.e(TAG, "Migration onError, e =  " + ErrorUtil.getPrintableStackTrace(e));
				Toast.makeText(MainActivity.this, "Migration failed with " + e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void showPayToUserDialog(final View v) {
		final EditText editUserId = new EditText(MainActivity.this);
		editUserId.setHint("Recipient User ID");
		final AlertDialog dialog = new Builder(MainActivity.this)
			.setView(editUserId)
			.setTitle("Enter Recipient User Id")
			.setPositiveButton("Pay To User", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					payToUserRecipientUserId = editUserId.getText().toString();
					showToast("Pay to user flow started");
					enableView(v, false);
					createPayToUserOffer();
					dialog.dismiss();
				}
			})
			.setNegativeButton("Cancel", null)
			.create();
		dialog.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface d) {
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
			}
		});

		editUserId.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (TextUtils.isEmpty(s)) {
					dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				} else {
					dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
				}

			}
		});

		dialog.show();
	}

	@Override
	protected void onStart() {
		super.onStart();
		// only if sdk is initialized then add the observer.
		if (isInitialized) {
			addBalanceObserver();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		removeBalanceObserver();
	}

	private void addBalanceObserver() {
		if (balanceObserver == null) {
			balanceObserver = new Observer<Balance>() {
				@Override
				public void onChanged(Balance value) {
					showToast("Balance - " + value.getAmount().intValue());
				}
			};

			try {
				Kin.addBalanceObserver(balanceObserver);
			} catch (ClientException e) {
				e.printStackTrace();
			}
		}

	}

	private void removeBalanceObserver() {
		try {
			Kin.removeBalanceObserver(balanceObserver);
			balanceObserver = null;
		} catch (ClientException e) {
			e.printStackTrace();
		}
	}

	// Use this method to remove the nativeSpendOffer you added
	private void removeNativeOffer(@NonNull NativeSpendOffer nativeSpendOffer) {
		try {
			if (Kin.removeNativeOffer(nativeSpendOffer)) {
				showToast("Native offer removed");
			}
		} catch (ClientException e) {
			e.printStackTrace();
		}
	}

	private void addNativeOfferClickedObserver() {
		try {
			Kin.addNativeOfferClickedObserver(getNativeOfferClickedObserver());
		} catch (ClientException e) {
			showToast("Could not add native offer callback");
		}
	}

	private Observer<NativeSpendOffer> getNativeOfferClickedObserver() {
		if (nativeSpendOfferClickedObserver == null) {
			nativeSpendOfferClickedObserver = new Observer<NativeSpendOffer>() {
				@Override
				public void onChanged(NativeSpendOffer value) {
					Intent nativeOfferIntent = NativeOfferActivity.createIntent(MainActivity.this, value.getTitle());
					startActivity(nativeOfferIntent);
				}
			};
		}
		return nativeSpendOfferClickedObserver;
	}

	private void addNativeSpendOffer(@NonNull NativeSpendOffer nativeSpendOffer) {
		try {
			if (Kin.addNativeOffer(nativeSpendOffer)) {
				showToast("Native offer added");
			}
		} catch (ClientException e) {
			e.printStackTrace();
			showToast("Could not add native offer");
		}
	}

	private void getPublicAddress() {
		try {
			publicAddress = Kin.getPublicAddress();
			int blueColor = ContextCompat.getColor(getApplicationContext(), R.color.sample_app_blue);
			publicAddressTextArea.getBackground().setColorFilter(blueColor, Mode.SRC_ATOP);
			showPublicAddressButton.setText(R.string.copy_public_address);
			publicAddressTextArea.setText(publicAddress);
		} catch (ClientException e) {
			e.printStackTrace();
		}
	}

	private void copyToClipboard(CharSequence textToCopy) {
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(
				Context.CLIPBOARD_SERVICE);
			clipboard.setText(textToCopy);
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(
				Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData.newPlainText("copied text", textToCopy);
			clipboard.setPrimaryClip(clip);
		}
		Toast.makeText(this, "Copied to your clipboard", Toast.LENGTH_SHORT).show();
	}

	private void getBalance() {
		try {
			//Get Cached Balance
			try {
				Balance cachedBalance = Kin.getCachedBalance();
				setBalanceWithAmount(cachedBalance);
			} catch (ClientException e) {
				e.printStackTrace();
			}

			Kin.getBalance(new KinCallback<Balance>() {
				@Override
				public void onResponse(Balance balance) {
					enableView(balanceView, true);
					setBalanceWithAmount(balance);
				}

				@Override
				public void onFailure(KinEcosystemException exception) {
					enableView(balanceView, true);
					setBalanceFailed();
				}
			});
		} catch (ClientException e) {
			setBalanceFailed();
			e.printStackTrace();
		}
	}

	private void setBalanceFailed() {
		balanceView.setText(R.string.failed_to_get_balance);
	}

	private void setBalanceWithAmount(Balance balance) {
		int balanceValue = balance.getAmount().intValue();
		balanceView.setText(getString(R.string.get_balance_d, balanceValue));
	}

	private void openKinMarketplace() {
		try {
			Kin.launchMarketplace(MainActivity.this);
		} catch (ClientException e) {
			e.printStackTrace();
			showToast("Failed - " + e.getMessage());
		}
	}

	private void createNativeSpendOffer() {
		String userID = SignInRepo.getUserId(getApplicationContext());
		String offerJwt = JwtUtil.generateSpendOfferExampleJWT(BuildConfig.SAMPLE_APP_ID, userID);
		Log.d(TAG, "createNativeSpendOffer: " + offerJwt);
		try {
			Kin.purchase(offerJwt, getNativeSpendOrderConfirmationCallback());
		} catch (ClientException e) {
			e.printStackTrace();
			showToast("Failed - " + e.getMessage());
			enableView(nativeSpendButton, true);
		}
	}

	private void createNativeEarnOffer() {
		String userID = SignInRepo.getUserId(getApplicationContext());
		String offerJwt = JwtUtil.generateEarnOfferExampleJWT(BuildConfig.SAMPLE_APP_ID, userID);
		try {
			Kin.requestPayment(offerJwt, getNativeEarnOrderConfirmationCallback());
		} catch (ClientException e) {
			e.printStackTrace();
			showToast("Failed - " + e.getMessage());
			enableView(nativeEarnButton, true);
		}
	}

	private void createPayToUserOffer() {
		String userID = SignInRepo.getUserId(getApplicationContext());
		String offerJwt = JwtUtil
			.generatePayToUserOfferExampleJWT(BuildConfig.SAMPLE_APP_ID, userID, payToUserRecipientUserId);
		try {
			Kin.payToUser(offerJwt, getNativePayToUserOrderConfirmationCallback());
		} catch (ClientException e) {
			e.printStackTrace();
			showToast("Failed - " + e.getMessage());
			enableView(payToUserButton, true);
		}
	}

	/**
	 * Use this method with the offerID you created, to get {@link OrderConfirmation}
	 */
	private void getOrderConfirmation(@NonNull final String offerID) {
		if (!TextUtils.isEmpty(offerID)) {
			try {
				Kin.getOrderConfirmation(offerID, new KinCallback<OrderConfirmation>() {
					@Override
					public void onResponse(OrderConfirmation orderConfirmation) {
						String msg = "Offer: " + offerID + " Status is: " + orderConfirmation.getStatus() + " jwt = "
							+ orderConfirmation.getJwtConfirmation();
						showToast(msg);
						Log.d(TAG, msg);
						enableView(getOrderConfirmationButton, true);
					}

					@Override
					public void onFailure(KinEcosystemException e) {
						Log.d(TAG, "getOrderConfirmation failure = " + Log.getStackTraceString(e));
						showToast("Failed to get OfferId: " + offerID + " status");
						enableView(getOrderConfirmationButton, true);
					}
				});
			} catch (ClientException e) {
				enableView(getOrderConfirmationButton, true);
				showToast("Failed - " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			showToast("No Order was sent in this session yet.");
			enableView(getOrderConfirmationButton, true);
		}
	}

	private KinCallback<OrderConfirmation> getNativeSpendOrderConfirmationCallback() {
		if (nativeSpendOrderConfirmationCallback == null) {
			nativeSpendOrderConfirmationCallback = new KinCallback<OrderConfirmation>() {
				@Override
				public void onResponse(OrderConfirmation orderConfirmation) {
					getBalance();
					showToast("Succeed to create native spend");
					Log.d(TAG, "Jwt confirmation: \n" + orderConfirmation.getJwtConfirmation());
					enableView(nativeSpendButton, true);
				}

				@Override
				public void onFailure(KinEcosystemException exception) {
					exception.printStackTrace();
					showToast("Failed - " + exception.getMessage());
					enableView(nativeSpendButton, true);
				}
			};
		}
		return nativeSpendOrderConfirmationCallback;
	}

	private KinCallback<OrderConfirmation> getNativePayToUserOrderConfirmationCallback() {
		if (nativePayToUserOrderConfirmationCallback == null) {
			nativePayToUserOrderConfirmationCallback = new KinCallback<OrderConfirmation>() {
				@Override
				public void onResponse(OrderConfirmation orderConfirmation) {
					getBalance();
					showToast("Succeed to create pay to user offer");
					Log.d(TAG, "Jwt confirmation: \n" + orderConfirmation.getJwtConfirmation());
					enableView(payToUserButton, true);
				}

				@Override
				public void onFailure(KinEcosystemException exception) {
					exception.printStackTrace();
					showToast("Failed - " + exception.getMessage());
					enableView(payToUserButton, true);
				}
			};
		}
		return nativePayToUserOrderConfirmationCallback;
	}

	private KinCallback<OrderConfirmation> getNativeEarnOrderConfirmationCallback() {
		if (nativeEarnOrderConfirmationCallback == null) {
			nativeEarnOrderConfirmationCallback = new KinCallback<OrderConfirmation>() {
				@Override
				public void onResponse(OrderConfirmation orderConfirmation) {
					getBalance();
					showToast("Succeed to create native earn");
					Log.d(TAG, "Jwt confirmation: \n" + orderConfirmation.getJwtConfirmation());
					enableView(nativeEarnButton, true);
				}

				@Override
				public void onFailure(KinEcosystemException exception) {
					exception.printStackTrace();
					showToast("Failed - " + exception.getMessage());
					enableView(nativeEarnButton, true);
				}
			};
		}
		return nativeEarnOrderConfirmationCallback;
	}

	private void enableView(View v, boolean enable) {
		v.setEnabled(enable);
		v.setClickable(enable);
		v.setAlpha(enable ? 1f : 0.5f);
	}

	private void showToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		nativeSpendOrderConfirmationCallback = null;
		nativeEarnOrderConfirmationCallback = null;
		try {
			Kin.removeNativeOffer(nativeSpendOffer);
			Kin.removeNativeOfferClickedObserver(nativeSpendOfferClickedObserver);
		} catch (ClientException e) {
			Log.d(TAG, "onDestroy: Failed to remove native offer clicked observer");
		}
	}
}
