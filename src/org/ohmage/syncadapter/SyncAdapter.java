/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ohmage.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncResult;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.ohmage.UserPreferencesHelper;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.responsesync.ResponseSyncService;
import org.ohmage.service.ProbeUploadService;
import org.ohmage.service.UploadService;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SyncAdapter";
    private final Context mContext;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {

        // Don't try to upload if we have less than 20% battery
        Context appContext = mContext.getApplicationContext();
        Intent battIntent = appContext.registerReceiver(null, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));
        int level = battIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = battIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (level == -1 || scale == -1 || status == -1) {
            Log.e(TAG, "Battery did not report level correctly");
            return;
        }
        if ((float) level * 100 / scale < 20 && status != BatteryManager.BATTERY_STATUS_CHARGING)
            return;

        // If we aren't connected don't try to upload
        if (!isOnline(mContext))
            return;

        // Check if user wants uploads to only happen over wifi
        UserPreferencesHelper user = new UserPreferencesHelper(mContext);
        ConnectivityManager connManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!user.getUploadResponsesWifiOnly() || (wifiInfo != null && wifiInfo.isConnected())) {
            // Start the normal upload service
            Intent i = new Intent(mContext, UploadService.class);
            i.setData(Responses.CONTENT_URI);
            i.putExtra(UploadService.EXTRA_BACKGROUND, true);
            WakefulIntentService.sendWakefulWork(mContext, i);

            // Download responses
            WakefulIntentService.sendWakefulWork(mContext, ResponseSyncService.class);
        }

        if (!user.getUploadProbesWifiOnly() || (wifiInfo != null && wifiInfo.isConnected())) {
            // And start the probe upload service
            Intent i = new Intent(mContext, ProbeUploadService.class);
            i.setData(Responses.CONTENT_URI);
            i.putExtra(UploadService.EXTRA_BACKGROUND, true);
            WakefulIntentService.sendWakefulWork(mContext, i);
        }
    }

    public boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
