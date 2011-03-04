package edu.ucla.cens.andwellness.triggers.notif;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import edu.ucla.cens.andwellness.triggers.base.TriggerActionDesc;
import edu.ucla.cens.andwellness.triggers.base.TriggerBase;
import edu.ucla.cens.andwellness.triggers.base.TriggerDB;
import edu.ucla.cens.andwellness.triggers.base.TriggerRunTimeDesc;
import edu.ucla.cens.andwellness.triggers.base.TriggerTypeMap;

public class NotifSurveyAdaptor {
	
	private static final String DEBUG_TAG = "TriggerFramework";
	private static final String SYSTEM_LOG_TAG = "TriggerFramework";
	
	private static final String KEY_ACTIVE_TRIGGERS = "active_triggers";
	private static final String KEY_TRIGGER_DESC = "trigger_description";
	private static final String KEY_NOTIF_DESC = "notification_description";
	private static final String KEY_RUNTIME_DESC = "runtime_description";
	private static final String KEY_TRIGGER_TYPE = "trigger_type";
	private static final String KEY_TRIGER_PREF = "trigger_preferences";
	private static final String KEY_SURVEY_LIST = "survey_list";
	private static final String KEY_UNTAKEN_SURVEYS = "surveys_not_taken";
	
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
			
		if(trigTS > now) {
			Log.w(DEBUG_TAG, "NotifSurveyAdaptor: Trigger time stamp is in the future!");
			return actSurveys;
		}
		
		long elapsedMS = now - trigTS;
		long durationMS = notifDesc.getDuration() * 60000;
		long suppressMS = notifDesc.getSuppression() * 60000;  

		if(elapsedMS < durationMS) {
			
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
	
	private static void addTriggerInfoToArray(Context context, 
			 								  Cursor trig, JSONArray jArray) {
		String rtDesc = trig.getString(
				 		trig.getColumnIndexOrThrow(TriggerDB.KEY_RUNTIME_DESCRIPT));
		TriggerRunTimeDesc desc = new TriggerRunTimeDesc();
		desc.loadString(rtDesc);

		String notifDesc = trig.getString(
						   trig.getColumnIndexOrThrow(TriggerDB.KEY_NOTIF_DESCRIPT));
		
		String trigDesc = trig.getString(
						  trig.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_DESCRIPT));
		
		String trigType = trig.getString(
						  trig.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_TYPE));
		
		JSONObject jPref = new JSONObject();
		TriggerBase trigBase = new TriggerTypeMap().getTrigger(trigType);
		if(trigBase != null) {
			jPref = trigBase.getPreferences(context);
		}
		
		JSONObject jTrigInfo = new JSONObject();
		
		try {
			jTrigInfo.put(KEY_TRIGGER_TYPE, trigType);
			jTrigInfo.put(KEY_TRIGGER_DESC, new JSONObject(trigDesc));
			jTrigInfo.put(KEY_NOTIF_DESC, new JSONObject(notifDesc));
			jTrigInfo.put(KEY_RUNTIME_DESC, new JSONObject(desc.toHumanReadableString()));
			jTrigInfo.put(KEY_TRIGER_PREF, jPref);

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
					addTriggerInfoToArray(context, c, jTrigs);
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
	
	private static void systemLog(Context context, String msg) {
		
		edu.ucla.cens.systemlog.Log.i(SYSTEM_LOG_TAG, msg);
	}
	
	public static void handleExpiredTrigger(Context context, int trigId) {
		TriggerDB db = new TriggerDB(context);
		db.open();
		
		String sActDesc = db.getActionDescription(trigId);
		String sTrigDesc = db.getTriggerDescription(trigId);
		String sTrigType = db.getTriggerType(trigId);
		String sRTDesc = db.getRunTimeDescription(trigId);
		
		db.close();
		
		if(sActDesc == null || 
		   sTrigDesc == null || 
		   sTrigType == null || 
		   sRTDesc == null) {
			
			return;
		}
		
		TriggerActionDesc actDesc = new TriggerActionDesc();
		if(!actDesc.loadString(sActDesc)) {
			return;
		}
		
		TriggerRunTimeDesc rtDesc = new TriggerRunTimeDesc();
		if(!rtDesc.loadString(sRTDesc)) {
			return;
		}
		
		LinkedList<String> untakenList = new LinkedList<String>();
		for(String survey: actDesc.getSurveys()) {
			
			if(!IsSurveyTaken(context, survey, 
					rtDesc.getTriggerTimeStamp())) {
				
				untakenList.add(survey);
			}
		}
		
		if(untakenList.size() == 0) {
			return;
		}
		
		JSONArray jSurveyList = new JSONArray();
		for(String survey : actDesc.getSurveys()) {
			jSurveyList.put(survey);
		}
		
		JSONArray jUntakenSurveys = new JSONArray();
		for(String unTakenSurvey : untakenList) {
			jUntakenSurveys.put(unTakenSurvey);
		}
		
		JSONObject jExpired = new JSONObject();
		
		try {
			jExpired.put(KEY_TRIGGER_TYPE, sTrigType);
			jExpired.put(KEY_TRIGGER_DESC, new JSONObject(sTrigDesc));
			jExpired.put(KEY_SURVEY_LIST, jSurveyList);
			jExpired.put(KEY_UNTAKEN_SURVEYS, jUntakenSurveys);
		} catch (JSONException e) {
			return;
		}
		
		//Log the info
		String msg = "Expired trigger has surveys not taken: " + 
					 jExpired.toString();
		
		Log.i(DEBUG_TAG, "NotifSurveyAdaptor: SystemLogging the following message: ");
		Log.i(DEBUG_TAG, msg);
		
		systemLog(context, msg);
	}
}
