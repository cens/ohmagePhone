package edu.ucla.cens.andwellness.triggers.base;

import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.text.format.DateUtils;
import android.util.Log;
import edu.ucla.cens.andwellness.triggers.notif.NotifDesc;
import edu.ucla.cens.andwellness.triggers.notif.Notifier;

/*
 * The base (abstract) trigger type. Provides APIs which can be
 * used by the concrete types to interact with the trig framework
 */
public abstract class TriggerBase {

	private static final String DEBUG_TAG = "TriggerFramework";
	
	public void notifyTrigger(Context context, int trigId) {
		Log.i(DEBUG_TAG, "TriggerBase: notifyTrigger(" + trigId + ")");
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		String rtDesc = db.getRunTimeDescription(trigId);
		TriggerRunTimeDesc desc = new TriggerRunTimeDesc();
		
		desc.loadString(rtDesc);
			
		//update the current timestamp
		desc.setTriggerTimeStamp(System.currentTimeMillis());		
		//update the current loc
		LocationManager locMan = (LocationManager)
								context.getSystemService(Context.LOCATION_SERVICE);
		Location loc = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		desc.setTriggerLocation(loc);

		//Save the run time desc
		db.updateRunTimeDescription(trigId, desc.toString());


		Notifier.notifyNewTrigger(context, trigId, db.getNotifDescription(trigId));
		db.close();
	}
	
	public LinkedList<Integer> getAllTriggerIds(Context context) {
	
		Log.i(DEBUG_TAG, "TriggerBase: getAllTriggerIds()");
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		Cursor c = db.getTriggers(this.getTriggerType());
		LinkedList<Integer> ids = new LinkedList<Integer>();
		
		if(c.moveToFirst()) {
			do {
				ids.add(c.getInt(
				 		c.getColumnIndexOrThrow(TriggerDB.KEY_ID)));	
				
			} while(c.moveToNext());
		}
			
				
		c.close();
		db.close();
		
		return ids;
	}
	
	/*
	 * Note: This function can be optimized by storing the action
	 * count separately in the db
	 */
	public LinkedList<Integer> getAllActiveTriggerIds(Context context) {
		Log.i(DEBUG_TAG, "TriggerBase: getAllActiveTriggerIds()");
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		Cursor c = db.getTriggers(this.getTriggerType());
		LinkedList<Integer> ids =  new LinkedList<Integer>();
		
		if(c.moveToFirst()) {
			do {
				String actDesc = c.getString(
								 c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_ACTION_DESCRIPT));
				TriggerActionDesc desc = new TriggerActionDesc();
				if(!desc.loadString(actDesc)) {
					continue;
				}
				
				if(desc.getCount() > 0) {
				
					ids.add(c.getInt(
					 		c.getColumnIndexOrThrow(TriggerDB.KEY_ID)));	
				}
				
			} while(c.moveToNext());
		}
			
				
		c.close();
		db.close();
	
		return ids;
	}
	
	public String getTrigger(Context context, int trigId) {
		Log.i(DEBUG_TAG, "TriggerBase: getTrigger(" + trigId + ")");
		
		TriggerDB db = new TriggerDB(context);
		db.open();
	
		String ret = null;
		Cursor c = db.getTrigger(trigId);
		if(c.moveToFirst()) {
			
			ret = c.getString(
				  c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_DESCRIPT));
		}
		
		c.close();
		db.close();
		return ret;
	}
	
	public long getTriggerLatestTimeStamp(Context context, int trigId) {
		Log.i(DEBUG_TAG, "TriggerBase: getTriggerLatestTimeStamp(" + trigId + ")");
		
		TriggerDB db = new TriggerDB(context);
		db.open();
	
		long ret = -1;
		Cursor c = db.getTrigger(trigId);
		if(c.moveToFirst()) {
			
			String rtDesc = c.getString(
							c.getColumnIndexOrThrow(TriggerDB.KEY_RUNTIME_DESCRIPT));
			TriggerRunTimeDesc desc = new TriggerRunTimeDesc();
			if(desc.loadString(rtDesc)) {
				ret = desc.getTriggerTimeStamp();
			}
		}
		
		c.close();
		db.close();
		return ret;
	}
	
	public void deleteTrigger(Context context, int trigId) {
		Log.i(DEBUG_TAG, "TriggerBase: deleteTrigger(" + trigId + ")");
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		Cursor c = db.getTrigger(trigId);
		if(c.moveToFirst()) {
			String trigType = c.getString(
					          c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_TYPE));
			
			if(trigType.equals(this.getTriggerType())) {
				//TODO move both these into a common place.
				//Trigger list activity also has the same logic
				db.deleteTrigger(trigId);
				
				//Now refresh the notification display
				Notifier.removeTriggerNotification(context, trigId);
			}
		}
		
		c.close();
		db.close();
	}
	
	public void addNewTrigger(Context context, String trigDesc) {
		Log.i(DEBUG_TAG, "TriggerBase: getTriggerLatestTimeStamp(" + trigDesc + ")");
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		int trigId = (int) db.addTrigger(this.getTriggerType(), trigDesc,
					  			   		TriggerActionDesc.getDefaultDesc(),
					  			   		NotifDesc.getDefaultDesc(context),
					  			   		TriggerRunTimeDesc.getDefaultDesc());
		
		String actDesc = db.getActionDescription(trigId);
		db.close();
		
		TriggerActionDesc desc = new TriggerActionDesc();
		if(desc.loadString(actDesc) && desc.getCount() > 0) {
			startTrigger(context, trigId, trigDesc);
		}	
	}
	
	public void updateTrigger(Context context, int trigId, String trigDesc) {
		Log.i(DEBUG_TAG, "TriggerBase: getTriggerLatestTimeStamp(" + trigId 
										+ ", " + trigDesc + ")");
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		db.updateTriggerDescription(trigId, trigDesc);
		
		String actDesc = db.getActionDescription(trigId);
		db.close();
		
		TriggerActionDesc desc = new TriggerActionDesc();
		if(desc.loadString(actDesc) && desc.getCount() > 0) {
			resetTrigger(context, trigId, trigDesc);
		}
	}	
	
	public boolean hasTriggeredToday(Context context, int trigId) {
		Log.i(DEBUG_TAG, "TriggerBase: hasTriggeredToday(" + trigId + ")");
		
		long trigTS = getTriggerLatestTimeStamp(context, trigId);
		
		if(trigTS != TriggerRunTimeDesc.INVALID_TIMESTAMP) {
			return DateUtils.isToday(trigTS);
		}
		
		return false;
	}
	
	
	public abstract String getTriggerType();
	public abstract int getIcon();
	public abstract String getTriggerTypeDisplayName();
	public abstract String getDisplayTitle(Context context, String trigDesc);
	public abstract String getDisplaySummary(Context context, String trigDesc);

	public abstract void startTrigger(Context context, int trigId, String trigDesc);
	public abstract void resetTrigger(Context context, int trigId, String trigDesc);
	public abstract void removeTrigger(Context context, int trigId, String trigDesc);
	
	public abstract void launchTriggerCreateActivity(Context context);
	public abstract void launchTriggerEditActivity(Context context, int trigId, 
													String trigDesc, boolean adminMode);
	public abstract boolean hasSettings();
	public abstract void launchSettingsEditActivity(Context context, boolean adminMode);
}
