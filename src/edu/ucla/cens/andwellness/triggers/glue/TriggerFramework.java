package edu.ucla.cens.andwellness.triggers.glue;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import edu.ucla.cens.andwellness.triggers.notif.NotifSurveyAdaptor;
import edu.ucla.cens.andwellness.triggers.notif.Notifier;
import edu.ucla.cens.andwellness.triggers.ui.TriggerListActivity;

/*
 * The glue layer between trigger framework and the 
 * survey core.
 */
public class TriggerFramework {
	
	/*
	 * This must be in sync with the action string in the notifier
	 * class 
	 */
	public static final String ACTION_TRIGGER_NOTIFICATION = 
					"edu.ucla.cens.andwellness.triggers.TRIGGER_NOTIFICATION";
	
	/*
	 * This must be in sync with the action string in the notifier
	 * class 
	 */
	public static final String ACTION_ACTIVE_SURVEY_LIST_CHANGED = 
					"edu.ucla.cens.andwellness.triggers.SURVEY_LIST_CHANGED";
	
	public static void launchTriggersActivity(Context context, String[] surveys ) {
		
		Intent i = new Intent(context, TriggerListActivity.class);
		i.putExtra(TriggerListActivity.KEY_ACTIONS, surveys);
		context.startActivity(i);
	}
	
	public static String[] getActiveSurveys(Context context) {
		
		Set<String> actSurveys = NotifSurveyAdaptor.getAllActiveSurveys(context);
		
		return actSurveys.toArray(new String[actSurveys.size()]);
	}
		
	public static void notifySurveyTaken(Context context, String survey) {
		
		NotifSurveyAdaptor.recordSurveyTaken(context, survey);
		//Quietly refresh the notification
		Notifier.refreshNotification(context, true);
	}
	
	public static JSONArray getActiveTriggerInfo(Context context, String survey) {
		
		return NotifSurveyAdaptor.getActiveTriggerInfo(context, survey);
	}
}

