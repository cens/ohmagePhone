package org.ohmage.triggers.notif;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.mobilizingcs.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.LogProbe.Status;

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
