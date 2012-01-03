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
package org.ohmage.triggers.glue;

import java.util.Set;

import org.json.JSONArray;
import org.ohmage.triggers.base.TriggerInit;
import org.ohmage.triggers.notif.NotifSurveyAdaptor;
import org.ohmage.triggers.notif.Notifier;
import org.ohmage.triggers.ui.TriggerListActivity;

import android.content.Context;
import android.content.Intent;

/*
 * The glue layer between trigger framework and the 
 * Ohmage survey core.
 */
public class TriggerFramework {
	
	/*
	 * Action of the intent which is broadcasted when the user
	 * clicks on the notification.
	 * 
	 * This must be in sync with the action string in the notifier
	 * class 
	 */
	public static final String ACTION_TRIGGER_NOTIFICATION = 
					"org.ohmage.triggers.TRIGGER_NOTIFICATION";
	
	/*
	 * Action of the intent which is broadcasted when the list of 
	 * active surveys is changed (due to a new trigger, due to 
	 * expiration of a trigger etc).
	 * 
	 * This must be in sync with the action string in the notifier
	 * class 
	 */
	public static final String ACTION_ACTIVE_SURVEY_LIST_CHANGED = 
					"org.ohmage.triggers.SURVEY_LIST_CHANGED";
	
	/*
	 * Launch the activity which displays the main trigger list. This activity
	 * is the entry point to the trigger framework. The list of all surveys 
	 * for which any trigger is to be set must be passed as argument.
	 */
	public static Intent launchTriggersIntent(Context context, String campaignUrn, String[] surveys ) {
		
		Intent i = new Intent(context, TriggerListActivity.class);
		i.putExtra(TriggerListActivity.KEY_CAMPAIGN_URN, campaignUrn);
		i.putExtra(TriggerListActivity.KEY_ACTIONS, surveys);
		return i;
	}
	
	/*
	 * Launch the activity which displays the main trigger list.
	 * 
	 * Same as above, except that this variant allows an array of surveys to be
	 * passed which will be preselected when a new trigger is created from the
	 * trigger list activity.
	 */
	public static Intent launchTriggersIntent(Context context, String campaignUrn, String[] surveys, String[] selectedSurveys) {
		
		Intent i = launchTriggersIntent(context, campaignUrn, surveys);
		i.putExtra(TriggerListActivity.KEY_PRESELECTED_ACTIONS, selectedSurveys);
		return i;
	}
	
	/*
	 * Get the list of all surveys which are active currently.
	 */
	public static String[] getActiveSurveys(Context context, String campaignUrn) {
		
		Set<String> actSurveys = NotifSurveyAdaptor.getAllActiveSurveys(context, campaignUrn);
		
		return actSurveys.toArray(new String[actSurveys.size()]);
	}
		
	/*
	 * Tell the trigger framework that a survey has been taken. The framework
	 * will store the current time stamp against the survey name and it will
	 * be used to decide whether a trigger notification for that survey must
	 * be displayed or not.
	 */
	public static void notifySurveyTaken(Context context, String campaignUrn, String survey) {
		
		NotifSurveyAdaptor.recordSurveyTaken(context, campaignUrn, survey);
		//Quietly refresh the notification to remove the taken 
		//survey from the notification if applicable
		Notifier.refreshNotification(context, campaignUrn, true);
	}
	
	/*
	 * Get the JSON array of the details of all triggers which have 
	 * activated a given survey currently. 
	 */
	public static JSONArray getActiveTriggerInfo(Context context, String campaignUrn, String survey) {
		
		return NotifSurveyAdaptor.getActiveTriggerInfo(context, campaignUrn, survey);
	}
	
	/*
	 * Stops and deletes all triggers and resets all trigger related settings
	 * to default. 
	 * 
	 * Note: This API must be called only on the background and must not be
	 * called when any of the trigger related UI is being shown.
	 */
	public static boolean resetAllTriggerSettings(Context context) {
		
		return TriggerInit.resetAllTriggersAndSettings(context);
	}
	
	public static boolean resetTriggerSettings(Context context, String campaignUrn) {
		
		return TriggerInit.resetTriggersAndSettings(context, campaignUrn);
	}

	public static void setDefaultTriggers(Context context, String campaignUrn) {
		TriggerInit.addDefaultTriggers(context, campaignUrn);
	}
}

