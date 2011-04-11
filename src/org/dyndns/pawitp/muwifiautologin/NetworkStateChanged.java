package org.dyndns.pawitp.muwifiautologin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class NetworkStateChanged extends BroadcastReceiver {

	public static String TAG = "NetworkStateChanged";
	public static String SSID = "MU-WiFi";
	
	private SharedPreferences mPrefs;
	private Context mContext;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		
		// Check network connected
		NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		if (!netInfo.isConnected()) {
			return;
		}
		
		// Check SSID
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (!wifi.getConnectionInfo().getSSID().equalsIgnoreCase(SSID)) {
			return;
		}
		
		// Check preference
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!mPrefs.getBoolean(Preferences.KEY_ENABLED, false)) {
			return;
		}
		
		Log.v(TAG, "Connected to the correct network");
	}
}
