package org.dyndns.pawitp.muwifiautologin;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static String KEY_ENABLED = "enabled";
	public static String KEY_USERNAME = "username";
	public static String KEY_PASSWORD = "password";
	public static String KEY_VERSION = "version";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        updateUsernameSummary();
        
        // Set version number
		try {
			String versionSummary = String.format(getString(R.string.pref_version_summary), getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
			findPreference(KEY_VERSION).setSummary(versionSummary);
		} catch (NameNotFoundException e) {
			// kind of impossible
		}
    }

	@Override
	protected void onResume() {
		super.onResume();

		// Callback to update username summary when it gets changed
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		updateUsernameSummary();
	}
	
	private void updateUsernameSummary() {
		// Set username as summary if set
        String username = getPreferenceManager().getSharedPreferences().getString(KEY_USERNAME, "");
        if (username.length() != 0) {
        	findPreference(KEY_USERNAME).setSummary(username);
        }
	}
}