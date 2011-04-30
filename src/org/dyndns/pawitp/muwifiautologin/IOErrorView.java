package org.dyndns.pawitp.muwifiautologin;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

public class IOErrorView extends Activity {

	static final String EXTRA_CONTENT = "content";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.loadLocale(this);
		setContentView(R.layout.io_error);
		
		// Set stack trace
		EditText txtStackTrace = (EditText) findViewById(R.id.io_error_txtStackTrace);
		txtStackTrace.setText(getIntent().getStringExtra(EXTRA_CONTENT));
	}
	
}
