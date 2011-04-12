package org.dyndns.pawitp.muwifiautologin;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ErrorTextView extends Activity {
	
	static final String EXTRA_CONTENT = "content";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TextView textview = new TextView(this);
		setContentView(textview);
		textview.setText(getIntent().getStringExtra(EXTRA_CONTENT), TextView.BufferType.NORMAL);
	}
	
}
