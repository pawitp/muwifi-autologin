package org.dyndns.pawitp.muwifiautologin;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Utils {
	public static String stackTraceToString(Exception e) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		e.printStackTrace(ps);
		String ret = os.toString();
		ps.close();
		return ret;
	}
}
