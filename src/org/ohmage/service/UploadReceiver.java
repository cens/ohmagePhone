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

package org.ohmage.service;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.systemlog.Log;

import org.ohmage.db.DbContract.Responses;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class UploadReceiver extends BroadcastReceiver {

	private static final String TAG = "UploadReceiver";

	// alarm to check for new data while phone is plugged in
	public static final String ACTION_UPLOAD_ALARM = "org.ohmage.service.ACTION_UPLOAD_ALARM";

	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		Log.i(TAG, "Broadcast received: " + action);

		if (UploadReceiver.ACTION_UPLOAD_ALARM.equals(action)) {

			// When the alarm goes off, get battery change sticky intent, if
			// plugged in, start upload
			Context appContext = context.getApplicationContext();
			Intent battIntent = appContext.registerReceiver(null, new IntentFilter(
					Intent.ACTION_BATTERY_CHANGED));
			int level = battIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = battIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			int status = battIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			if (level == -1 || scale == -1 || status == -1) {
				Log.e(TAG, "Battery did not report level correctly");
				return;
			}

			float percent = (float) level * 100 / scale;

			// If we have more than 20% battery or we are currently charging,
			// start the upload service
			if (percent > 20 || status == BatteryManager.BATTERY_STATUS_CHARGING) {
				Log.i(TAG, "Power is not low.");
				Log.i(TAG, "Starting UploadService.");

				Intent i = new Intent(context, UploadService.class);
				i.setData(Responses.CONTENT_URI);
				i.putExtra(UploadService.EXTRA_BACKGROUND, true);
				i.putExtra(UploadService.EXTRA_UPLOAD_MOBILITY, true);
				i.putExtra(UploadService.EXTRA_UPLOAD_SURVEYS, true);
				WakefulIntentService.sendWakefulWork(context, i);
			} else {
				Log.i(TAG, "Power is low.");
				Log.i(TAG, "Not starting UploadService.");
			}
		}
	}
}
