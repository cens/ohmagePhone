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

import org.ohmage.activity.SurveyListActivity;
import org.ohmage.triggers.glue.TriggerFramework;
import org.ohmage.triggers.notif.Notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
			i.putExtra("campaign_urn", campaignUrn);
			context.startActivity(i);
		}
	}
}
