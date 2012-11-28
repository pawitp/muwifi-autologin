package org.dyndns.pawitp.muwifiautologin;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.KeyStore;
import java.util.Locale;

public class Utils {

    private static final String TAG = "Utils";

    static final int CONNECTION_TIMEOUT = 2000;
    static final int SOCKET_TIMEOUT = 2000;

    public static String stackTraceToString(Exception e) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        e.printStackTrace(ps);
        String ret = os.toString();
        ps.close();
        return ret;
    }

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

    public static DefaultHttpClient createHttpClient(int sslMode) {
        // SSL stuff
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore, sslMode);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
            DefaultHttpClient httpClient = new DefaultHttpClient(ccm, params);

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
}
