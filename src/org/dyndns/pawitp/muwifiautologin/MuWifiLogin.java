package org.dyndns.pawitp.muwifiautologin;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

import javax.net.ssl.SSLException;

public class MuWifiLogin extends IntentService {

    static final String TAG = "MuWifiLogin";
    static final int LOGIN_ERROR_ID = 1;
    static final int LOGIN_ONGOING_ID = 2;
    static final String EXTRA_LOGOUT = "logout";

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
        Utils.loadLocale(this);

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

        boolean isLogout = intent.getBooleanExtra(EXTRA_LOGOUT, false);

        try {
            if (isLogout) {
                Log.v(TAG, "Logging out");
                updateOngoingNotification(getString(R.string.notify_logout_ongoing_text), true);

                // Currently, only ArubaClient supports logout
                ArubaClient loginClient = new ArubaClient();
                loginClient.logout();

                createToastNotification(R.string.logout_successful, Toast.LENGTH_SHORT);
                Log.v(TAG, "Logout successful");
            } else {
                updateOngoingNotification(getString(R.string.notify_login_ongoing_text_determine_requirement), true);
                if (loginRequired()) {
                    Log.v(TAG, "Login required");

                    // TOOO: Detect client
                    String username = mPrefs.getString(Preferences.KEY_USERNAME, null);
                    String password = mPrefs.getString(Preferences.KEY_PASSWORD, null);
                    ArubaClient loginClient = new ArubaClient(username, password);

                    updateOngoingNotification(getString(R.string.notify_login_ongoing_text_logging_in), true);
                    loginClient.login();

                    if (mPrefs.getBoolean(Preferences.KEY_TOAST_NOTIFY_SUCCESS, true)) {
                        createToastNotification(R.string.login_successful, Toast.LENGTH_SHORT);
                    }

                    Log.v(TAG, "Login successful");
                } else {
                    if (mPrefs.getBoolean(Preferences.KEY_TOAST_NOTIFY_NOT_REQUIRED, true)) {
                        createToastNotification(R.string.no_login_required, Toast.LENGTH_SHORT);
                    }

                    Log.v(TAG, "No login required");
                }
            }
        } catch (LoginException e) {
            Log.v(TAG, "Login failed: LoginException");
            Log.v(TAG, Utils.stackTraceToString(e));

            createRetryNotification(isLogout, e.getMessage());
        } catch (IOException e) {
            Log.v(TAG, "Login failed: IOException");
            Log.v(TAG, Utils.stackTraceToString(e));

            createRetryNotification(isLogout);
        } catch (NullPointerException e) {
            // a bug in HttpClient library
            // thrown when there is a connection failure when handling a redirect
            Log.v(TAG, "Login failed: NullPointerException");
            Log.v(TAG, Utils.stackTraceToString(e));

            createRetryNotification(isLogout);
        }
    }

    private void updateOngoingNotification(String message, boolean notify) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent() , 0);
        mNotification.setLatestEventInfo(this, getString(R.string.notify_login_ongoing_title), message, contentIntent);

        if (notify) {
            mNotifMan.notify(LOGIN_ONGOING_ID, mNotification);
        }
    }

    private void createRetryNotification(boolean isLogout, String text) {
        Intent notificationIntent = new Intent(this, MuWifiLogin.class);
        PendingIntent contentIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
        createErrorNotification(contentIntent, text, isLogout);
    }

    private void createRetryNotification(boolean isLogout) {
        createRetryNotification(isLogout, getString(R.string.notify_login_error_text));
    }

    private void createErrorNotification(PendingIntent contentIntent, String errorText, boolean isLogout) {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) {
            // Don't show errors if wifi is disabled
            return;
        }

        Notification notification = new Notification(R.drawable.ic_stat_notify_key, getString(R.string.ticker_login_error), System.currentTimeMillis());
        int message = isLogout ? R.string.notify_logout_error_title : R.string.notify_login_error_title;
        notification.setLatestEventInfo(this, getString(message), errorText, contentIntent);
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

    private boolean loginRequired() throws IOException {
        try {
            HttpGet httpget = new HttpGet("https://www.google.com/");
            Utils.createHttpClient().execute(httpget);
        }
        catch (SSLException e) {
            return true; // If login is required, the certificate sent will be securelogin.arubanetworks.com
        }
        return false;
    }

}
