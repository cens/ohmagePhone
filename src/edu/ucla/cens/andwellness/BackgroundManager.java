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
package edu.ucla.cens.andwellness;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.os.SystemClock;
import android.widget.Toast;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.service.UploadReceiver;
import edu.ucla.cens.andwellness.storagemonitor.StorageMonitorService;
import edu.ucla.cens.systemlog.Log;

public class BackgroundManager {
	
	private static final String TAG = "BACKGROUND_MANAGER";

	public static void initComponents(Context context) {
		
		Log.i(TAG, "initializing application components");
		
		Context appContext = context.getApplicationContext();
		
		/*boolean parsedCampaign = false;
		
		SharedPreferencesHelper prefs = new SharedPreferencesHelper(appContext);
		try {
			Map<String, String> map = PromptXmlParser.parseCampaignInfo(CampaignXmlHelper.loadDefaultCampaign(appContext));
			if (map.containsKey("campaign_name") && map.containsKey("campaign_urn")) {
				prefs.putCampaignName(map.get("campaign_name"));
				prefs.putCampaignUrn(map.get("campaign_urn"));
				parsedCampaign = true;
			} 
			if (map.containsKey("server_url")) {
				prefs.putServerUrl(map.get("server_url"));
			}
		} catch (NotFoundException e) {
			Log.e(TAG, "Error parsing xml for campaign info", e);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Error parsing xml for campaign info", e);
		} catch (IOException e) {
			Log.e(TAG, "Error parsing xml for campaign info", e);
		}
		
		if (!parsedCampaign) {
			Log.e(TAG, "Unable to set campaign name and version!");
			Toast.makeText(appContext, "Unable to set campaign name and version!", Toast.LENGTH_LONG).show();
		}*/
		
		//uploadservice
		AlarmManager alarms = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);
		Intent intentToFire = new Intent(UploadReceiver.ACTION_UPLOAD_ALARM);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intentToFire, 0);
		//alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), pendingIntent);
		alarms.cancel(pendingIntent);
		alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_HOUR, pendingIntent);
		Log.i(TAG, "UploadReceiver repeating alarm set");
		
		//storagemonitor
		appContext.startService(new Intent(appContext, StorageMonitorService.class));
		Log.i(TAG, "started storage monitor service");
		
		
	}
}
