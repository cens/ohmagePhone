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
package edu.ucla.cens.andwellness.storagemonitor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.systemlog.Log;

public class StorageMonitorSettingsActivity extends Activity {
	
	private static final String TAG = "STORAGE_MONITOR_SETTINGS_ACTIVITY";
	
	CheckBox mCheckBox;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_monitor_settings);
        
        mCheckBox = (CheckBox) findViewById(R.id.is_monitoring_check_box);
        
        mCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
        
        SharedPreferences prefs = getSharedPreferences("STORAGE_MONITOR_PREFS", Activity.MODE_PRIVATE);
		mCheckBox.setChecked(prefs.getBoolean("IS_MONITORING", false));
    }
    
    private OnCheckedChangeListener mCheckedChangeListener = new OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			switch(buttonView.getId()) {
			case R.id.is_monitoring_check_box:
				if(isChecked) {
					SharedPreferences.Editor prefsEdit = getSharedPreferences("STORAGE_MONITOR_PREFS", Activity.MODE_PRIVATE).edit();
					prefsEdit.putBoolean("IS_MONITORING", true);
					prefsEdit.commit();
					Intent startIntent = new Intent(StorageMonitorSettingsActivity.this, StorageMonitorService.class);
					StorageMonitorSettingsActivity.this.startService(startIntent);
					Log.i(TAG, "Starting StorageMonitorService.");
				} else {
					SharedPreferences.Editor prefsEdit = getSharedPreferences("STORAGE_MONITOR_PREFS", Activity.MODE_PRIVATE).edit();
					prefsEdit.putBoolean("IS_MONITORING", false);
					prefsEdit.commit();
				}
			}
		}
	};
    
}
