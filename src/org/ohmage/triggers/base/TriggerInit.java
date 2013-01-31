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
package org.ohmage.triggers.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.db.DbHelper;
import org.ohmage.db.Models.Campaign;
import org.ohmage.logprobe.Log;
import org.ohmage.triggers.config.NotifConfig;
import org.ohmage.triggers.notif.NotifDesc;
import org.ohmage.triggers.notif.Notifier;
import org.ohmage.triggers.types.location.LocTrigMapsActivity;
import org.ohmage.triggers.types.location.LocationTrigger;
import org.ohmage.triggers.types.time.TimeTrigger;
import org.ohmage.triggers.utils.TrigPrefManager;

/*
 * Boot listener. Starts all the active triggers. 
 * Also restores the pending notifications if any
 */
public class TriggerInit {
	
	private static final String TAG = "TriggerFramework";
	
	public static void initTriggers(Context context, String campaignUrn) {
		
		Log.v(TAG, "TriggerInit: Initializing triggers for " + campaignUrn);
		
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
			
				String notifDesc = c.getString(
						 		   c.getColumnIndexOrThrow(TriggerDB.KEY_NOTIF_DESCRIPT));
				
				String trigType = c.getString(
				 		   		  c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_TYPE));
				
				String rtDesc = c.getString(
		 		   		  		c.getColumnIndexOrThrow(TriggerDB.KEY_RUNTIME_DESCRIPT));
				
