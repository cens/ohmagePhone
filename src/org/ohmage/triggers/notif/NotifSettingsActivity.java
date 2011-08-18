package org.ohmage.triggers.notif;

import org.ohmage.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class NotifSettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.notification_settings);
	}
}
