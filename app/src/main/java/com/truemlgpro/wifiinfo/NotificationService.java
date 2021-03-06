package com.truemlgpro.wifiinfo;

import android.app.*;
import android.content.*;
import android.net.*;
import android.net.wifi.*;
import android.os.*;
import android.provider.*;
import android.support.annotation.*;

public class NotificationService extends Service
{
	
	private NotificationManager notificationManager;
	private Notification notification26_28;
	private Notification notification29;
	private Notification notification21_25;
	private Notification.Builder builder;
	
	private int visSigStrgNtfcColor;
	
	@Override
	public void onCreate()
	{
		handler.post(runnable);
		super.onCreate();
	}

	@Override
	public void onDestroy()
	{
		handler.removeCallbacks(runnable);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		/// Notification Button Receivers ///
		
		try {
			if (intent.getAction() != null && intent.getAction().equals("ACTION_NTFC_SETTINGS")) {
				startNtfcSettingsActivity();
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		
		/// END ///
		
		if (android.os.Build.VERSION.SDK_INT >= 26 && android.os.Build.VERSION.SDK_INT < 29) {
			/// Android 8 - Android 9 ///
			showNotificationAPI26_28();
			startForeground(1301, notification26_28);
		} else if (android.os.Build.VERSION.SDK_INT == 29) {
			/// Android 10 ///
			showNotificationAPI29();
			startForeground(1302, notification29);
		} else if (android.os.Build.VERSION.SDK_INT < 26) {
			/// Android 5 - Android 7 ///
			showNotificationAPI21_25();
			startForeground(1303, notification21_25);
		}
		
		return START_STICKY;
	}
	
	/// Handler for Notification Updates ///
	
	private Handler handler = new Handler(Looper.getMainLooper());
	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			String keyNtfcFreq = new SharedPreferencesManager(getApplicationContext()).retrieveString(SettingsActivity.KEY_PREF_NTFC_FREQ, MainActivity.ntfcUpdateInterval);
			int keyNtfcFreqFormatted = Integer.parseInt(keyNtfcFreq);
			
			if (android.os.Build.VERSION.SDK_INT == 29) {
				showNotificationAPI29();
			}
			
			if (android.os.Build.VERSION.SDK_INT < 26) {
				showNotificationAPI21_25();
			}
			
			if (android.os.Build.VERSION.SDK_INT >= 26 && android.os.Build.VERSION.SDK_INT < 29) {
				showNotificationAPI26_28();
			}
			handler.postDelayed(runnable, keyNtfcFreqFormatted);
		}
	};
	
	/// END ///
	
	/// Notification Settings Activity ///
	
	public void startNtfcSettingsActivity() {
		Intent Ntfc_Intent = new Intent();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Ntfc_Intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
			Ntfc_Intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
			Ntfc_Intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			Ntfc_Intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
			Ntfc_Intent.putExtra("app_package", getPackageName());
			Ntfc_Intent.putExtra("app_uid", getApplicationInfo().uid);
			Ntfc_Intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		startActivity(Ntfc_Intent);
	}
	
	/// END ///

	/// ANDROID 8 - ANDROID 9 ///

