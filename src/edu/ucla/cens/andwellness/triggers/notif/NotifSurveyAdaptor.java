package edu.ucla.cens.andwellness.triggers.notif;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import edu.ucla.cens.andwellness.triggers.base.TriggerActionDesc;
import edu.ucla.cens.andwellness.triggers.base.TriggerDB;
import edu.ucla.cens.andwellness.triggers.base.TriggerRunTimeDesc;

public class NotifSurveyAdaptor {
	
	private static final String DEBUG_TAG = "TriggerFramework";
	
	private static final String KEY_ACTIVE_TRIGGERS = "active_triggers";
	private static final String KEY_TRIGGER_DESC = "trigger_description";
	private static final String KEY_NOTIF_DESC = "notification_description";
	private static final String KEY_RUNTIME_DESC = "runtime_description";
	
	private static HashSet<String> getActiveSurveys(Context context, Cursor trig) {
		
		HashSet<String> actSurveys = new HashSet<String>();
		
		String runTime = trig.getString(
						 trig.getColumnIndexOrThrow(TriggerDB.KEY_RUNTIME_DESCRIPT));
		
		String notif = trig.getString(
					   trig.getColumnIndexOrThrow(TriggerDB.KEY_NOTIF_DESCRIPT));
		
		String actions = trig.getString(
						 trig.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_ACTION_DESCRIPT));
	
		Log.i(DEBUG_TAG, "NotifSurveyAdaptor: Calculating active surveys for trigger");
		
		TriggerRunTimeDesc rtDesc = new TriggerRunTimeDesc();
		NotifDesc notifDesc = new NotifDesc();
		TriggerActionDesc actDesc = new TriggerActionDesc();
		
		if(!rtDesc.loadString(runTime) || 
		   !notifDesc.loadString(notif) || 
		   !actDesc.loadString(actions)) {
				
			Log.w(DEBUG_TAG, "NotifSurveyAdaptor: Descritptor(s) failed to parse");
			
			return actSurveys;
		}
		
		if(!rtDesc.hasTriggerTimeStamp()) {
			Log.i(DEBUG_TAG, "NotifSurveyAdaptor: Trigger time stamp is invalid");
			
			return actSurveys;
		}
		
		long now = System.currentTimeMillis();
		long trigTS = rtDesc.getTriggerTimeStamp();
		long suppressMS = notifDesc.getSuppression() * 60000;  
			
		if(trigTS > now) {
			Log.w(DEBUG_TAG, "NotifSurveyAdaptor: Trigger time stamp is in the future!");
			return actSurveys;
		}
		
		int elapsedMins = (int) (((now - trigTS) / 1000 ) / 60);
		
		if(elapsedMins < notifDesc.getDuration()) {
			
			String[] surveys = actDesc.getSurveys();
			for(int i = 0; i < surveys.length; i++) {
				
				if(IsSurveyTaken(context, surveys[i], 
							     trigTS - suppressMS)) {
					continue;
				}
				
				actSurveys.add(surveys[i]);
			}
		}
		
		return actSurveys;
	}
	
	private static boolean IsSurveyTaken(Context context, 
							      		 String survey, 
							      		 long since) {
		
		SharedPreferences pref = context.getSharedPreferences(
				 				NotifSurveyAdaptor.class.getName(), 
				 				Context.MODE_PRIVATE);
		
		if(!pref.contains(survey)) {
			return false;
		}
		
		if(pref.getLong(survey, 0) <= since) {
			return false;
		}
		
		return true;
	}

	public static Set<String> getAllActiveSurveys(Context context) {
		HashSet<String> actSurveys = new HashSet<String>();
	
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		Cursor c = db.getAllTriggers();
		if(c.moveToFirst()) {
			do {
				
				actSurveys.addAll(getActiveSurveys(context, c));
				
			} while(c.moveToNext());
		}
		c.close();
		db.close();
		
		return actSurveys;
	} 
	
	public static Set<String> getActiveSurveysForTrigger(Context context, 
														 int trigId) {
		HashSet<String> actSurveys = new HashSet<String>();
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		Cursor c = db.getTrigger(trigId);
		if(c.moveToFirst()) {
			actSurveys.addAll(getActiveSurveys(context, c));
		}
		
		c.close();
		db.close();
		
		return actSurveys;
	}
														 
	public static void recordSurveyTaken(Context context, String survey) {
			
		SharedPreferences pref = context.getSharedPreferences(
								 NotifSurveyAdaptor.class.getName(), 
								 Context.MODE_PRIVATE);
		
		SharedPreferences.Editor editor = pref.edit();
		editor.putLong(survey, System.currentTimeMillis());
		editor.commit();
	}
	
	/*
	private static void clearAllTimeStamps(Context context) {
		//Remove all time stamps
		SharedPreferences pref = context.getSharedPreferences(
				 NotifSurveyAdaptor.class.getName(), 
				 Context.MODE_PRIVATE);
		
		SharedPreferences.Editor editor = pref.edit();
		editor.clear();
		editor.commit();
	}
	*/
	
	private static void addTriggerInfoToArray(Cursor trig, JSONArray jArray) {
		String rtDesc = trig.getString(
				 		trig.getColumnIndexOrThrow(TriggerDB.KEY_RUNTIME_DESCRIPT));
		TriggerRunTimeDesc desc = new TriggerRunTimeDesc();
		desc.loadString(rtDesc);

		String notifDesc = trig.getString(
						   trig.getColumnIndexOrThrow(TriggerDB.KEY_NOTIF_DESCRIPT));
		
		String trigDesc = trig.getString(
						  trig.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_DESCRIPT));
		
		JSONObject jTrigInfo = new JSONObject();
		
		try {
			jTrigInfo.put(KEY_TRIGGER_DESC, new JSONObject(trigDesc));
			jTrigInfo.put(KEY_NOTIF_DESC, new JSONObject(notifDesc));
			jTrigInfo.put(KEY_RUNTIME_DESC, new JSONObject(desc.toHumanReadableString()));

		} catch (JSONException e) {
			return;
		}	
		
		jArray.put(jTrigInfo);
	}	
	
	
	public static JSONArray getActiveTriggerInfo(Context context, 
												  String survey) {
		JSONObject jInfo = new JSONObject();
		JSONArray jTrigs = new JSONArray();
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		Cursor c = db.getAllTriggers();
		if(c.moveToFirst()) {
			do {
				if(getActiveSurveys(context, c).contains(survey)) {
					addTriggerInfoToArray(c, jTrigs);
				}
				
			} while(c.moveToNext());
		}
		c.close();
		db.close();
		
		try {
			jInfo.put(KEY_ACTIVE_TRIGGERS, jTrigs);	
		} catch (JSONException e) {
			return null;
		}
		
		return jTrigs;
	}
	
	//TODO merge all the time receivers
	public static class NotifTimeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent i) {
			if(i.getAction().equals(Intent.ACTION_TIME_CHANGED) || 
			   i.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
						
				//TODO enable this later
				//clearAllTimeStamps(context);
			}
		}
		
	}
}
