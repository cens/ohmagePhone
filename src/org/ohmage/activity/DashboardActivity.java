package org.ohmage.activity;

import org.ohmage.R;
import android.app.Activity;
import android.os.Bundle;

public class DashboardActivity extends Activity {
	private static final String TAG = "DashboardActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.dashboard);
	}
}
