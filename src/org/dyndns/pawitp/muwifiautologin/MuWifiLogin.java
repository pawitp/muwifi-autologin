package org.dyndns.pawitp.muwifiautologin;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class MuWifiLogin extends IntentService {

    static final String TAG = "MuWifiLogin";
    static final int LOGIN_ERROR_ID = 1;
    static final int LOGIN_ONGOING_ID = 2;
    static final String EXTRA_LOGOUT = "logout";
    static final String IC_WIFI_SSID = "IC-WiFi";

    static final int NETWORK_TIMEOUT = 3000;

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
        updateOngoingNotification(getString(R.string.notify_request_wifi_ongoing_text), false); // Foreground service requires a valid notification
        startForeground(LOGIN_ONGOING_ID, mNotification); // Stopped automatically when onHandleIntent returns
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mNotifMan.cancel(LOGIN_ERROR_ID); // clear any old notification

        boolean isLogout = intent.getBooleanExtra(EXTRA_LOGOUT, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // For Lollipop+, we need to request the Wi-Fi network since
            // connections will go over mobile data by default if a captive
            // portal is detected
            Log.v(TAG, "Requesting Wi-Fi network");

            if (!requestNetwork()) {
                Log.e(TAG, "Unable to request Wi-Fi network");
                createRetryNotification(isLogout, getString(R.string.notify_request_wifi_error_text));
                return;
            }
        }

        doLogin(isLogout);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean requestNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        for (Network net : cm.getAllNetworks()) {
            if (cm.getNetworkInfo(net).getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(TAG, "Set network to " + net);
                ConnectivityManager.setProcessDefaultNetwork(net);
                return true;
            }
        }

        return false;
    }

    private void doLogin(boolean isLogout) {
        try {
            if (isLogout) {
                Log.v(TAG, "Logging out");
                updateOngoingNotification(getString(R.string.notify_logout_ongoing_text), true);

                LoginClient loginClient = getLogoutClient();
                loginClient.logout();

                createToastNotification(R.string.logout_successful, Toast.LENGTH_SHORT);
                Log.v(TAG, "Logout successful");
            } else {
                updateOngoingNotification(getString(R.string.notify_login_ongoing_text_determine_requirement), true);
                LoginClient loginClient = getLoginClient();
                if (loginClient != null) {
                    Log.v(TAG, "Login required");

                    String username = mPrefs.getString(Preferences.KEY_USERNAME, null);
                    String password = mPrefs.getString(Preferences.KEY_PASSWORD, null);

                    updateOngoingNotification(getString(R.string.notify_login_ongoing_text_logging_in), true);
                    loginClient.login(username, password);

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
            Log.e(TAG, "Login failed: LoginException", e);

            createRetryNotification(isLogout, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Login failed: IOException", e);

            tryConnection(isLogout);
        } catch (NullPointerException e) {
            // a bug in HttpClient library
            // thrown when there is a connection failure when handling a redirect
            Log.e(TAG, "Login failed: NullPointerException", e);

            tryConnection(isLogout);
        }
    }

    // After a login "failure", we can check if our connection is working or not
    private void tryConnection(boolean isLogout) {
        Log.v(TAG, "Trying connection after failure");
        try {
            boolean loggedIn = getLoginClient() == null;
            if ((!loggedIn && !isLogout) || (loggedIn && isLogout)) {
                createRetryNotification(isLogout);
            }
            else {
                Log.v(TAG, "Connection working");
            }
        }
        catch (Exception e) {
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
        notificationIntent.putExtra(EXTRA_LOGOUT, isLogout);
        PendingIntent contentIntent = PendingIntent.getService(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        createErrorNotification(contentIntent, text, isLogout);
    }

    private void createRetryNotification(boolean isLogout) {
        createRetryNotification(isLogout, getString(R.string.notify_login_error_text));
    }

    private void createErrorNotification(PendingIntent contentIntent, String errorText, boolean isLogout) {
        Log.d(TAG, "createErrorNotification isLogout=" + isLogout);

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) {
            // Don't show errors if wifi is disabled
            return;
        }

        int message = isLogout ? R.string.notify_logout_error_title : R.string.notify_login_error_title;
        Notification notification = new Notification(R.drawable.ic_stat_notify_key, getString(message), System.currentTimeMillis());
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

    private LoginClient getLoginClient() throws IOException, LoginException {
        HttpGet httpget = new HttpGet("http://client3.google.com/generate_204");
        HttpResponse response = Utils.createHttpClient(true, null).execute(httpget);

        if (response.getStatusLine().getStatusCode() == 204) {
            // We're online! No login required
            return null;
        }

        String strRes = EntityUtils.toString(response.getEntity());
        Log.d(TAG, strRes);
        if (strRes.contains("https://1.1.1.1/login.html")) {
            // Cisco authentication
            Log.v(TAG, "Cisco network");
            return new CiscoClient();
        }
        else {
            // Assume Aruba
            return getArubaClient();
        }
    }

    private LoginClient getLogoutClient() throws IOException, LoginException {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.d(TAG, "IP :" + ip);
        if (ip.startsWith("10.7.")) {
            // Cisco IP
            Log.v(TAG, "Cisco network");
            return new CiscoClient();
        }
        else {
            // Assume Aruba
            return getArubaClient();
        }
    }

    /**
     * Get a ArubaClient or ArubaIcClient based on SSID
     */
    private LoginClient getArubaClient() throws IOException, LoginException {
        String ssid = Utils.getSsid(this);
        if (ssid != null && ssid.contains(IC_WIFI_SSID)) {
            Log.v(TAG, "Aruba IC network");
            return new ArubaIcClient(this);
        } else {
            Log.v(TAG, "Aruba network");
            return new ArubaClient(this);
        }
    }

}
