package org.dyndns.pawitp.muwifiautologin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class NetworkStateChanged extends BroadcastReceiver {

	static final String TAG = "NetworkStateChanged";
	static final String SSID = "MU-WiFi";
	static final String REDIRECT_PAGE_PATTERN = "TODO";
	static final String FORM_USERNAME = "TODO";
	static final String FORM_PASSWORD = "TODO";
	static final String FORM_URL = "TODO";
	
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
		
		try {
			if (loginRequired()) {
				Log.v(TAG, "Login required");
				login();
				if (!loginRequired()) {
					Toast.makeText(mContext, R.string.login_successful, Toast.LENGTH_SHORT).show();
					
					Log.v(TAG, "Login successful");
				} else {
					// TODO Notification
					Log.v(TAG, "Login failed");
				}
			} else {
				Log.v(TAG, "No login required");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	}
	
	private boolean loginRequired() throws IOException {
		// TODO Timeout
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://www.google.com/");
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		InputStream is = entity.getContent();
		Scanner scanner = new Scanner(is);
		String found = scanner.findWithinHorizon(REDIRECT_PAGE_PATTERN, 0);
		scanner.close();
		if (found == null) {
			return false;
		} else {
			return true;
		}
	}
	
	private void login() throws IOException {
		// TODO timeout
		HttpClient httpclient = new DefaultHttpClient();
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair(FORM_USERNAME, mPrefs.getString(Preferences.KEY_USERNAME, null)));
		formparams.add(new BasicNameValuePair(FORM_PASSWORD, mPrefs.getString(Preferences.KEY_PASSWORD, null)));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
		HttpPost httppost = new HttpPost(FORM_URL);
		httppost.setEntity(entity);
		httpclient.execute(httppost);
		// TODO deal with response
	}
}