				String actDesc = c.getString(
 		   		  				 c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_ACTION_DESCRIPT));
				
				Log.v(TAG, "TriggerInit: Read from db: " + trigId +
								 ", " + trigDesc + ", " + actDesc);	
				
				TriggerBase trig = trigMap.getTrigger(trigType);
				if(trig != null) {
	
					//Start the trigger
					TriggerActionDesc aDesc = new TriggerActionDesc();
					//Start only if it has a positive number of surveys
					if(aDesc.loadString(actDesc) && aDesc.getCount() > 0) {
						Log.v(TAG, "TriggerInit: Starting trigger: " + trigId + 
										 ", " + trigDesc);
						
						trig.startTrigger(context, trigId, trigDesc);
					}
					
					//Restore the notification states for this trigger
					TriggerRunTimeDesc desc = new TriggerRunTimeDesc();
					if(desc.loadString(rtDesc) && desc.hasTriggerTimeStamp()) {
						Log.v(TAG, "TriggerInit: Restoring notifications for " + trigId);
						
						Notifier.restorePastNotificationStates(context, trigId, notifDesc, 
														desc.getTriggerTimeStamp());
					}
				}
			
			} while(c.moveToNext());
		}
		
		c.close();
		db.close();
		
		//Refresh the notification display
		Notifier.refreshNotification(context, campaignUrn, true);
	}
	
	
	/*
	 * Resets all triggers, settings and preferences to its default.
	 * Removes all triggers from the database after stopping them.
	 */
	public static boolean resetTriggersAndSettings(Context context, String campaignUrn) {
		Log.v(TAG, "TriggerInit: Resetting all triggers for " + campaignUrn);
		
		TriggerTypeMap trigMap = new TriggerTypeMap();
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		Cursor c = db.getAllTriggers(campaignUrn);
		
		//Stop and delete all triggers
		if(c.moveToFirst()) {
			do {
				int trigId = c.getInt(
							 c.getColumnIndexOrThrow(TriggerDB.KEY_ID));
				
				TriggerBase trig = trigMap.getTrigger(
											db.getTriggerType(trigId));
				if(trig != null) {
					//delete the trigger 
					trig.deleteTrigger(context, trigId);
				}
			} while(c.moveToNext());
		}
		
		c.close();
		db.close();
		
		
		
		//Refresh the notification display
		Notifier.refreshNotification(context, campaignUrn, true);
		
		//Clear all preference files registered with the preference manager
		TrigPrefManager.clearPreferenceFiles(context, campaignUrn);
		
		return true;
	}
	
	public static boolean resetAllTriggersAndSettings(Context context) {
		Log.v(TAG, "TriggerInit: Resetting all triggers");
		
		DbHelper dbHelper = new DbHelper(context);
		for (Campaign c : dbHelper.getReadyCampaigns()) {
			resetTriggersAndSettings(context, c.mUrn);
		}
		
		TrigPrefManager.clearPreferenceFiles(context, "GLOBAL");
		
		TriggerTypeMap trigMap = new TriggerTypeMap();
		
		//Reset the settings of all trigger types
		for(TriggerBase trig : trigMap.getAllTriggers()) {
			
			if(trig.hasSettings()) {
				trig.resetSettings(context);
			}
		}
		
		SharedPreferences pref = context.getSharedPreferences(LocTrigMapsActivity.TOOL_TIP_PREF_NAME, Context.MODE_PRIVATE);

		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean(LocTrigMapsActivity.KEY_TOOL_TIP_DO_NT_SHOW, false);
		editor.commit();
		
		NotifDesc.setGlobalDesc(context, NotifConfig.defaultConfig);
		
		return true;
	}

	/**
	 * <p>Adds default triggers read from R.raw.triggers in json format</p>
	 *
	 * <p>Examples:</p>
	 * <ul>
	 * <li>1) A time trigger for a specific survey in a campaign that goes off at 2:26 every day</li>
	 * <li>2) A time trigger for a specific survey in a campaign that goes off at 3:30pm every day, but has a start and end time range of 3:00pm to 4:00pm</li>
	 * <li>3) A time trigger for two different surveys in a campaign that goes off randomly between 3:00pm and 4:00pm every day</li>
	 * <li>4) A location trigger for Home</li>
	 * <li>5) A location trigger for Work that will only go off if you go there between 3:00pm and 4:00pm</li>
	 * <li>6) A location trigger for Work that will go off at 4:00pm even if the location is not reached</li>
	 * </ul>
	 * <pre>
	 * {@code
	 * {
	 * 	"triggers":[
	 * 		{
	 * 		"campaign_urn":"urn:mo:chipts",
	 * 		"type":"TimeTrigger",
	 * 		"description":{"time":"14:26","repeat":["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"]},
	 * 		"action":{"surveys":["General Feeling Today"]}
	 * 		},
	 *
	 * 		{
	 * 		"campaign_urn":"urn:mo:chipts",
	 * 		"type":"TimeTrigger",
	 * 		"description":{"start":"15:00","end":"16:00","time":"15:30","repeat":["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"]},
	 * 		"action":{"surveys":["General Feeling Today"]}
	 * 		},
	 *
	 * 		{
	 * 		"campaign_urn":"urn:mo:chipts",
	 * 		"type":"TimeTrigger",
	 * 		"description":{"start":"15:00","end":"16:00","time":"random","repeat":["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"]},
	 * 		"action":{"surveys":["General Feeling Today","Medication"]}
	 * 		},
	 *
	 * 		{
	 * 		"campaign_urn":"urn:mo:chipts",
	 * 		"type":"LocationTrigger",
	 * 		"description":{"location":"Home","min_reentry_interval":120},
	 * 		"action":{"surveys":["General Feeling Today","Medication"]}
	 * 		},
	 *
	 * 		{
	 * 		"campaign_urn":"urn:mo:chipts",
	 * 		"type":"LocationTrigger",
	 * 		"description":{"time_range":{"trigger_always":false,"start":"15:00","end":"16:00"},"location":"Work","min_reentry_interval":120},
	 * 		"action":{"surveys":["General Feeling Today","Medication"]}
	 * 		},
	 *
	 * 		{
	 * 		"campaign_urn":"urn:mo:chipts",
	 * 		"type":"LocationTrigger",
	 * 		"description":{"time_range":{"trigger_always":true,"start":"15:00","end":"16:00"},"location":"Work","min_reentry_interval":120},
	 * 		"action":{"surveys":["Medication"]}
	 * 		}
	 * 	]
	 * }</pre>
	 * @param context
	 * @param camapaignUrn - filter for triggers which have this campaignUrn (set to null for no filtering)
	 */
	public static void addDefaultTriggers(Context context, String campaignUrn) {
		try {
			JSONArray triggersList = new JSONObject(Utilities.convertStreamToString(context.getResources().openRawResource(R.raw.triggers))).getJSONArray("triggers");
			for(int i=0;i<triggersList.length();i++) {
				JSONObject trigger = triggersList.getJSONObject(i);
				if(campaignUrn == null || campaignUrn.equals(trigger.getString("campaign_urn"))) {
					String type = trigger.getString("type");
					if("TimeTrigger".equals(type)) {
						TimeTrigger time = new TimeTrigger();
						time.addNewTrigger(context, trigger.getString("campaign_urn"), trigger.getString("description"), trigger.getString("action"));
					} else if("LocationTrigger".equals(type)) {
						LocationTrigger location = new LocationTrigger();
						location.addNewTrigger(context, trigger.getString("campaign_urn"), trigger.getString("description"), trigger.getString("action"));
					} else {
						throw new RuntimeException("Unsupported trigger type");
					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
