package com.truemlgpro.wifiinfo;

import android.content.*;
import android.net.*;
import android.os.*;
import android.support.design.widget.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.lang.ref.*;
import java.net.*;
import me.anwarshahriar.calligrapher.*;

import android.support.v7.widget.Toolbar;

public class URLtoIPActivity extends AppCompatActivity
{

	private static final int MIN_TEXT_LENGTH = 4;
    private static final String EMPTY_STRING = "";

    private TextInputLayout mTextInputLayout;
    private static EditText mEditText;
	private static TextView textview_ipFromURL;
	private TextView textview_nonetworkconn;
	private Button convert_button;
	private LinearLayout layout_url_to_ip_results;
	private static ScrollView url_to_ip_scroll;
	private Toolbar toolbar;
	
	private ConnectivityManager CM;
	private NetworkInfo WiFiCheck;
	private NetworkInfo CellularCheck;
	
	public Boolean wifi_connected;
	public Boolean cellular_connected;
	
	private BroadcastReceiver NetworkConnectivityReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
	{
		Boolean keyTheme = new SharedPreferencesManager(getApplicationContext()).retrieveBoolean(SettingsActivity.KEY_PREF_SWITCH, MainActivity.darkMode);
		Boolean keyAmoledTheme = new SharedPreferencesManager(getApplicationContext()).retrieveBoolean(SettingsActivity.KEY_PREF_AMOLED_CHECK, MainActivity.amoledMode);

		if (keyTheme == true) {
			setTheme(R.style.DarkTheme);
		}

		if (keyAmoledTheme == true) {
			if (keyTheme == true) {
				setTheme(R.style.AmoledDarkTheme);
			}
		}

		if (keyTheme == false) {
			setTheme(R.style.LightTheme);
		}

        super.onCreate(savedInstanceState);
        setContentView(R.layout.url_to_ip_activity);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
        mTextInputLayout = (TextInputLayout) findViewById(R.id.input_layout);
        mEditText = (EditText) findViewById(R.id.edittext_main);
		convert_button = (Button) findViewById(R.id.convert_button);
		textview_ipFromURL = (TextView) findViewById(R.id.textview_ipFromURL);
		layout_url_to_ip_results = (LinearLayout) findViewById(R.id.layout_url_to_ip_results);
		url_to_ip_scroll = (ScrollView) findViewById(R.id.url_to_ip_scroll);
		textview_nonetworkconn = (TextView) findViewById(R.id.textview_nonetworkconn);

		mEditText.setOnEditorActionListener(ActionListener.newInstance(this));

		getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		Calligrapher calligrapher = new Calligrapher(this);
		String font = new SharedPreferencesManager(getApplicationContext()).retrieveString(SettingsActivity.KEY_PREF_APP_FONT, MainActivity.appFont);
		calligrapher.setFont(this, font, true);

		setSupportActionBar(toolbar);
		final ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setDisplayShowHomeEnabled(true);
		actionbar.setElevation(20);

		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// Back button pressed
					finish();
				}
			});

		convert_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!shouldShowError()) {
						String url = mEditText.getText().toString();
						try {
							String ip = URLandIPConverter.convertUrl("https://" + url);
							appendResultsText("Converting URL: " + url);
							appendResultsText("IP: " + ip);
						} catch (MalformedURLException e) {
							e.printStackTrace();
							appendResultsText("Converting URL: " + url);
							appendResultsText("Error: Malformed URL");
						} catch (UnknownHostException e) {
							e.printStackTrace();
							appendResultsText("Converting URL: " + url);
							appendResultsText("Error: Unknown Host");
						}
						hideError();
					} else {
						showError();
					}
				}
			});
    }

    private boolean shouldShowError() {
        int textLength = mEditText.getText().length();
        return textLength >= 0 && textLength < MIN_TEXT_LENGTH;
    }

    private void showError() {
        mTextInputLayout.setError("Field is too short...");
    }

    private void hideError() {
        mTextInputLayout.setError(EMPTY_STRING);
    }

    private static final class ActionListener implements TextView.OnEditorActionListener {
        private final WeakReference<URLtoIPActivity> urlToIPWeakReference;

        public static ActionListener newInstance(URLtoIPActivity urlToIPActivity) {
            WeakReference<URLtoIPActivity> urlToIPWeakReference = new WeakReference<>(urlToIPActivity);
            return new ActionListener(urlToIPWeakReference);
        }

        private ActionListener(WeakReference<URLtoIPActivity> urlToIPWeakReference) {
            this.urlToIPWeakReference = urlToIPWeakReference;
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            URLtoIPActivity urlToIPActivity = urlToIPWeakReference.get();
            if (urlToIPActivity != null) {
                if (actionId == EditorInfo.IME_ACTION_GO && urlToIPActivity.shouldShowError()) {
                    urlToIPActivity.showError();
                } else {
                    urlToIPActivity.hideError();
					try {
						String url = mEditText.getText().toString();
						try {
							String ip = URLandIPConverter.convertUrl("https://" + url);
							textview_ipFromURL.append("Converting URL: " + url + "\n");
							textview_ipFromURL.append("IP: " + ip + "\n");
							url_to_ip_scroll.post(new Runnable() {
								@Override
								public void run() {
									url_to_ip_scroll.fullScroll(View.FOCUS_DOWN);
								}
							});
						} catch (MalformedURLException e) {
							e.printStackTrace();
							textview_ipFromURL.append("Converting URL: " + url + "\n");
							textview_ipFromURL.append("Error: Malformed URL" + "\n");
							url_to_ip_scroll.post(new Runnable() {
								@Override
								public void run() {
									url_to_ip_scroll.fullScroll(View.FOCUS_DOWN);
								}
							});
						}
					} catch (UnknownHostException e) {
						String url = mEditText.getText().toString();
						e.printStackTrace();
						textview_ipFromURL.append("Converting URL: " + url + "\n");
						textview_ipFromURL.append("Error: Unknown Host" + "\n");
						url_to_ip_scroll.post(new Runnable() {
							@Override
							public void run() {
								url_to_ip_scroll.fullScroll(View.FOCUS_DOWN);
							}
						});
					}
                }
            }
            return true;
        }
    }
	
	class NetworkConnectivityReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			checkNetworkConnectivity();
		}
	}

	public void checkNetworkConnectivity() {
		CM = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		WiFiCheck = CM.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		CellularCheck = CM.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		// WI-FI Connectivity Check

		if (WiFiCheck.isConnected() && !CellularCheck.isConnected()) {
			showWidgets();
			textview_ipFromURL.setText("...\n");
			mEditText.setText("");
			wifi_connected = true;
			cellular_connected = false;
		} else if (!WiFiCheck.isConnected() && !CellularCheck.isConnected()) {
			textview_ipFromURL.setText("...\n");
			mEditText.setText("");
			hideWidgets();
			wifi_connected = false;
			cellular_connected = false;
		}

		// Cellular Connectivity Check

		if (CellularCheck.isConnected() && !WiFiCheck.isConnected()) {
			showWidgets();
			textview_ipFromURL.setText("...\n");
			mEditText.setText("");
			wifi_connected = false;
			cellular_connected = true;
		} else if (!CellularCheck.isConnected() && !WiFiCheck.isConnected()) {
			textview_ipFromURL.setText("...\n");
			mEditText.setText("");
			hideWidgets();
			wifi_connected = false;
			cellular_connected = false;
		}
	}

	public void showWidgets() {
		textview_ipFromURL.setVisibility(View.VISIBLE);
		layout_url_to_ip_results.setVisibility(View.VISIBLE);
		mTextInputLayout.setVisibility(View.VISIBLE);
		mEditText.setVisibility(View.VISIBLE);
		convert_button.setVisibility(View.VISIBLE);
		textview_nonetworkconn.setVisibility(View.GONE);
	}

	public void hideWidgets() {
		textview_ipFromURL.setVisibility(View.GONE);
		layout_url_to_ip_results.setVisibility(View.GONE);
		mTextInputLayout.setVisibility(View.GONE);
		mEditText.setVisibility(View.GONE);
		convert_button.setVisibility(View.GONE);
		textview_nonetworkconn.setVisibility(View.VISIBLE);
	}
	
	private void appendResultsText(final String text) {
        runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textview_ipFromURL.append(text + "\n");
				url_to_ip_scroll.post(new Runnable() {
					@Override
					public void run() {
						url_to_ip_scroll.fullScroll(View.FOCUS_DOWN);
					}
				});
			}
		});
    }

	@Override
	protected void onStart()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		NetworkConnectivityReceiver = new NetworkConnectivityReceiver();
		registerReceiver(NetworkConnectivityReceiver, filter);
		super.onStart();
	}

	@Override
	protected void onStop()
	{
		unregisterReceiver(NetworkConnectivityReceiver);
		super.onStop();
	}

}
