package org.dyndns.pawitp.muwifiautologin;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

public class IOErrorView extends Activity {

	static final String EXTRA_CONTENT = "content";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.io_error);
		
		// Set stack trace
		IOException e = (IOException) getIntent().getSerializableExtra(EXTRA_CONTENT);
		EditText txtStackTrace = (EditText) findViewById(R.id.io_error_txtStackTrace);
		txtStackTrace.setText(Utils.stackTraceToString(e));
	}
	
}
