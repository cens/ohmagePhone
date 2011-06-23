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
package edu.ucla.cens.andwellness.service;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.andwellness.storagemonitor.StorageMonitorService;
import edu.ucla.cens.systemlog.Log;

public class UploadReceiver extends BroadcastReceiver {

private static final String TAG = "UploadReceiver";
	
	//alarm to check for new data while phone is plugged in
	public static final String ACTION_UPLOAD_ALARM = "edu.ucla.cens.andwellness.service.ACTION_UPLOAD_ALARM";
	private static final long ALARM_FREQ = AlarmManager.INTERVAL_HOUR; 
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		String action = intent.getAction();
		Log.i(TAG, "Broadcast received: " + action);
		
		//When the alarm goes off, get battery change sticky intent, if plugged in, start upload
		if (UploadReceiver.ACTION_UPLOAD_ALARM.equals(action)) {
			
			/*AlarmManager alarms = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			Intent intentToFire = new Intent(UploadReceiver.ACTION_UPLOAD_ALARM);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentToFire, 0);
			alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_FREQ, pendingIntent);
			Log.i(TAG, "Alarm set for " + String.valueOf(ALARM_FREQ/60000) + " minutes from now.");*/
			
			Context appContext = context.getApplicationContext();
			Intent battIntent = appContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			int level = battIntent.getIntExtra("level", -1);
			int scale = battIntent.getIntExtra("scale", -1);
			float percent = (float) level * 100 / (float) scale;
			Log.i(TAG, "Battey level: " + percent + "% ("+ level + " / " + scale + ")");
			if (percent > 20) {
				Log.i(TAG, "Power is not low.");
				Log.i(TAG, "Starting UploadService.");
				
				WakefulIntentService.sendWakefulWork(context, UploadService.class);
			} else {
				Log.i(TAG, "Power is low.");
				Log.i(TAG, "Not starting UploadService.");
			}
			
			/*int plugged = battIntent.getIntExtra("plugged", -1);
			if (	plugged == BatteryManager.BATTERY_PLUGGED_AC ||
					plugged == BatteryManager.BATTERY_PLUGGED_USB) {
				
				Log.i(TAG, "Power is connected.");
				Log.i(TAG, "Starting UploadService.");
				
				WakefulIntentService.acquireStaticLock(context);
				context.startService(new Intent(context, UploadService.class));
				
			} else {
				Log.i(TAG, "Power is disconnected.");
			}*/

		}
		
		//If the storage monitor reports low or very low storage, the upload thread is started. Simple.
		if (StorageMonitorService.ACTION_STORAGE_REPORT.equals(action)) {
			int status = intent.getIntExtra(StorageMonitorService.EXTRA_STORAGE_STATUS, -1);
			if (	status == StorageMonitorService.FREE_STORAGE_LOW ||
					status == StorageMonitorService.FREE_STORAGE_VERY_LOW) {
				
				Log.i(TAG, "Storage is low.");
				Log.i(TAG, "Starting UploadService.");
				
				WakefulIntentService.sendWakefulWork(context, UploadService.class);
				
			} else {
				Log.i(TAG, "Storage is not low.");
			}
		}
		
		
		//The following code passes the original intent data to the UploadService if needed.
		//It's not needed now because the UploadService doesn't care about anything except being told to start Uploading.
		//The UploadService should remain as such.
		//Intent uploadIntent = new Intent(intent).setClass(context, UploadService.class);
		//context.startService(uploadIntent);
		
	}

}
