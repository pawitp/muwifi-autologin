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
import org.apache.http.util.EntityUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
	static final String LOGIN_SUCCESSFUL_PATTERN = "TODO"; // not regex
	static final String FORM_USERNAME = "TODO";
	static final String FORM_PASSWORD = "TODO";
	static final String FORM_URL = "TODO";
	static final int LOGIN_ERROR_ID = 1;
	
	private SharedPreferences mPrefs;
	private HttpClient mHttpClient;
	
	@Override
	public void onReceive(Context context, Intent intent) {
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
		
		mHttpClient = new DefaultHttpClient();
		
		try {
			if (loginRequired()) {
				Log.v(TAG, "Login required");
				
				login();
				Toast.makeText(context, R.string.login_successful, Toast.LENGTH_SHORT).show();
				Log.v(TAG, "Login successful");
			} else {
				Log.v(TAG, "No login required");
			}
		} catch (LoginException e) {
			Log.v(TAG, "Login failed");
			
			NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			
			Intent notificationIntent = new Intent(context, ErrorWebView.class);
			notificationIntent.setAction(ErrorWebView.class.getName());
			notificationIntent.putExtra(ErrorWebView.EXTRA_CONTENT, e.getMessage());
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
			
			// TODO better icon
			Notification notification = new Notification(android.R.drawable.stat_sys_warning, context.getString(R.string.ticker_login_error), System.currentTimeMillis());
			notification.setLatestEventInfo(context, context.getString(R.string.notification_login_error_title), context.getString(R.string.notification_login_error_text), contentIntent);
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			
			notifMan.notify(LOGIN_ERROR_ID, notification);
		} catch (IOException e) {
			Log.v(TAG, "Login failed: IOException");
			
			NotificationManager notifMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			
			Intent notificationIntent = new Intent(context, ErrorTextView.class);
			notificationIntent.setAction(ErrorTextView.class.getName());
			notificationIntent.putExtra(ErrorTextView.EXTRA_CONTENT, e.toString());
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
			
			// TODO better icon
			Notification notification = new Notification(android.R.drawable.stat_sys_warning, context.getString(R.string.ticker_login_error), System.currentTimeMillis());
			notification.setLatestEventInfo(context, context.getString(R.string.notification_login_error_title), context.getString(R.string.notification_login_error_text), contentIntent);
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			
			notifMan.notify(LOGIN_ERROR_ID, notification);
		}
	}
	
	private boolean loginRequired() throws IOException {
		// TODO Timeout
		HttpGet httpget = new HttpGet("http://www.google.com/");
		HttpResponse response = mHttpClient.execute(httpget);
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
	
	private void login() throws IOException, LoginException {
		// TODO timeout
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair(FORM_USERNAME, mPrefs.getString(Preferences.KEY_USERNAME, null)));
		formparams.add(new BasicNameValuePair(FORM_PASSWORD, mPrefs.getString(Preferences.KEY_PASSWORD, null)));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
		HttpPost httppost = new HttpPost(FORM_URL);
		httppost.setEntity(entity);
		HttpResponse response = mHttpClient.execute(httppost);
		String strRes = EntityUtils.toString(response.getEntity());
		
		if (strRes.contains(LOGIN_SUCCESSFUL_PATTERN)) {
			// login successful
		} else {
			throw new LoginException(strRes);
		}
	}
}
