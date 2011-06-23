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
package edu.ucla.cens.andwellness.triggers.types.time;

import java.util.Calendar;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import edu.ucla.cens.andwellness.triggers.utils.SimpleTime;

public class TimeTrigService extends Service {

	private static final String DEBUG_TAG = "TimeTrigger";
	
	public static final String WAKE_LOCK_NAME =
		"edu.ucla.cens.andwellness.triggers.types.timeTimeTrigService.wake_lock";	
	
	public static final String ACTION_HANDLE_TRIGGER = "handle_alarm";
	public static final String ACTION_SET_TRIGGER = "set_trigger";
	public static final String ACTION_REMOVE_TRIGGER = "remove_trigger";
	public static final String ACTION_RESET_TRIGGER = "reset_trigger";
	public static final String KEY_TRIG_ID = "trigger_id";
	public static final String KEY_TRIG_DESC = "trigger_desc";
	
	private static final String ACTION_TRIG_ALM = 
		"edu.ucla.cens.triggers.types.time.TimeTriggerAlarm";
	private static final String DATA_PREFIX_TRIG_ALM = 
		"timetrigger://edu.ucla.cens.triggers.types.time/";
	
	private AlarmManager mAlarmMan = null;
	private static PowerManager.WakeLock mWakeLock = null;
	
    @Override
	public void onCreate() {
    	super.onCreate();
    	
    	Log.i(DEBUG_TAG, "TimeTriggerService: onCreate");
    	
    	mAlarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
	}
    
    @Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		Log.i(DEBUG_TAG, "TimeTriggerService: onStart");
		
		String action = intent.getAction();
		if(action == null ||
		   !intent.hasExtra(KEY_TRIG_ID) ||
		   !intent.hasExtra(KEY_TRIG_DESC)) {
			
			Log.w(DEBUG_TAG, "TimeTriggerService: Started with invalid intent");
			
			releaseWakeLock();
			return;
		}
		
		int trigId = intent.getIntExtra(KEY_TRIG_ID, -1);
		String trigDesc = intent.getStringExtra(KEY_TRIG_DESC);		
		
		if(action.equals(ACTION_HANDLE_TRIGGER)) {
			Log.i(DEBUG_TAG, "TimeTriggerService: Handling trigger "
						+ trigId);
			
			//Notify user
			new TimeTrigger().notifyTrigger(this, trigId);
			//repeat the alarm
			setTrigger(trigId, trigDesc);
		}
		else if(action.equals(ACTION_SET_TRIGGER)) {
			Log.i(DEBUG_TAG, "TimeTriggerService: Setting trigger "
					+ trigId);
			
			setTrigger(trigId, trigDesc);
		}
		else if(action.equals(ACTION_REMOVE_TRIGGER)) {
			Log.i(DEBUG_TAG, "TimeTriggerService: Removing trigger "
					+ trigId);
			
			removeTrigger(trigId, trigDesc);
		}
		else if(action.equals(ACTION_RESET_TRIGGER)) {
			Log.i(DEBUG_TAG, "TimeTriggerService: Resetting trigger "
					+ trigId);
			
			removeTrigger(trigId, trigDesc);
			setTrigger(trigId, trigDesc);
		}
		
