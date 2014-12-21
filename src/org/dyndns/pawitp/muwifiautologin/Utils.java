package org.dyndns.pawitp.muwifiautologin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.util.Locale;

public class Utils {

    private static final String TAG = "Utils";

    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int SOCKET_TIMEOUT = 30000;

    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            // Should not happen
            return "Unknown";
        }
    }

    public static void loadLocale(Context context) {
        String lang = PreferenceManager.getDefaultSharedPreferences(context).getString(Preferences.KEY_LANGUAGE, Preferences.LANGUAGE_DEFAULT);
        Configuration config = new Configuration();
        if (!lang.equals(Preferences.LANGUAGE_DEFAULT)) {
            config.locale = new Locale(lang);
        }
        else {
            config.locale = Locale.getDefault();
        }
        context.getResources().updateConfiguration(config, null);
    }

    public static void setEnableBroadcastReceiver(Context context, boolean enabled) {
        Log.v(TAG, "Setting BroadcastReceiver status to: " + enabled);

        ComponentName receiver = new ComponentName(context, NetworkStateChanged.class);
        int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        context.getPackageManager().setComponentEnabledSetting(receiver, state, PackageManager.DONT_KILL_APP);
    }

    public static DefaultHttpClient createHttpClient(boolean customVerify, byte[][] trustedDers) {
        // SSL stuff
        try {
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);

            DefaultHttpClient httpClient;

            if (!customVerify) {
                if (trustedDers != null) {
                    throw new IllegalArgumentException("customVerify must be true when using trustedDer");
                }

                // Use default HTTP client
                httpClient = new DefaultHttpClient(params);
            } else {
                // Create an HTTP client that doesn't verify SSL certificates
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);

                SSLSocketFactory sf = new MySSLSocketFactory(trustStore, trustedDers);
                sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

                SchemeRegistry registry = new SchemeRegistry();
                registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                registry.register(new Scheme("https", sf, 443));

                ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
                httpClient = new DefaultHttpClient(ccm, params);
            }

            // Also retry POST requests (normally not retried because it is not regarded idempotent)
            httpClient.setHttpRequestRetryHandler(new PostRetryHandler());

            return httpClient;
        }
        catch (Exception e) {
            // Too many to handle individually
            Log.e(TAG, "", e);
            return null;
        }
    }

    public static String getSsid(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        try {
            return wifi.getConnectionInfo().getSSID();
        }
        catch (NullPointerException e) {
            // So many things can be null here when network is not connected
            Log.d(TAG, "Exception getting SSID", e);
            return null;
        }
    }

    public static byte[] getRawResource(Context context, int id) throws IOException {
        InputStream is = context.getResources().openRawResource(id);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int size;
        byte[] buffer = new byte[1024];
        while ((size = is.read(buffer)) >= 0) {
            baos.write(buffer, 0, size);
        }
        is.close();

        return baos.toByteArray();
    }

    public static String getLogCat() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line).append("\n");
            }
            return log.toString();
        }
        catch (IOException e) {
            return null;
        }
    }

    public static void checkWifiAndDoLogin(Context context, boolean isLogout) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            Intent i = new Intent(context, MuWifiLogin.class);
            i.putExtra(MuWifiLogin.EXTRA_LOGOUT, isLogout);
            i.putExtra(MuWifiLogin.EXTRA_USER_TRIGGERED, true);
            context.startService(i);
        }
        else {
            Toast.makeText(context, R.string.wifi_disabled, Toast.LENGTH_SHORT).show();
        }
    }
}
