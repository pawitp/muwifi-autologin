package org.dyndns.pawitp.muwifiautologin;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class ErrorWebView extends Activity {

	static final String EXTRA_CONTENT = "content";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		WebView webview = new WebView(this);
		webview.getSettings().setBuiltInZoomControls(true);
		setContentView(webview);
		webview.loadDataWithBaseURL(NetworkStateChanged.FORM_URL, getIntent().getStringExtra(EXTRA_CONTENT), null, "utf-8", null);
	}	
	
}
