/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

/*
 * Authored by: Ankit Gupta. for questions, please email: agupta423@gmail.com
 * Adapted from Location Triggers and Time Triggers by: Kannan Parameswaran
 * 
 */
package org.ohmage.triggers.types.activity;

import org.ohmage.R;
import org.ohmage.triggers.utils.SimpleTime;
import org.ohmage.triggers.utils.TimePickerPreference;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class ActTrigSettingsActivity extends PreferenceActivity 
									implements OnPreferenceClickListener, 
									OnPreferenceChangeListener {
	private SharedPreferences sharedPref;
	private static final String PREF_KEY_SLEEP_TIME = "activity_sleep";
	private static final String SLEEP_HOUR_KEY = "activity_trigger_sleep_hour";
	private static final String SLEEP_MIN_KEY = "activity_trigger_sleep_minute";
	
	private static int HOUR = -1;
	private static int MIN = -1;
	

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.trig_act_settings_preferences);
		setContentView(R.layout.trigger_act_settings);
		
		Button done = (Button) findViewById(R.id.trigger_act_settings_done);
		final TimePickerPreference sleepPicker = (TimePickerPreference) getPreferenceScreen().findPreference(PREF_KEY_SLEEP_TIME);
		
		done.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				SimpleTime time = sleepPicker.getTime();
				HOUR = time.getHour();
				MIN = time.getMinute();
				
				if (HOUR < 16){
					Toast.makeText(getBaseContext(), "Please pick a later time", Toast.LENGTH_SHORT).show();
					return;
				}
				updateSleepPref(HOUR,MIN);
				finish();
			}
		
		});
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		HOUR = sharedPref.getInt(SLEEP_HOUR_KEY, 21);
		MIN = sharedPref.getInt(SLEEP_MIN_KEY, 0);
		
		sleepPicker.setTime(HOUR, MIN);
		
	}

	private void updateSleepPref(int hour, int minute){
		Editor editor = sharedPref.edit();
		editor.putInt(SLEEP_HOUR_KEY, hour);
		editor.putInt(SLEEP_MIN_KEY, minute);
		editor.commit();
	}
	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		return false;
	}
}