	public void showNotificationAPI26_28() {
		ConnectivityManager CM = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo WiFiCheck = CM.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		int NOTIFICATION_ID = 1301;

		Intent NotificationIntent = new Intent(this, MainActivity.class);
		PendingIntent content_intent = PendingIntent.getActivity(this, 0, NotificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		Intent intentActionStop = new Intent(this, ActionButtonReceiver.class);
		intentActionStop.setAction("ACTION_STOP");
		PendingIntent pIntentActionStop = PendingIntent.getBroadcast(this, 0, intentActionStop, PendingIntent.FLAG_ONE_SHOT);

		Intent intentActionSettings = new Intent(this, ActionButtonReceiver.class);
		intentActionSettings.setAction("ACTION_NTFC_SETTINGS");
		PendingIntent pIntentActionSettings = PendingIntent.getBroadcast(this, 0, intentActionSettings, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		String channelID = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
		builder = new Notification.Builder(this, channelID);
		
		Boolean keyNtfcClr = new SharedPreferencesManager(getApplicationContext()).retrieveBoolean(SettingsActivity.KEY_PREF_CLR_CHECK, MainActivity.colorizeNtfc);
		Boolean keyVisSigStrgNtfc = new SharedPreferencesManager(getApplicationContext()).retrieveBoolean(SettingsActivity.KEY_PREF_VIS_SIG_STRG_CHECK, MainActivity.visualizeSigStrg);

		WifiManager mainWifi;
		mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = mainWifi.getConnectionInfo();
		String ssid = wInfo.getSSID();
		if (ssid.equals("<unknown ssid>")) {
			ssid = "N/A";
		}
		String bssid;
		if (wInfo.getBSSID() != null) {
			bssid = wInfo.getBSSID().toUpperCase();
		} else {
			bssid = "N/A";
		}
		int rssi = wInfo.getRssi();
		int RSSIconv = mainWifi.calculateSignalLevel(rssi, 101);
		if (keyVisSigStrgNtfc) {
			if (RSSIconv >= 75) {
				visSigStrgNtfcColor = getResources().getColor(R.color.ntfcColorSignalHigh);
			} else if (RSSIconv >= 50 && RSSIconv < 75) {
				visSigStrgNtfcColor = getResources().getColor(R.color.ntfcColorSignalAvg);
			} else if (RSSIconv >= 1 && RSSIconv < 50) {
				visSigStrgNtfcColor = getResources().getColor(R.color.ntfcColorSignalLow);
			}
		} else {
			visSigStrgNtfcColor = getResources().getColor(R.color.ntfcColor);
		}
		int freq = wInfo.getFrequency();
		int channel = convertFrequencyToChannel(freq);
		int networkSpeed = wInfo.getLinkSpeed();
		int ipAddress = wInfo.getIpAddress();
		int network_id = wInfo.getNetworkId();
		String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
		String smallInfo = "SSID: " + ssid + " | " + "Signal Strength: " + RSSIconv + "%" + " (" + rssi + "dBm" + ")";
		String extendedInfo = "SSID: " + ssid + "\n" + "BSSID: " + bssid + "\n" + "Signal Strength: " + RSSIconv + "%" + " (" + rssi + "dBm" + ")" + "\n" + 
			"Frequency: " + freq + "MHz" + "\n" + "Channel: " + channel + "\n" + "Network Speed: " + networkSpeed + "MB/s" + "\n" + "ID: " + network_id;

		notification26_28 = builder.setContentIntent(content_intent)
			.setSmallIcon(R.drawable.ic_wifi)
			.setContentTitle("Local IP: " + ip)
			.setContentText(smallInfo)
			.setWhen(System.currentTimeMillis())
			.addAction(R.drawable.ic_stop, "Stop Services", pIntentActionStop)
			.addAction(R.drawable.ic_settings_light, "Notification Settings", pIntentActionSettings)
			.setChannelId(channelID)
			.setColorized(keyNtfcClr)
			.setColor(visSigStrgNtfcColor)
			.setCategory(Notification.CATEGORY_SERVICE)
			.setStyle(new Notification.BigTextStyle().bigText(extendedInfo))
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.setAutoCancel(false)
			.build();

			notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notificationManager.notify(NOTIFICATION_ID, notification26_28);
	}

	/// ANDROID 10 ///

	public void showNotificationAPI29() {
		ConnectivityManager CM = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo WiFiCheck = CM.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		int NOTIFICATION_ID = 1302;

		Intent NotificationIntent = new Intent(this, MainActivity.class);
		PendingIntent content_intent = PendingIntent.getActivity(this, 0, NotificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		Intent intentActionStop = new Intent(this, ActionButtonReceiver.class);
		intentActionStop.setAction("ACTION_STOP");
		PendingIntent pIntentActionStop = PendingIntent.getBroadcast(this, 0, intentActionStop, PendingIntent.FLAG_ONE_SHOT);

		Intent intentActionSettings = new Intent(this, ActionButtonReceiver.class);
		intentActionSettings.setAction("ACTION_NTFC_SETTINGS");
		PendingIntent pIntentActionSettings = PendingIntent.getBroadcast(this, 0, intentActionSettings, PendingIntent.FLAG_UPDATE_CURRENT);
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String channelID = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
		builder = new Notification.Builder(this, channelID);
		
		Boolean keyNtfcClr = new SharedPreferencesManager(getApplicationContext()).retrieveBoolean(SettingsActivity.KEY_PREF_CLR_CHECK, MainActivity.colorizeNtfc);
		Boolean keyVisSigStrgNtfc = new SharedPreferencesManager(getApplicationContext()).retrieveBoolean(SettingsActivity.KEY_PREF_VIS_SIG_STRG_CHECK, MainActivity.visualizeSigStrg);
		
		WifiManager mainWifi;
		mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = mainWifi.getConnectionInfo();
		String ssid = wInfo.getSSID();
		if (ssid.equals("<unknown ssid>")) {
			ssid = "N/A";
		}
		int rssi = wInfo.getRssi();
		int RSSIconv = mainWifi.calculateSignalLevel(rssi, 101);
		if (keyVisSigStrgNtfc) {
			if (RSSIconv >= 75) {
				visSigStrgNtfcColor = getResources().getColor(R.color.ntfcColorSignalHigh);
			} else if (RSSIconv >= 50 && RSSIconv < 75) {
				visSigStrgNtfcColor = getResources().getColor(R.color.ntfcColorSignalAvg);
			} else if (RSSIconv >= 1 && RSSIconv < 50) {
				visSigStrgNtfcColor = getResources().getColor(R.color.ntfcColorSignalLow);
			}
		} else {
			visSigStrgNtfcColor = getResources().getColor(R.color.ntfcColor);
		}
		int ipAddress = wInfo.getIpAddress();
		int freq = wInfo.getFrequency();
		int channel = convertFrequencyToChannel(freq);
		String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
		String info = "SSID: " + ssid + " | " + "Signal Strength: " + RSSIconv + "%" + " (" + rssi + "dBm" + ")" + " |" + "\n" + freq + " MHz " + "(Ch: " + channel + ")";
			
		notification29 = builder.setContentIntent(content_intent)
			.setSmallIcon(R.drawable.ic_wifi)
			.setContentTitle("Local IP: " + ip)
			.setContentText(info)
			.setWhen(System.currentTimeMillis())
			.addAction(R.drawable.ic_stop, "Stop Services", pIntentActionStop)
			.addAction(R.drawable.ic_settings_light, "Notification Settings", pIntentActionSettings)
			.setChannelId(channelID)
			.setColorized(keyNtfcClr)
			.setColor(visSigStrgNtfcColor)
			.setCategory(Notification.CATEGORY_SERVICE)
			.setStyle(new Notification.BigTextStyle().bigText(info))
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.setAutoCancel(false)
			.build();

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, notification29);
	}

	/// ANDROID 5 - ANDROID 7 ///

	public void showNotificationAPI21_25() {
		ConnectivityManager CM = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo WiFiCheck = CM.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		int NOTIFICATION_ID = 1303;

		Intent NotificationIntent = new Intent(this, MainActivity.class);
		PendingIntent content_intent = PendingIntent.getActivity(this, 0, NotificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		Intent intentActionStop = new Intent(this, ActionButtonReceiver.class);
		intentActionStop.setAction("ACTION_STOP");
		PendingIntent pIntentActionStop = PendingIntent.getBroadcast(this, 0, intentActionStop, PendingIntent.FLAG_ONE_SHOT);

		Intent intentActionSettings = new Intent(this, ActionButtonReceiver.class);
		intentActionSettings.setAction("ACTION_NTFC_SETTINGS");
		PendingIntent pIntentActionSettings = PendingIntent.getBroadcast(this, 0, intentActionSettings, PendingIntent.FLAG_UPDATE_CURRENT);
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		builder = new Notification.Builder(this);

		WifiManager mainWifi;
		mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = mainWifi.getConnectionInfo();
		String ssid = wInfo.getSSID();
		if (ssid.equals("<unknown ssid>")) {
			ssid = "N/A";
		}
		String bssid;
		if (wInfo.getBSSID() != null) {
			bssid = wInfo.getBSSID().toUpperCase();
		} else {
			bssid = "N/A";
		}
		int rssi = wInfo.getRssi();
		int RSSIconv = mainWifi.calculateSignalLevel(rssi, 101);
		int freq = wInfo.getFrequency();
		int channel = convertFrequencyToChannel(freq);
		int networkSpeed = wInfo.getLinkSpeed();
		int ipAddress = wInfo.getIpAddress();
		int network_id = wInfo.getNetworkId();
		String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
		String smallInfo = "SSID: " + ssid + " | " + "Signal Strength: " + RSSIconv + "%" + " (" + rssi + "dBm" + ")";
		String extendedInfo = "SSID: " + ssid + "\n" + "BSSID: " + bssid + "\n" + "Signal Strength: " + RSSIconv + "%" + " (" + rssi + "dBm" + ")" + "\n" + 
			"Frequency: " + freq + "MHz" + "\n" + "Channel: " + channel + "\n" + "Network Speed: " + networkSpeed + "MB/s" + "\n" + "ID: " + network_id;

		notification21_25 = builder.setContentIntent(content_intent)
			.setSmallIcon(R.drawable.ic_wifi)
			.setContentTitle("Local IP: " + ip)
			.setContentText(smallInfo)
			.setWhen(System.currentTimeMillis())
			.addAction(R.drawable.ic_stop, "Stop Services", pIntentActionStop)
			.addAction(R.drawable.ic_settings_light, "Notification Settings", pIntentActionSettings)
			.setPriority(Notification.PRIORITY_LOW)
			.setColor(getResources().getColor(R.color.ntfcColor))
			.setCategory(Notification.CATEGORY_SERVICE)
			.setStyle(new Notification.BigTextStyle().bigText(extendedInfo))
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.setAutoCancel(false)
			.build();

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, notification21_25);
	}
	
	/// END ///
	
	public static int convertFrequencyToChannel(int freq) {
		if (freq >= 2412 && freq <= 2484) {
			return (freq - 2412) / 5 + 1;
		} else if (freq >= 5170 && freq <= 5825) {
			return (freq - 5170) / 5 + 34;
		} else {
			return -1;
		}
	}
	
	@RequiresApi(Build.VERSION_CODES.O)
	private String createNotificationChannel(NotificationManager notificationManager) {
		String channelID = "wifi_info";
		CharSequence channelName = "Notification Service";
		NotificationChannel channel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_LOW);
		channel.setDescription("Main Wi-Fi Info Notification");
		channel.setShowBadge(false);
		notificationManager.createNotificationChannel(channel);
		return channelID;
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
		
}
