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
package org.ohmage;

import org.ohmage.feedback.FeedbackSyncReceiver;
import org.ohmage.service.UploadReceiver;
import org.ohmage.storagemonitor.StorageMonitorService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import edu.ucla.cens.systemlog.Log;

public class BackgroundManager {
	
	private static final String TAG = "BACKGROUND_MANAGER";

	public static void initComponents(Context context) {
		
		Log.i(TAG, "initializing application components");
		
		Context appContext = context.getApplicationContext();
		
		//uploadservice
		AlarmManager alarms = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);
		Intent intentToFire = new Intent(UploadReceiver.ACTION_UPLOAD_ALARM);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intentToFire, 0);
		//alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), pendingIntent);
		alarms.cancel(pendingIntent);
		alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 60 * 1000, pendingIntent);
		Log.i(TAG, "UploadReceiver repeating alarm set");
		
		//storagemonitor
		appContext.startService(new Intent(appContext, StorageMonitorService.class));
		Log.i(TAG, "started storage monitor service");
		
		// FAISAL: feedback service repeating alarm registered here
		if (SharedPreferencesHelper.ALLOWS_FEEDBACK) {
			Intent fbServiceSyncIntent = new Intent(FeedbackSyncReceiver.ACTION_FBSYNC_ALARM);
			PendingIntent fbServiceSyncPendingIntent = PendingIntent.getBroadcast(appContext, 0, fbServiceSyncIntent, 0);
			alarms.cancel(fbServiceSyncPendingIntent);
			alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_HOUR, fbServiceSyncPendingIntent);
			Log.i(TAG, "Feedback sync repeating alarm set");
		}
	}
}
