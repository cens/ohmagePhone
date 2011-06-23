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
package edu.ucla.cens.andwellness.triggers.notif;


import java.util.List;
import java.util.Set;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.db.Campaign;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.triggers.base.TriggerBase;
import edu.ucla.cens.andwellness.triggers.base.TriggerDB;
import edu.ucla.cens.andwellness.triggers.utils.TrigPrefManager;

/*
 * The trigger notification manager. The logic which displays, repeats and
 * removes the home screen notifications are implemented here. Whenever a 
 * trigger goes off, the base calls the notifyNewTrigger provided by this 
 * class and passes the notification description as argument. This class 
 * displays the notification, sets up alarms to expire and repeat the 
 * notification. 
 * 
 * The notifications from all the triggers are summarized into one item
 * on the home screen. At any point in time, this notification item will 
 * display the list of all surveys which are active at that moment. 
 * Whenever a trigger expires, the list of surveys associated with that 
 * trigger are removed from the notification item and the surveys list 
 * in the item is updated with the rest of active surveys if any.  
 */
public class Notifier {

	private static final String DEBUG_TAG = "TriggerFramework";
	
	//TODO - This needs to be defined in a common place in order
	//make sure that it does not collide with any other notification
	//id in AndWellness
	private static final int NOIF_ID = 100;
	
	//Action of the intent which is broadcasted when the user
	//clicks on the notification
	private static final String ACTION_TRIGGER_NOTIFICATION = 
		"edu.ucla.cens.andwellness.triggers.TRIGGER_NOTIFICATION";
	//Action of the intent which is broadcasted when the notification
	//is updated by adding or removing surveys
	private static final String ACTION_ACTIVE_SURVEY_LIST_CHANGED = 
		"edu.ucla.cens.andwellness.triggers.SURVEY_LIST_CHANGED";
		
	private static final String ACTION_NOTIF_CLICKED = 
			"edu.ucla.cens.triggers.notif.Notifier.notification_clicked";
	private static final String ACTION_NOTIF_DELETED = 
			"edu.ucla.cens.triggers.notif.Notifier.notification_deleted";
	private static final String ACTION_EXPIRE_ALM = 
			"edu.ucla.cens.triggers.notif.Notifier.expire_notif";
	private static final String ACTION_REPEAT_ALM = 
			"edu.ucla.cens.triggers.notif.Notifier.repeat_notif";
	private static final String DATA_PREFIX_ALM = 
			"notifier://edu.ucla.cens.triggers.notif.Notifier/";
	
	private static final String KEY_TRIGGER_ID = 
			Notifier.class.getName() + ".trigger_id";
	private static final String KEY_REPEAT_LIST = 
			Notifier.class.getName() + ".repeat_list";
	
	private static final String KEY_NOTIF_VISIBILITY_PREF = 
			"notif_visibility";
	
	public static final String KEY_CAMPAIGN_URN = "campaign_urn";
	public static final String KEY_CAMPAIGN_NAME = "campaign_name";

	/*
	 * Utility function to save the status of the notification when it 
	 * is cleared from the home screen. The status is persistently stored
	 * using shared preferences. This status is used while the notification
	 * needs to be refreshed 'quietly'. If the notification is already hidden
	 * and it needs to be refreshed quietly (without alerting the user), no 
	 * action is required.  
	 */
	private static void hideNotification(Context context, String campaignUrn) {
		NotificationManager notifMan = (NotificationManager)context.getSystemService(
										Context.NOTIFICATION_SERVICE);
		
		notifMan.cancel(campaignUrn.hashCode());
		saveNotifVisibility(context, campaignUrn, false);
	}
	
	private static void displayNotification(Context context,
											String campaignUrn,
											String title,
											String summary,
											boolean quiet) {
		
		/*
		 * If the notification is to be refreshed quietly, and if it
		 * is hidden, do nothing. 
		 */
		if(quiet && !getNotifVisibility(context, campaignUrn)) {
			return;
		}
		
		NotificationManager notifMan = (NotificationManager)context.getSystemService(
										Context.NOTIFICATION_SERVICE);
		
		
		Notification notif = new Notification(R.drawable.survey_notification,
											  title, System.currentTimeMillis());

		if(!quiet) {
			notif.defaults = Notification.DEFAULT_ALL;
		}
		else {
			// If it is a quiet update, disable the ticker as well
			notif.tickerText = null;
		}
		
		//Watch for notification cleared events
		notif.deleteIntent = PendingIntent.getBroadcast(context, 0, 
											new Intent(ACTION_NOTIF_DELETED).putExtra(KEY_CAMPAIGN_URN, campaignUrn).putExtra(KEY_CAMPAIGN_NAME, title).setData(Uri.parse(DATA_PREFIX_ALM + campaignUrn)),
											PendingIntent.FLAG_CANCEL_CURRENT);
		
		//Watch for notification clicked events
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, 
											new Intent(ACTION_NOTIF_CLICKED).putExtra(KEY_CAMPAIGN_URN, campaignUrn).putExtra(KEY_CAMPAIGN_NAME, title).setData(Uri.parse(DATA_PREFIX_ALM + campaignUrn)),
											PendingIntent.FLAG_CANCEL_CURRENT);
		
