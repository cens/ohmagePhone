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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import edu.ucla.cens.systemlog.Log;

public class StorageMonitorAlarmReceiver extends BroadcastReceiver {
	
	private static final String TAG = "STORAGE_MONITOR_ALARM_RECIEVER";
	
	public static final String ACTION_STORAGE_MONITOR_ALARM = "edu.ucla.cens.andwellness.storagemonitor.ACTION_STORAGE_MONITOR_ALARM";

	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.i(TAG, "Recieved: " + intent.getAction());
		
		Intent startIntent = new Intent(context, StorageMonitorService.class);
		context.startService(startIntent);
	}

}
