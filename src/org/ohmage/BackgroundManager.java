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

import org.ohmage.db.DbHelper;
import org.ohmage.db.Models.Campaign;
import org.ohmage.logprobe.Log;
import org.ohmage.triggers.base.TriggerInit;

public class BackgroundManager {

    private static final String TAG = "BACKGROUND_MANAGER";

    public static void initComponents(Context context) {

        Log.v(TAG, "initializing application components");

        // init triggers for all campaigns
        DbHelper dbHelper = new DbHelper(context);
        for (Campaign c : dbHelper.getReadyCampaigns()) {
            TriggerInit.initTriggers(context, c.mUrn);
        }
    }
}
