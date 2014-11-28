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

    private static final String TAG = "NetworkStateChanged";
    private static final String[] SSID = {"MU-WiFi", "IC-WiFi"};

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        // Check preference
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(Preferences.KEY_ENABLED, false)) {
            // Disable the BroadcastReceiver so it isn't called in the future
            Utils.setEnableBroadcastReceiver(context, false);
            return;
        }

        // Check network connected
        NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        Log.d(TAG, "onReceive: netInfo = " + netInfo);
        if (!netInfo.isConnected()) {
            return;
        }

        // Check SSID
        String ssid = Utils.getSsid(context);
        Log.d(TAG, "ssid = " + ssid);
        boolean validSsid = false;
        for (String checkSsid : SSID) {
            if (checkSsid.equalsIgnoreCase(ssid) || ("\"" + checkSsid + "\"").equalsIgnoreCase(ssid)) {
                // JB 4.2 puts quote around SSID
                validSsid = true;
                break;
            }
        }
        if (!validSsid) {
            Log.d(TAG, "Invalid SSID");
            return;
        }

        Log.v(TAG, "Connected to the correct network");

        Intent i = new Intent(context, MuWifiLogin.class);
        context.startService(i);
    }

}
