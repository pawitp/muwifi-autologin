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

    private static final String TAG = "MuWifiLogin";
    private static final int LOGIN_ERROR_ID = 1;
    private static final int LOGIN_ONGOING_ID = 2;

    public static final String EXTRA_LOGOUT = "logout";
    public static final String EXTRA_USER_TRIGGERED = "user_triggered";

    private Handler mHandler;
    private SharedPreferences mPrefs;
    private NotificationManager mNotifMan;
    private Notification mNotification;
    private Network mNetwork;

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
        boolean isUserTriggered = intent.getBooleanExtra(EXTRA_USER_TRIGGERED, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // For Lollipop+, we need to request the Wi-Fi network since
            // connections will go over mobile data by default if a captive
            // portal is detected
            Log.v(TAG, "Requesting Wi-Fi network");

            if (!requestNetwork()) {
                Log.e(TAG, "Unable to request Wi-Fi network");
                createRetryNotification(isLogout, null, getString(R.string.notify_request_wifi_error_text));
                return;
            }
        }

        doLogin(isLogout, isUserTriggered);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean requestNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        for (Network net : cm.getAllNetworks()) {
            if (cm.getNetworkInfo(net).getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(TAG, "Set network to " + net);
                mNetwork = net;
                ConnectivityManager.setProcessDefaultNetwork(net);
                return true;
            }
        }

        return false;
    }

    /**
     * Report successful to Lollipop's captive portal detector
     *
     * See CaptivePortalLoginActivity in frameworks/base
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void reportStateChange() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // We're reporting "good" network. This function forces Android to
        // re-evaluate the network (and realize it's no longer a captive portal).
        cm.reportBadNetwork(mNetwork);
    }

    private void doLogin(boolean isLogout, boolean isUserTriggered) {
        try {
            if (isLogout) {
                Log.v(TAG, "Logging out");
                updateOngoingNotification(getString(R.string.notify_logout_ongoing_text), true);

                LoginClient loginClient = getLogoutClient();
                loginClient.logout();

                createToastNotification(R.string.logout_successful, Toast.LENGTH_SHORT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Report logout successful so Android stops using this network
                    // (or at least that should happen, but 5.0.0_r2 doesn't seem to
                    // automatically switch to cellular)
                    reportStateChange();
                }

                Log.v(TAG, "Logout successful");
            } else {
                updateOngoingNotification(getString(R.string.notify_login_ongoing_text_determine_requirement), true);
                LoginClient loginClient = getLoginClient();
                if (loginClient != null) {
                    Log.v(TAG, "Login required");

                    if (!isUserTriggered && !loginClient.allowAuto()) {
                        // Require user to click login for insecure hotspots
                        createRetryNotification(isLogout,
                                String.format(getString(R.string.notify_request_wifi_connected_title), Utils.getSsid(this)),
                                getString(R.string.notify_request_wifi_connected_text));
                        return;
                    }

                    String username = mPrefs.getString(Preferences.KEY_USERNAME, null);
                    String password = mPrefs.getString(Preferences.KEY_PASSWORD, null);

                    updateOngoingNotification(getString(R.string.notify_login_ongoing_text_logging_in), true);
                    loginClient.login(username, password);

                    if (mPrefs.getBoolean(Preferences.KEY_TOAST_NOTIFY_SUCCESS, true)) {
                        createToastNotification(R.string.login_successful, Toast.LENGTH_SHORT);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // Report login successful so Android starts using this network
                        reportStateChange();
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

            createRetryNotification(isLogout, null, e.getMessage());
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

    private void createRetryNotification(boolean isLogout, String title, String text) {
        Intent notificationIntent = new Intent(this, MuWifiLogin.class);
        notificationIntent.putExtra(EXTRA_LOGOUT, isLogout);
        notificationIntent.putExtra(EXTRA_USER_TRIGGERED, true);
        PendingIntent contentIntent = PendingIntent.getService(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        if (title == null) {
            title = getString(isLogout ? R.string.notify_logout_error_title : R.string.notify_login_error_title);
        }

        createErrorNotification(contentIntent, title, text, isLogout);
    }

    private void createRetryNotification(boolean isLogout) {
        createRetryNotification(isLogout, null, getString(R.string.notify_login_error_text));
    }

    private void createErrorNotification(PendingIntent contentIntent, String title, String errorText, boolean isLogout) {
        Log.d(TAG, "createErrorNotification isLogout=" + isLogout);

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) {
            // Don't show errors if wifi is disabled
            return;
        }

        Notification notification = new Notification(R.drawable.ic_stat_notify_key, title, System.currentTimeMillis());
        notification.setLatestEventInfo(this, title, errorText, contentIntent);
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

        return getLogoutClient();
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
        else if (ip.startsWith("10.27.")) {
            // IC-WiFi IP
            Log.v(TAG, "IC-WiFi network");
            return new IcClient();
        }
        else {
            // Assume Aruba
            return new ArubaClient(this);
        }
    }
}
