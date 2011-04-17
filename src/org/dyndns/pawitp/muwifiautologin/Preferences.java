package org.dyndns.pawitp.muwifiautologin;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	static final String KEY_LOGIN_NOW = "login_now";
	static final String KEY_ENABLED = "enabled";
	static final String KEY_USERNAME = "username";
	static final String KEY_PASSWORD = "password";
	static final String KEY_VERSION = "version";
	
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
		
		// Login now callback
		findPreference(KEY_LOGIN_NOW).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				MuWifiLogin login = new MuWifiLogin(Preferences.this, getPreferenceManager().getSharedPreferences());
				login.login();
				return true;
			}
		});
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
		if (key.equals(KEY_USERNAME)) {
			updateUsernameSummary();
		}
	}
	
	private void updateUsernameSummary() {
		// Set username as summary if set
        String username = getPreferenceManager().getSharedPreferences().getString(KEY_USERNAME, "");
        if (username.length() != 0) {
        	findPreference(KEY_USERNAME).setSummary(username);
        } else {
        	findPreference(KEY_USERNAME).setSummary(R.string.pref_username_summary);
        }
	}
}