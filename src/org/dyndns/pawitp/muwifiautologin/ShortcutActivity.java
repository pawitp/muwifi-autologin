package org.dyndns.pawitp.muwifiautologin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

public class ShortcutActivity extends Activity {

    public static final String SHORTCUT_INTENT = "org.dyndns.pawitp.muwifiautologin.SHORTCUT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isLogout = getIntent().getBooleanExtra(MuWifiLogin.EXTRA_LOGOUT, false);
        Utils.checkWifiAndDoLogin(this, isLogout);

        finish();
    }

    public static class CreateLoginShortcut extends Activity {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setResult(RESULT_OK, createShortcutIntent(this, false));
            finish();
        }

    }

    public static class CreateLogoutShortcut extends Activity {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setResult(RESULT_OK, createShortcutIntent(this, true));
            finish();
        }

    }

    private static Intent createShortcutIntent(Context context, boolean isLogout) {
        Intent shortcutIntent = new Intent(ShortcutActivity.SHORTCUT_INTENT);
        shortcutIntent.putExtra(MuWifiLogin.EXTRA_LOGOUT, isLogout);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            addClearTaskFlag(shortcutIntent);
        } else {
            // This will cause the Setting page to disappear from recents
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent.ShortcutIconResource iconResource = Intent.ShortcutIconResource.fromContext(context, R.drawable.icon);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getString(isLogout ? R.string.logout : R.string.login));
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
        return intent;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void addClearTaskFlag(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

}
