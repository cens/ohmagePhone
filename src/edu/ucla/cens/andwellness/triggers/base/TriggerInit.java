package edu.ucla.cens.andwellness.triggers.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import edu.ucla.cens.andwellness.triggers.notif.Notifier;

/*
 * Boot listener. Initialized all triggers and pending
 * notifications
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
}