		releaseWakeLock();
	}
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	releaseWakeLock();
    }
    
    private static void acquireWakeLock(Context context) {
		
    	if(mWakeLock == null) {
			PowerManager powerMan = (PowerManager) context.
									getSystemService(POWER_SERVICE);
			
			mWakeLock = powerMan.newWakeLock(
	    				PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_NAME);
			mWakeLock.setReferenceCounted(true);
    	}
    	
		if(!mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}
	}
	
	private static void releaseWakeLock() {
	
		if(mWakeLock == null) {
			return;
		}
		
		if(mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}
   
    private Intent createAlarmIntent(int trigId, String trigDesc) {
    	Intent i = new Intent();
    	
    	i.setAction(ACTION_TRIG_ALM);
    	i.setData(Uri.parse(DATA_PREFIX_TRIG_ALM + trigId));
    	i.putExtra(KEY_TRIG_ID, trigId);
    	i.putExtra(KEY_TRIG_DESC, trigDesc);
    	return i;
    }
    
    private Calendar getTriggerTimeForToday(int trigId, TimeTrigDesc trigDesc) {
    	
    	TimeTrigger timeTrig = new TimeTrigger();
    	if(timeTrig.hasTriggeredToday(this, trigId)) {
    		return null;
    	}
    	
    	Calendar now = Calendar.getInstance();
    	
    	Calendar target = Calendar.getInstance();
    	target.set(Calendar.SECOND, 0);
    	
    	if(!trigDesc.isRandomized()) {
    		target.set(Calendar.HOUR_OF_DAY, trigDesc.getTriggerTime().getHour());
	    	target.set(Calendar.MINUTE, trigDesc.getTriggerTime().getMinute());
    	
	    	if(now.before(target)) {
	    		return target;
	    	}
		}
    	else { //if randomized, check if there is any more time left in the interval
			SimpleTime tCurr = new SimpleTime();
			SimpleTime tStart = trigDesc.getRangeStart();
			SimpleTime tEnd = trigDesc.getRangeEnd();
			
			if(tCurr.isBefore(tEnd)) {
				
				int diff;
				if(tCurr.isAfter(tStart)) {
					diff = tCurr.differenceInMinutes(tEnd);
					target.set(Calendar.HOUR_OF_DAY, tCurr.getHour());
	    	    	target.set(Calendar.MINUTE, tCurr.getMinute());
				}
				else {
					diff = tStart.differenceInMinutes(tEnd);
					target.set(Calendar.HOUR_OF_DAY, tStart.getHour());
	    	    	target.set(Calendar.MINUTE, tStart.getMinute());
				}
			
				Random rand = new Random();
		    	//Generate a random number (both ranges inclusive)
		    	target.add(Calendar.MINUTE, rand.nextInt(diff + 1));
		    	return target;
			}
    	}
    	
    	return null;
    }
    
    private Calendar getTriggerTimeForDay(int trigId, TimeTrigDesc trigDesc, 
    										int dayOffset) {
    	
    	Calendar target = Calendar.getInstance();
    	target.add(Calendar.DAY_OF_YEAR, dayOffset);
    
    	String dayStr = TimeTrigDesc.getDayOfWeekString(
    						target.get(Calendar.DAY_OF_WEEK));
    	
    	if(!trigDesc.doesRepeatOnDay(dayStr)) {
			return null;
		}
    	
    	if(dayOffset == 0) {
    		return getTriggerTimeForToday(trigId, trigDesc);
    	}

    	target.set(Calendar.SECOND, 0);
    	if(!trigDesc.isRandomized()) {
    		target.set(Calendar.HOUR_OF_DAY, trigDesc.getTriggerTime().getHour());
	    	target.set(Calendar.MINUTE, trigDesc.getTriggerTime().getMinute());
    	}
    	else {
    		target.set(Calendar.HOUR_OF_DAY, trigDesc.getRangeStart().getHour());
	    	target.set(Calendar.MINUTE, trigDesc.getRangeStart().getMinute());
	    	
			int diff = trigDesc.getRangeStart()
								.differenceInMinutes(trigDesc.getRangeEnd());
			Random rand = new Random();
	    	//Generate a random number (both ranges inclusive)
	    	target.add(Calendar.MINUTE, rand.nextInt(diff + 1));
    	}
    	
    	return target;
    }
     
    private long getAlarmTimeInMillis(int trigId, TimeTrigDesc trigDesc) {
    	
    	for(int i = 0; i <= 7; i++) {
    		
    		Calendar target = getTriggerTimeForDay(trigId, trigDesc, i);
    		if(target != null) {
    			Log.i(DEBUG_TAG, "TimeTriggerService: Calculated target time: " +
  					  target.getTime().toString());
    			
    			return target.getTimeInMillis();
    		}
    	}
    	
		Log.w(DEBUG_TAG, "TimeTriggerService: No valid day of " +
				"the week found!");
    	
    	//Must not reach here
		return -1;
    }
    
    private void cancelAlarm(int trigId, String trigDesc) {
    	
    	Intent i = createAlarmIntent(trigId, trigDesc);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
				   		   	  PendingIntent.FLAG_NO_CREATE);
		
		if(pi != null) {
			//remove the pending intent
			Log.i(DEBUG_TAG, "TimeTriggerService: Canceling the pending" +
			" intent and alarm for id: " + trigId);
	
			mAlarmMan.cancel(pi);
			pi.cancel();
		}
    }
    
    private void setAlarm(int trigId, TimeTrigDesc desc) {
    	
    	//Cancel the pending intent and the existing alarm first
    	cancelAlarm(trigId, desc.toString());

		Log.i(DEBUG_TAG, "TimeTriggerService: Attempting to set trigger " 
				+ trigId);
		
		Intent i = createAlarmIntent(trigId, desc.toString());
	    PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
	    				   PendingIntent.FLAG_CANCEL_CURRENT);

	    long alarmTime = getAlarmTimeInMillis(trigId, desc);
	    if(alarmTime == -1) {
	    	Log.i(DEBUG_TAG, "TimeTriggerService: No valid time found for " 
					+ trigId);
	    	return;
	    }
	    
	    /* Convert the alarm time to elapsed real time. 
	     * If we dont do this, a time change in the system might
	     * set off all the alarms and a trigger might go off before
	     * we get a chance to cancel it
	     */
	    long elapsedRT = alarmTime - System.currentTimeMillis();
	    if(elapsedRT <= 0) {
	    	Log.i(DEBUG_TAG, "TimeTriggerService: negative elapsed realtime - "
	    			+ "alarm not setting: "
					+ trigId);
	    	return;
	    }
	    
	    Log.i(DEBUG_TAG, "TimeTriggerService: Setting alarm for " + elapsedRT
	    				 + " millis into the future");
	    
		mAlarmMan.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
					 SystemClock.elapsedRealtime() + elapsedRT, pi);
    }
    
    private void setTrigger(int trigId, String trigDesc) {
    
    	Log.i(DEBUG_TAG, "TimeTriggerService: Attempting to set " +
    			"the trigger: " + trigId);
		
    	TimeTrigDesc desc = new TimeTrigDesc();
    	if(desc.loadString(trigDesc)) {
    		setAlarm(trigId, desc);
    	}
    	else {
    		Log.i(DEBUG_TAG, "TimeTriggerService: Failed to parse" +
    				" trigger config: id = " + trigId);
    	}
    }
    
    private void removeTrigger(int trigId, String trigDesc) {
    	cancelAlarm(trigId, trigDesc);
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		
		return null;
	}
	

	 /* Receiver for alarms */
	public static class AlarmReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			
			Log.i(DEBUG_TAG, "TimeTriggerService: Recieved broadcast");
			
			if(intent.getAction().equals(ACTION_TRIG_ALM)) {
				
				Log.i(DEBUG_TAG, "TimeTriggerService: Handling alarm event");
				
				acquireWakeLock(context);
				
				Intent i = new Intent(context, TimeTrigService.class);
				
				i.setAction(ACTION_HANDLE_TRIGGER);
				i.replaceExtras(intent);
				context.startService(i);
			}
		}
	}
}
