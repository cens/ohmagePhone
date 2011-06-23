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
package edu.ucla.cens.andwellness.triggers.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import edu.ucla.cens.andwellness.db.Campaign;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.triggers.notif.Notifier;

/*
 * Time/Time-zone change listener. Restarts the triggers and
 * refreshes the notification 
 */
public class TriggerTimeReceiver extends BroadcastReceiver{

	private static final String DEBUG_TAG = "TriggerFramework";
	
	private static void handleTimeChange(Context context, String campaignUrn) {
		TriggerTypeMap trigMap = new TriggerTypeMap();
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		Cursor c = db.getAllTriggers(campaignUrn);
		
		if(c.moveToFirst()) {
			do {
				int trigId = c.getInt(
							 c.getColumnIndexOrThrow(TriggerDB.KEY_ID));
				String trigDesc = c.getString(
								  c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_DESCRIPT));
				String trigType = c.getString(
				 		   		  c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_TYPE));
				String actDesc = c.getString(
 		   		  				 c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_ACTION_DESCRIPT));
				
				TriggerBase trig = trigMap.getTrigger(trigType);
				if(trig != null) {
					TriggerActionDesc aDesc = new TriggerActionDesc();
					//Restart the trigger if it is active
					if(aDesc.loadString(actDesc) && aDesc.getCount() > 0) {
						trig.resetTrigger(context, trigId, trigDesc);
					}
				}
			} while(c.moveToNext());
		}
		
		c.close();
		db.close();
		
		//Finally, quietly refresh the notification
		Notifier.refreshNotification(context, campaignUrn, true);
	}
	
	@Override
	public void onReceive(Context context, Intent i) {
		if(i.getAction().equals(Intent.ACTION_TIME_CHANGED) || 
		   i.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
			
			Log.i(DEBUG_TAG, "TriggerTimeReceiver: " + i.getAction());
			
			DbHelper dbHelper = new DbHelper(context);
			for (Campaign c : dbHelper.getCampaigns()) {
				handleTimeChange(context, c.mUrn);
			}
		}
	}
}
