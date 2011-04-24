package org.dyndns.pawitp.muwifiautologin;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

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
}
