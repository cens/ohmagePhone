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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import edu.ucla.cens.andwellness.activity.SurveyListActivity;
import edu.ucla.cens.andwellness.db.Campaign;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;
import edu.ucla.cens.andwellness.triggers.notif.Notifier;
import edu.ucla.cens.systemlog.Log;

public class TriggerNotificationReceiver extends BroadcastReceiver {
	
	private static final String TAG = "TriggerNotificationReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		
		String action = intent.getAction();
		String campaignUrn = intent.getStringExtra(Notifier.KEY_CAMPAIGN_URN);
		String campaignName = intent.getStringExtra(Notifier.KEY_CAMPAIGN_NAME);

		Log.i(TAG, "Broadcast received: " + action);

		if (TriggerFramework.ACTION_TRIGGER_NOTIFICATION.equals(action)) {
			Intent i = new Intent(context, SurveyListActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // need to fix this so it doesn't start new activity if already on screen
			i.putExtra("campaign_urn", campaignUrn);
			i.putExtra("campaign_name", campaignName);
			context.startActivity(i);
		}
	}
}
