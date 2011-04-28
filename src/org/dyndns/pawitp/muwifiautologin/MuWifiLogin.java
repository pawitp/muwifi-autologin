package org.dyndns.pawitp.muwifiautologin;

import java.io.IOException;
import java.net.SocketTimeoutException;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class MuWifiLogin extends IntentService {

	static final String TAG = "MuWifiLogin";
	static final int LOGIN_ERROR_ID = 1;
	static final int LOGIN_ONGOING_ID = 2;
	
	private Handler mHandler;
	private SharedPreferences mPrefs;
	private NotificationManager mNotifMan;
	private Notification mNotification;

	public MuWifiLogin() {
		super(TAG);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mHandler = new Handler();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mNotifMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		mNotification = new Notification(R.drawable.ic_stat_notify_key, null, System.currentTimeMillis());
		updateOngoingNotification(null, false); // Foreground service requires a valid notification
		startForeground(LOGIN_ONGOING_ID, mNotification); // Stopped automatically when onHandleIntent returns
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		mNotifMan.cancel(LOGIN_ERROR_ID); // clear any old notification
		
		MuWifiClient loginClient = new MuWifiClient(mPrefs.getString(Preferences.KEY_USERNAME, null), mPrefs.getString(Preferences.KEY_PASSWORD, null));
		
		try {
			updateOngoingNotification(getString(R.string.notify_login_ongoing_text_determine_requirement), true);
			if (loginClient.loginRequired()) {
				try {
					Log.v(TAG, "Login required");
					
					updateOngoingNotification(getString(R.string.notify_login_ongoing_text_logging_in), true);
					loginClient.login();
					createToastNotification(R.string.login_successful, Toast.LENGTH_SHORT);
					
					Log.v(TAG, "Login successful");
				} catch (SocketTimeoutException e) {
					// A socket timeout here means invalid crendentials!
					Log.v(TAG, "Invalid credentials");
					
					Intent notificationIntent = new Intent(this, Preferences.class);
					createErrorNotification(notificationIntent, getString(R.string.notify_login_error_invalid_credentials_text));
				}
			} else {
				createToastNotification(R.string.no_login_required, Toast.LENGTH_SHORT);
				
				Log.v(TAG, "No login required");
			}
		} catch (LoginException e) {
			Log.v(TAG, "Login failed");
			
			Intent notificationIntent = new Intent(this, ErrorWebView.class);
			notificationIntent.putExtra(ErrorWebView.EXTRA_CONTENT, e.getMessage());
			createErrorNotification(notificationIntent, getString(R.string.notify_login_error_text));
		} catch (IOException e) {
			Log.v(TAG, "Login failed: IOException");
			
			Intent notificationIntent = new Intent(this, IOErrorView.class);
			notificationIntent.putExtra(IOErrorView.EXTRA_CONTENT, Utils.stackTraceToString(e));
			createErrorNotification(notificationIntent, getString(R.string.notify_login_error_text));
		} catch (NullPointerException e) {
			// a bug in HttpClient library
			// thrown when there is a connection failure when handling a redirect
			Log.v(TAG, "Login failed: NullPointerException");
			Log.v(TAG, Utils.stackTraceToString(e));
			
			createErrorNotification(new Intent(), getString(R.string.notify_login_error_null_exception_text));
		}
	}
	
	private void updateOngoingNotification(String message, boolean notify) {
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent() , 0);
		mNotification.setLatestEventInfo(this, getString(R.string.notify_login_ongoing_title), message, contentIntent);
		
		if (notify) {
			mNotifMan.notify(LOGIN_ONGOING_ID, mNotification);
		}
	}
	
	private void createErrorNotification(Intent notificationIntent, String errorText) {
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		Notification notification = new Notification(R.drawable.ic_stat_notify_key, getString(R.string.ticker_login_error), System.currentTimeMillis());
		notification.setLatestEventInfo(this, getString(R.string.notify_login_error_title), errorText, contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		
		if (mPrefs.getBoolean(Preferences.KEY_ERROR_NOTIFY_SOUND, false)) {
			notification.defaults |= Notification.DEFAULT_SOUND;
		}
		if (mPrefs.getBoolean(Preferences.KEY_ERROR_NOTIFY_VIBRATE, false)) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		if (mPrefs.getBoolean(Preferences.KEY_ERROR_NOTIFY_LIGHTS, false)) {
			notification.defaults |= Notification.DEFAULT_LIGHTS;
		}
		
		mNotifMan.notify(LOGIN_ERROR_ID, notification);
	}
	
	private void createToastNotification(final int message, final int length) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MuWifiLogin.this, message, length).show();
			}
		});
	}
}
