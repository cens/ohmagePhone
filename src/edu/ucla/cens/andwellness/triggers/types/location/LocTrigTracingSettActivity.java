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
package edu.ucla.cens.andwellness.triggers.types.location;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import edu.ucla.cens.andwellness.R;

/*
 * Preference activity to manage the settings of location tracing.
 * Currently, two settings are provided
 * 	- Enable/disable tracing
 *  - Enable/disable 'upload always'
 *  
 * When 'upload always' is enabled, the LocTrigService always uploads
 * the trace at the end of every duty cycle even if the user location
 * has not changed. If 'upload always' is disabled, the traces are 
 * uploaded according to the parameters defined in LocTrigConfig
 */
public class LocTrigTracingSettActivity extends PreferenceActivity 
							implements OnPreferenceChangeListener {
	
	public static final String PREF_KEY_TRACING_STATUS = 
										"enable_location_tracing";
	public static final String PREF_KEY_UPLOAD_ALWAYS = 
										"enable_trace_upload_always";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.trig_loc_trace_preferences);
		
		PreferenceScreen screen = getPreferenceScreen();
		int prefCount = screen.getPreferenceCount();
		for(int i = 0; i < prefCount; i++) {
			screen.getPreference(i).setOnPreferenceChangeListener(this);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference pref, Object val) {
		
		//Whenever any of the preference value changes, notify
		//the service to refresh these values 
		Intent i = new Intent(this, LocTrigService.class);
		i.setAction(LocTrigService.ACTION_UPDATE_TRACING_STATUS);
		startService(i);
		
		return true;
	}
	
	public static void resetAllSettings(Context context) {
		SharedPreferences prefs = 
		  		PreferenceManager.getDefaultSharedPreferences(context);
	
		Editor editor = prefs.edit();
		editor.remove(PREF_KEY_TRACING_STATUS);
		editor.remove(PREF_KEY_UPLOAD_ALWAYS);
		editor.commit();
	}
}
