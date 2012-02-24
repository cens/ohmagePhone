package org.ohmage.triggers.notif;

import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Analytics.Status;

import org.ohmage.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class NotifSettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.notification_settings);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Analytics.activity(this, Status.ON);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Analytics.activity(this, Status.OFF);
	}
}
