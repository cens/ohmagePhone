package edu.ucla.cens.andwellness.triggers.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import edu.ucla.cens.andwellness.triggers.notif.Notifier;
import edu.ucla.cens.andwellness.triggers.utils.TrigPrefManager;

/*
 * Boot listener. Starts all the active triggers. 
 * Also restores the pending notifications if any
 */
public class TriggerInit extends BroadcastReceiver {
	
	private static final String DEBUG_TAG = "TriggerFramework";
	
	private static void initTriggers(Context context) {
		
		Log.i(DEBUG_TAG, "TriggerInit: Initializing triggers");
		
		TriggerTypeMap trigMap = new TriggerTypeMap();
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		Cursor c = db.getAllTriggers();
				
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
				
				Log.i(DEBUG_TAG, "TriggerInit: Read from db: " + trigId +
								 ", " + trigDesc + ", " + actDesc);	
				
				TriggerBase trig = trigMap.getTrigger(trigType);
				if(trig != null) {
	
					//Start the trigger
					TriggerActionDesc aDesc = new TriggerActionDesc();
					//Start only if it has a positive number of surveys
					if(aDesc.loadString(actDesc) && aDesc.getCount() > 0) {
						Log.i(DEBUG_TAG, "TriggerInit: Starting trigger: " + trigId + 
										 ", " + trigDesc);
						
						trig.startTrigger(context, trigId, trigDesc);
					}
					
					//Restore the notification states for this trigger
					TriggerRunTimeDesc desc = new TriggerRunTimeDesc();
					if(desc.loadString(rtDesc) && desc.hasTriggerTimeStamp()) {
						Log.i(DEBUG_TAG, "TriggerInit: Restoring notifications for " + trigId);
						
						Notifier.restorePastNotificationStates(context, trigId, notifDesc, 
														desc.getTriggerTimeStamp());
					}
				}
			
			} while(c.moveToNext());
		}
		
		c.close();
		db.close();
		
		//Refresh the notification display
		Notifier.refreshNotification(context, true);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Log.i(DEBUG_TAG, "TriggerInit: Received boot completed intent");
			
			initTriggers(context);
		}
	}
	
	/*
	 * Resets all triggers, settings and preferences to its default.
	 * Removes all triggers from the database after stopping them.
	 */
	public static boolean resetAllTriggersAndSettings(Context context) {
		Log.i(DEBUG_TAG, "TriggerInit: Resetting all triggers");
		
		TriggerTypeMap trigMap = new TriggerTypeMap();
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		Cursor c = db.getAllTriggers();
		
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
		
		//Reset the settings of all trigger types
		for(TriggerBase trig : trigMap.getAllTriggers()) {
			
			if(trig.hasSettings()) {
				trig.resetSettings(context);
			}
		}
		
		//Refresh the notification display
		Notifier.refreshNotification(context, true);
		
		//Clear all preference files registered with the preference manager
		TrigPrefManager.clearAllPreferenceFiles(context);
		
		return true;
	}
}
