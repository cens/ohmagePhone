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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.ohmage.UserPreferencesHelper;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.logprobe.Log;
import org.ohmage.responsesync.ResponseSyncService;

public class SyncReceiver extends BroadcastReceiver {

    private static final String TAG = "UploadReceiver";

    // alarm to check for new data while phone is plugged in
    public static final String ACTION_SYNC_ALARM = "org.ohmage.service.ACTION_SYNC_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (SyncReceiver.ACTION_SYNC_ALARM.equals(action)) {

            // Don't try to upload if we have less than 20% battery
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
            if ((float) level * 100 / scale < 20
                    && status != BatteryManager.BATTERY_STATUS_CHARGING)
                return;

            // If we aren't connected don't try to upload
            if (!isOnline(context))
                return;

            // Check if user wants uploads to only happen over wifi
            UserPreferencesHelper user = new UserPreferencesHelper(context);
            ConnectivityManager connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (!user.getUploadResponsesWifiOnly()
                    || (wifiInfo != null && wifiInfo.isConnected())) {
                // Start the normal upload service
                Intent i = new Intent(context, UploadService.class);
                i.setData(Responses.CONTENT_URI);
                i.putExtra(UploadService.EXTRA_BACKGROUND, true);
                WakefulIntentService.sendWakefulWork(context, i);

                // Download responses
                WakefulIntentService.sendWakefulWork(context, ResponseSyncService.class);
            }

            if (!user.getUploadProbesWifiOnly()
                    || (wifiInfo != null && wifiInfo.isConnected())) {
                // And start the probe upload service
                Intent i = new Intent(context, ProbeUploadService.class);
                i.setData(Responses.CONTENT_URI);
                i.putExtra(UploadService.EXTRA_BACKGROUND, true);
                WakefulIntentService.sendWakefulWork(context, i);
            }
        }
    }

    public boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
