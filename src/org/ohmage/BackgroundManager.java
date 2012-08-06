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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import edu.ucla.cens.systemlog.Log;

import org.ohmage.db.DbHelper;
import org.ohmage.db.Models.Campaign;
import org.ohmage.responsesync.ResponseSyncReceiver;
import org.ohmage.service.UploadReceiver;
import org.ohmage.triggers.base.TriggerInit;

public class BackgroundManager {

    private static final String TAG = "BACKGROUND_MANAGER";

    public static void initComponents(Context context) {

        Log.i(TAG, "initializing application components");

        // init triggers for all campaigns
        DbHelper dbHelper = new DbHelper(context);
        for (Campaign c : dbHelper.getReadyCampaigns()) {
            TriggerInit.initTriggers(context, c.mUrn);
        }
    }

    public static void verifyAlarms(Context context) {

        Log.i(TAG, "verifying application alarms");

        Context appContext = context.getApplicationContext();

        // uploadservice
        AlarmManager alarms = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intentToFire = new Intent(UploadReceiver.ACTION_UPLOAD_ALARM);

        // Set the alarm if it is not already set
        if (PendingIntent.getBroadcast(context, 0, intentToFire, PendingIntent.FLAG_NO_CREATE) == null) {
            PendingIntent pendingIntent = PendingIntent
                    .getBroadcast(appContext, 0, intentToFire, 0);
            alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_HOUR, pendingIntent);
            Log.i(TAG, "UploadReceiver repeating alarm set");
        }

        // response sync service
        if (context.getResources().getBoolean(R.bool.allows_feedback)) {
            Intent fbServiceSyncIntent = new Intent(ResponseSyncReceiver.ACTION_FBSYNC_ALARM);

            // Set the alarm if it is not already set
            if (PendingIntent.getBroadcast(context, 0, fbServiceSyncIntent,
                    PendingIntent.FLAG_NO_CREATE) == null) {
                PendingIntent fbServiceSyncPendingIntent = PendingIntent.getBroadcast(appContext,
                        0, fbServiceSyncIntent, 0);
                alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_HOUR,
                        fbServiceSyncPendingIntent);
                Log.i(TAG, "Feedback sync repeating alarm set");
            }
        }
    }
}
