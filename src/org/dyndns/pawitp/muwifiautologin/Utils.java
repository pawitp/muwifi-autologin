package org.dyndns.pawitp.muwifiautologin;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Locale;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

public class Utils {
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
}