		notif.setLatestEventInfo(context, title, summary, pi);
		notifMan.notify(campaignUrn.hashCode(), notif);
		
		//Save the current visibility
		saveNotifVisibility(context, campaignUrn, true);
	}
	
	/*
	 * Utility function to prepare a string of surveys from a list 
	 * of surveys. This function merely adds commas in between. 
	 */
	private static String getSurveyDisplayList(Set<String> surveys) {
		String ret = "";
	
		int i = 0;
		for(String survey : surveys) {
			ret += survey;
			
			i++;
			if(i < surveys.size()) {
				ret += ", ";
			}
		}
		
		return ret;
	}
	
	/*
	 * Refreshes the notification. The caller can specify if the user needs 
	 * to be alerted or the refresh needs to be done quietly. The user 
	 * can be alerted when there is a new trigger or when there is a repeat
	 * reminder. The notification can be refreshed quietly when a trigger 
	 * expires.
	 */
	public static void refreshNotification(Context context, String campaignUrn, boolean quiet) {
		
		Log.i(DEBUG_TAG, "Notifier: Refreshing notification, quiet = " + quiet);
		
		//Get the list of all the surveys active at the moment
		Set<String> actSurveys = NotifSurveyAdaptor.getAllActiveSurveys(context, campaignUrn);
		
		//Remove the notification if there are no active surveys
		if(actSurveys.size() == 0) {
			Log.i(DEBUG_TAG, "Notifier: No active surveys");
			hideNotification(context, campaignUrn);
		}
		else {
			//Prepare the message and display the notification
			
			String summary = "You have " + actSurveys.size() + 
							" survey" + (actSurveys.size() != 1 ? "s" : "") +
							" to take";
			
			DbHelper dbHelper = new DbHelper(context);
			
			//displayNotification(context, campaignUrn, title, getSurveyDisplayList(actSurveys), quiet);
			displayNotification(context, campaignUrn, dbHelper.getCampaign(campaignUrn).mName, summary, quiet);
		}
		
		//Send the broadcast indicating a change in the notification survey
		//list
		context.sendBroadcast(new Intent(ACTION_ACTIVE_SURVEY_LIST_CHANGED).putExtra(KEY_CAMPAIGN_URN, campaignUrn));
	}
	
	private static Intent getAlarmIntent(String action, int trigId) {
		Intent i = new Intent(action);
		i.setData(Uri.parse(DATA_PREFIX_ALM + trigId));
		i.putExtra(KEY_TRIGGER_ID, trigId);
		return i;
	}
	
	private static void cancelAllAlarms(Context context, int trigId) {
		AlarmManager alarmMan = (AlarmManager) context.getSystemService(
									Context.ALARM_SERVICE);

		Intent i = getAlarmIntent(ACTION_EXPIRE_ALM, trigId);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 
						   PendingIntent.FLAG_NO_CREATE);
		
		if(pi != null) {
			alarmMan.cancel(pi);
			pi.cancel();
		}
		
		i = getAlarmIntent(ACTION_REPEAT_ALM, trigId);
		pi = PendingIntent.getBroadcast(context, 0, i, 
			 PendingIntent.FLAG_NO_CREATE);
		
		if(pi != null) {
			alarmMan.cancel(pi);
			pi.cancel();
		}
	}
	
	private static void setAlarm(Context context, 
						  		 String action, 
						  		 int trigId, 
						  		 int mins,
						  		 Bundle extras) {
		
		Log.i(DEBUG_TAG, "Notifier: Setting alarm(" + trigId + 
						 ", " + mins + ", " + action + ")");
		
		AlarmManager alarmMan = (AlarmManager) 
								context.getSystemService(Context.ALARM_SERVICE);
		
		Intent i = getAlarmIntent(action, trigId);
		if(extras != null) {
			i.putExtras(extras);
		}
		
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 
						   PendingIntent.FLAG_CANCEL_CURRENT);

		long elapsed = mins * 60 * 1000;
		
		alarmMan.set(AlarmManager.RTC_WAKEUP,
					 System.currentTimeMillis() + elapsed, pi);
	}
	
	/*
	 * Set the alarm for a first item in the repeat reminder list
	 * of a trigger. The remaining list is bundled with the alarm
	 * intent. When alarm fires, this function is called again 
	 * with the repeat list obtained from the bundle until there
	 * are no items remaining in the list.
	 * 
	 * In order make this algorithm easier, this function accepts 
	 * the repeats as a list of their diffs. 
	 */
	private static void setRepeatAlarm(Context context, int trigId, 
									   int[] repeatDiffs) {
		
		if(repeatDiffs.length == 0) {
			//No more repeats in the list
			return;
		}
		
		//Set a repeat reminder for the first item in the list
		//and prepare the new list by removing this item
		
		int[] newRepeats = new int[repeatDiffs.length - 1];
		System.arraycopy(repeatDiffs, 1, newRepeats, 0, repeatDiffs.length -1);
		
		Bundle repeatBundle = new Bundle();
		repeatBundle.putIntArray(KEY_REPEAT_LIST, newRepeats);
		//Set the alarm for the first repeat item and attach the remaining list
		setAlarm(context, ACTION_REPEAT_ALM, trigId, 
				repeatDiffs[0], repeatBundle);
	}
	
	/*
	 * Restores the state of a notification such as repeat reminders and
	 * expiration timer for a specific trigger. This can be called at 
	 * bootup to restore the notification if it is still valid after
	 * bootup.
	 * 
	 * The expiration alarm is restored for the rest of the interval 
	 * calculated using the saved trigger time stamp. 
	 * 
	 * In the case of repeat reminder, only the remaining valid
	 * reminders are set. 
	 */
	public static void restorePastNotificationStates(Context context, 
								  		  	  		 int trigId, 
								  		  	  		 String notifDesc, 
								  		  	  		 long timeStamp) {
		
		NotifDesc desc = new NotifDesc();
		if(!desc.loadString(notifDesc)) {
			return;
		}
		
		//Cancel all the current alarms
		cancelAllAlarms(context, trigId);
		
		//if it hasn't expired yet, create a notif for the remaining time
		long now = System.currentTimeMillis();
		
		if(timeStamp > now || timeStamp < 0) {
			//TODO log
			return;
		}
		
		//Calculate the elapsed number of minutes for this trigger
		int elapsedMins = (int) (((now - timeStamp) / 1000 ) / 60);
		//Calculate the remaining duration for this trigger
		int remDuration = desc.getDuration() - elapsedMins;
		
		if(remDuration <= 0) {
			//The trigger expired
			return;
		}
		
		//Set an expire alarm for the remaining duration
		setAlarm(context, ACTION_EXPIRE_ALM, trigId, 
				 remDuration, null);
		
		//Set an alarm for the remaining repeats, if any
		List<Integer> repeats = desc.getSortedRepeats();	
		//Check if there is any repeat after the current time
		//Older repeats are to be discarded
		int i = 0;
		for(int repeat : repeats) {
			
			if(repeat > elapsedMins) {
				
				//There are repeats after the current time
				
				//Discard the older ones from the list
				int[] repeatDiffs = getRepeatDiffs(repeats.subList(i, repeats.size()));
				
				/* Subtract the elapsed time from the first repeat
				 * For instance, let the original repeat list be [5, 10, 15].
				 * Let's assume 7 minutes have elapsed. 
				 * So, the remaining list would be [10, 15] and the diff list 
				 * of this remaining list would be [10, 5]. Now, since 7 minutes
				 * have already elapsed, the first alarm should be set to fire
				 * after 3 minutes (10 - 7)
				 */
				
				repeatDiffs[0] -=  elapsedMins;
				
				setRepeatAlarm(context, trigId, repeatDiffs);
				break;
			}
			
			i++;
		}
	}
	
	/*
	 * Prepare the array of the differences of the repeats from 
	 * the repeat reminders list. The given list must be sorted.
	 */
	private static int[] getRepeatDiffs(List<Integer> repeatList) {
		
		int[] ret = new int[repeatList.size()];
		
		int i = 0;
		for(int repeat : repeatList) {
			
			if(i > 0) {
				ret[i] = (repeat - ret[i-1]);
			}
			else {
				ret[i] = repeat;
			}
			
			i++;
		}
		
		return ret;
	}
	
	/*
	 * Utility function to handle a repeat reminder alarm. Refreshes the
	 * notification and resets the repeat alarm if required.
	 */
	private static void repeatReminder(Context context, int trigId, Intent intent) {
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		String campaignUrn = db.getCampaignUrn(trigId); 
		db.close();
		
		Set<String> actSurveys = NotifSurveyAdaptor.getActiveSurveysForTrigger(context, 
																			   trigId);
		
		//Check if this trigger is still active. If not cancel all the alarms
		if(actSurveys.size() == 0) {
			cancelAllAlarms(context, trigId);
			return;
		}
		
		//Trigger is still active, alert the user
		refreshNotification(context, campaignUrn, false);
		//Continue the remaining repeat reminders
		int[] repeatDiffs = intent.getIntArrayExtra(KEY_REPEAT_LIST);
		setRepeatAlarm(context, trigId, repeatDiffs);
	}
	
	private static void handleNotifClicked(Context context, String campaignUrn) {
		//Hide the notification window when the user clicks on it
		hideNotification(context, campaignUrn);
		
		//Broadcast to Andwellness
		Intent i = new Intent(ACTION_TRIGGER_NOTIFICATION);
		i.putExtra(KEY_CAMPAIGN_URN, campaignUrn);
		context.sendBroadcast(i);
	}
	
	/*
	 * Save the visibility of the notification to preferences
	 */
	private static void saveNotifVisibility(Context context, String campaignUrn, boolean visible) {
		SharedPreferences pref = context.getSharedPreferences(
										 Notifier.class.getName() + "_" + campaignUrn, 
										 Context.MODE_PRIVATE);
		
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean(KEY_NOTIF_VISIBILITY_PREF, visible);
		editor.commit();
		
		TrigPrefManager.registerPreferenceFile(context, campaignUrn,
										Notifier.class.getName() + "_" + campaignUrn);
	}
	
	/*
	 * Get the current visibility of the notification
	 */
	private static boolean getNotifVisibility(Context context, String campaignUrn) {
		SharedPreferences pref = context.getSharedPreferences(
										 Notifier.class.getName() + "_" + campaignUrn, 
										 Context.MODE_PRIVATE);
		
		return pref.getBoolean(KEY_NOTIF_VISIBILITY_PREF, false);
	}
	

	private static void handleNotifDeleted(Context context, String campaignUrn) {
		saveNotifVisibility(context, campaignUrn, false);
	}
	
	private static void handleTriggerExpired(Context context, int trigId) {
	
		Log.i(DEBUG_TAG, "Notifier: Handling expiration alarm for: " 
				+ trigId);
		
		//Log information related to expired triggers.
		NotifSurveyAdaptor.handleExpiredTrigger(context, trigId);
		
		TriggerDB db = new TriggerDB(context);
		db.open();
		String campaignUrn = db.getCampaignUrn(trigId); 
		db.close();
		
		//Quietly refresh the notification
		Notifier.refreshNotification(context, campaignUrn, true);
	}
	
	/*
	 * Displays a new trigger notification. If the notification is
	 * already being displayed, the survey list is updated and the user
	 * is alerted.
	 */
	public static void notifyNewTrigger(Context context, 
							  		 	int trigId, 
							  		 	String notifDesc) {
		TriggerDB db = new TriggerDB(context);
		db.open();
		String campaignUrn = db.getCampaignUrn(trigId); 
		db.close();
		
		//Clear all existing alarms for this trigger if required
		cancelAllAlarms(context, trigId);
		//Update the notification with quite = false
		refreshNotification(context, campaignUrn, false);
		
		NotifDesc desc = new NotifDesc();
		if(!desc.loadString(notifDesc)) {	
			Log.e(DEBUG_TAG, "Notifier: Error parsing notif desc in " +
					"notifyNewTrigger()");
			return;
		}
		
		//Set an alarm to expire this trigger notif
		setAlarm(context, ACTION_EXPIRE_ALM, trigId, 
						desc.getDuration(), null);
		
		//Set an alarm for repeat reminder
		int[] repeatDiffs = getRepeatDiffs(desc.getSortedRepeats());
		setRepeatAlarm(context, trigId, repeatDiffs);
	}	

	public static void removeTriggerNotification(Context context, int trigId) {
		TriggerDB db = new TriggerDB(context);
		db.open();
		String campaignUrn = db.getCampaignUrn(trigId); 
		db.close();
		
		//Clear all existing alarms for this trigger if required
		cancelAllAlarms(context, trigId);
		refreshNotification(context, campaignUrn, true);
	}
	
	/* Receiver for all alarms */
	public static class NotifReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			if(intent.getAction().equals(ACTION_NOTIF_CLICKED)) {
				Notifier.handleNotifClicked(context, intent.getStringExtra(KEY_CAMPAIGN_URN));
			}
			else if(intent.getAction().equals(ACTION_NOTIF_DELETED)) {
				Notifier.handleNotifDeleted(context, intent.getStringExtra(KEY_CAMPAIGN_URN));
			}
			else if(intent.getAction().equals(ACTION_EXPIRE_ALM)) {
				
				Notifier.handleTriggerExpired(context, 
						intent.getIntExtra(KEY_TRIGGER_ID, -1));
			}
			else if(intent.getAction().equals(ACTION_REPEAT_ALM)) {
				
				if(!intent.hasExtra(KEY_TRIGGER_ID)) {
					return;
				}
				
				int trigId = intent.getIntExtra(KEY_TRIGGER_ID, -1);
				
				Notifier.repeatReminder(context, trigId, intent);
			}
		}

	}
}
