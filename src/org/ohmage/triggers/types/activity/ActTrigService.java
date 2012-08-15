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

/*
 * Authored by: Ankit Gupta. for questions, please email: agupta423@gmail.com
 * Adapted from Location Triggers and Time Triggers by: Kannan Parameswaran
 * 
 */


package org.ohmage.triggers.types.activity;


import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.ohmage.db.DbHelper;
import org.ohmage.db.Models.Campaign;
import org.ohmage.triggers.config.ActTrigConfig;
import org.ohmage.triggers.utils.SimpleTime;


import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Log;
import edu.ucla.cens.systemlog.Analytics.Status;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

public class ActTrigService extends Service {

	private static final String TAG = "ActTrigService";
	
	private static final String WAKE_LOCK_TAG = 
			ActTrigService.class.getName() + ".wake_lock";
	private static final String RECR_WAKE_LOCK_TAG = 
			ActTrigService.class.getName() + ".recr_wake_lock";
	public static final String ACTION_START_TRIGGER = 
			ActTrigService.class.getName() + ".start_trigger";
	public static final String ACTION_REMOVE_TRIGGER = 
			ActTrigService.class.getName() + ".stop_trigger";
	public static final String ACTION_RESET_TRIGGER = 
			ActTrigService.class.getName() + ".reset_trigger";
	public static final String ACTION_HANDLE_ALARM = 
			ActTrigService.class.getName() + ".handle_alarm";
	
	
	//Alarm actions
	private static final String ACTION_ALRM_WAKEUP_CHECK = 
			ActTrigService.class.getName() +".WAKEUP_CHECK";
	private static final String ACTION_ALRM_TRIGGER_ALWAYS = 
			ActTrigService.class.getName() +".TRIGGER_ALWAYS";
	private static final String ACTION_ALRM_COUNT_TIMEOUT = 
			ActTrigService.class.getName() +".COUNT_TIMEOUT";
	private static final String DATA_PREFIX_TRIG_ALWAYS_ALM = 
			"activitytrigger://edu.ucla.cens.triggers.types.activity/";
	
	public static final String KEY_TRIG_ID = "trigger_id";
	public static final String KEY_TRIG_DESC = "trigger_description";
	private static final String KEY_ALARM_ACTION = "alarm_action";
	private static final String KEY_SAMPLING_ALARM_EXTRA = "alarm_extra";
	
	private ContentResolver mResolver;
	private String MOBILITY = "mobility";
	private Uri uriMobility;
	private static final long COUNT_TIMEOUT = ActTrigConfig.COUNT_TIMEOUT; // 1min.
	private static final long FIVE_MIN = 1000L * 60L * 5L;
	private static final long TWO_MIN = 1000L * 60L * 2L;
	private Set<Integer> openTrigSet;
	private Set<Integer> closedTrigSet;
	
	private static boolean triggerOnceClosedTimeRange = ActTrigConfig.TRIGGER_ONCE_CLOSED_TIME_RANGE;
	private static boolean triggerOnceOpenTimeRange = ActTrigConfig.TRIGGER_ONCE_OPEN_TIME_RANGE;
	private static int OPEN_TIME_RANGE_SLEEP_HOUR = ActTrigConfig.OPEN_TIME_RANGE_SLEEP_HOUR;
	private static int OPEN_TIME_RANGE_SLEEP_MINUTE = ActTrigConfig.OPEN_TIME_RANGE_SLEEP_MINUTE;
	private static int OPEN_TIME_RANGE_WAKEUP_DEFAULT_HOUR = ActTrigConfig.OPEN_TIME_RANGE_WAKEUP_DEFAULT_HOUR;
	private static int OPEN_TIME_RANGE_WAKEUP_DEFAULT_MINUTE = ActTrigConfig.OPEN_TIME_RANGE_WAKEUP_DEFAULT_MINUTE;
	
	private static int WAKEUP_SCAN_HOUR = ActTrigConfig.WAKEUP_SCAN_HOUR;
	private static int WAKEUP_SCAN_MINUTE = ActTrigConfig.WAKEUP_SCAN_MINUTE;
	private static long WAKEUP_SCAN_INTERVAL = ActTrigConfig.WAKEUP_SCAN_INTERVAL; //1 hour
	
												
	
	//Wake lock for service
	private PowerManager.WakeLock mWakeLock = null;
	//Wake lock for alarm receiver
	private static PowerManager.WakeLock mRecvrWakeLock = null;
	

	
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate(){
		
		//initState() -- initialize any constants or anything like that.
		
		
		PowerManager powerMan = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = powerMan.newWakeLock(
    				PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
		mWakeLock.acquire();
		
		
	
		
		Uri.Builder builder = new Uri.Builder();
    	builder.scheme("content");
    	builder.authority("edu.ucla.cens.mobility.MobilityContentProvider");
    	builder.path(MOBILITY);
		uriMobility = builder.build();
		
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		Log.d(TAG, "onStartCommand()");
		int trigId = intent.getIntExtra(KEY_TRIG_ID, -1);
		String trigDesc = intent.getStringExtra(KEY_TRIG_DESC);
		
		if(intent.getAction().equals(ACTION_START_TRIGGER)) {
			setTriggerAlwaysAlarm(trigId, trigDesc);
			updateSamplingStatus();
		}
		else if(intent.getAction().equals(ACTION_REMOVE_TRIGGER)) {
			cancelTriggerAlwaysAlarm(trigId);
			updateSamplingStatus();
		}
		else if(intent.getAction().equals(ACTION_RESET_TRIGGER)) {
			setTriggerAlwaysAlarm(trigId, trigDesc);
			updateSamplingStatus();
		}
		else if(intent.getAction().equals(ACTION_HANDLE_ALARM)) {
			Log.d(TAG, "service started with HANDLE_ALARM intent action");
			handleAlarm(intent.getExtras());
		}

		 
		//-- check if any timeperiods to watch for or if already sampling, and somewhere from there set new alarm for future.
		//releaseWakeLock() here too?
		releaseRecvrWakeLock();
		
		return START_NOT_STICKY;

	}
	
	
	@Override
	public void onDestroy() {
		Analytics.service(this, Status.OFF);
		
		releaseWakeLock();
		releaseRecvrWakeLock();
		
		super.onDestroy();
	}
	
	
	
	
	private void handleAlarm(Bundle extras) {
		
		String alm = extras.getString(KEY_ALARM_ACTION);
		Log.d(TAG, "alarm action is: " + alm);
		if(alm.equals(ACTION_ALRM_TRIGGER_ALWAYS)) {
			Log.d(TAG, "handleAlarm called with ALRM_TRIGGER_ALWAYS");
			handleTriggerAlwaysAlarm(extras.getInt(KEY_TRIG_ID));
			updateSamplingStatus();
		}
		else if(alm.equals(ACTION_ALRM_COUNT_TIMEOUT)) {
			updateSamplingStatus();
		}
		else if(alm.equals(ACTION_ALRM_WAKEUP_CHECK)){
			if (checkAwake()){
				turnOnTriggers();
				updateSamplingStatus();
			}
			else{
				setSamplingAlarm(ACTION_ALRM_WAKEUP_CHECK , WAKEUP_SCAN_INTERVAL , 0);
			}
		}
	}
	
	private boolean checkAwake(){
		Calendar instance = Calendar.getInstance();
		if (instance.get(Calendar.HOUR) >= OPEN_TIME_RANGE_WAKEUP_DEFAULT_HOUR && instance.get(Calendar.MINUTE) >= OPEN_TIME_RANGE_WAKEUP_DEFAULT_MINUTE){
			
			return true;
		}
		instance.set(Calendar.HOUR , WAKEUP_SCAN_HOUR);
		instance.set(Calendar.MINUTE, WAKEUP_SCAN_MINUTE);
		instance.set(Calendar.SECOND, 0);
		
		mResolver = getContentResolver();
		long start = instance.getTimeInMillis();
		String[] projection = new String[]{"time", "mode"} ;
		String selection = "time >= '" + start +"'";
		String sortOrder = "time DESC"; //latest to earliest. for counting backwards
		Cursor c = mResolver.query(uriMobility, projection, selection, null, sortOrder);
		Log.d(TAG, "Cursor count: " + c.getCount());
		long oppTime = 0L;
		boolean gettinShifty = false;
		long elapsedTime = 0L;
		long currentTime = System.currentTimeMillis();
		long previousTime = System.currentTimeMillis();
		
		if (c.moveToFirst()){
			do{
				currentTime = c.getLong(0);
				int mode = modeToIntWakeupCheck(c.getString(1));
				long difference = previousTime - currentTime;
				if (mode == 1 && !gettinShifty){
					elapsedTime += difference;
					oppTime = 0L;
					gettinShifty = true;
				}
				else if (mode == 1 && gettinShifty){
					elapsedTime += difference;
				}
				else if (mode == 0 && gettinShifty){
					oppTime += difference;
					
				}
				else if (mode == 0 && !gettinShifty){
					
				}
				
				if (oppTime > FIVE_MIN * 2){
					elapsedTime = 0L;
					gettinShifty = false;
				}
				if (elapsedTime > TWO_MIN - 5000){
					
					return true;
				}
				
				previousTime = currentTime;
			}while (c.moveToNext());
		}
		return false;
	}
	
	private void turnOnTriggers(){
		LinkedList<Integer> actTrigs = new LinkedList<Integer>();
		ActivityTrigger at = new ActivityTrigger();
		SimpleTime now  = new SimpleTime();
		DbHelper dbHelper = new DbHelper(this);
		Calendar instance = Calendar.getInstance();
		String dayStr = ActTrigDesc.getDayOfWeekString(
				instance.get(Calendar.DAY_OF_WEEK));
		for (Campaign c : dbHelper.getReadyCampaigns()) {
			Log.d(TAG, "adding all active trigs from campaign: " + c.toString());
			actTrigs.addAll(at.getAllActiveTriggerIds(this, c.mUrn));
		}
		for (Integer i: actTrigs){
			ActTrigDesc desc = new ActTrigDesc();
			desc.loadString(at.getTrigger(this, i));
			if (desc.doesRepeatOnDay(dayStr)){
				updateTriggerStart(i , now , true);
			}
		}
	}
	
	
	private void updateSamplingStatus() {
		openTrigSet = new TreeSet<Integer>();
		closedTrigSet = new TreeSet<Integer>();
		Log.d(TAG, "updateSamplingStatus");
		LinkedList<Integer> actTrigs = new LinkedList<Integer>();
		ActivityTrigger at = new ActivityTrigger();
		
		DbHelper dbHelper = new DbHelper(this);
		for (Campaign c : dbHelper.getReadyCampaigns()) {
			Log.d(TAG, "adding all active trigs from campaign: " + c.toString());
			actTrigs.addAll(at.getAllActiveTriggerIds(this, c.mUrn));
		}
		
		
		if((actTrigs.size() > 0) && timeRangeCheck(actTrigs)) {
			Log.d(TAG, "actTrigs and timeRangeCheck are true, setting sampling alarm");
			this.setSamplingAlarm(ACTION_ALRM_COUNT_TIMEOUT, COUNT_TIMEOUT, 0);
			if (areAllOpenTrigsOffForToday(openTrigSet)){
				//set wakeup alarm.
			}
		}
		else if (actTrigs.size() > 0) {
			Log.d(TAG, "timeRangeCheck false, setting alarm for beginning of next alarm.");
			this.setAlarmForSampleNextStartRangeOfTrigger(closedTrigSet);
			if (areAllOpenTrigsOffForToday(openTrigSet)){
				this.setWakeupAlarm(openTrigSet);
			}
			stopSampling();
		}
		else{
			Log.d(TAG, "no active trigs, timeRangeCheck false, cancelling all alarms");
			cancelAllSamplingAlarms();
			stopSampling();
		}
	}
	
	private boolean areAllOpenTrigsOffForToday(Set<Integer> openTrigIds){
		if (openTrigIds.size() == 0){
			return false;
		}
		ActivityTrigger actTrig = new ActivityTrigger();
		for (Integer id: openTrigIds){
			ActTrigDesc desc = new ActTrigDesc();
			desc.loadString(actTrig.getTrigger(this, id));
			if (desc.getSwitch()){
				return false;
			}
		}
		return true;
		
	}
	
	private boolean timeRangeCheck(LinkedList<Integer> activeTriggers){
		Calendar instance = Calendar.getInstance();
		boolean didCheck = false;
		ActivityTrigger actTrig = new ActivityTrigger();
		String dayStr = ActTrigDesc.getDayOfWeekString(
				instance.get(Calendar.DAY_OF_WEEK));
		for (Integer trigId: activeTriggers){
			
			ActTrigDesc desc = new ActTrigDesc();
			Log.d(TAG, "loading desc for trig num: " + trigId);
			if(!desc.loadString(actTrig.getTrigger(this, trigId))) {
				Log.d(TAG, "didn't load properly");
				continue;
			}
			boolean closedTimeRange = desc.isRangeEnabled();
			if (closedTimeRange){
				//add to closedTrigList
				closedTrigSet.add(trigId);
				
				//check if repeats today
				if (!desc.doesRepeatOnDay(dayStr)){
					continue;
				}
				
				//if currently in the time range
				SimpleTime now = new SimpleTime();
				SimpleTime startTime = desc.getStartTime();
				if(now.isBefore(startTime)) {
					continue;
				}
				
				SimpleTime endTime = desc.getEndTime();
				if(now.isAfter(endTime)) {
					continue;
				}
//				else if(now.equals(endTime) && instance.get(Calendar.SECOND) > 0){
//					continue;
//				}
				
				//has Trigged today
					//if so update timestamp
				if (actTrig.hasTriggeredToday(this, trigId)){
					if (triggerOnceClosedTimeRange){
						continue;
					}
					Calendar temp = Calendar.getInstance();
					temp.setTimeInMillis(actTrig.getTriggerLatestTimeStamp(this, trigId));
					startTime = new SimpleTime(temp.get(Calendar.HOUR_OF_DAY), temp.get(Calendar.MINUTE));
					
				}
				
				//trigger if necessary and didCheck = true
				this.triggerIfRequired(trigId, startTime, desc.getState(), desc.getDuration(), false);
				didCheck = true;
				
			}
			else{
				//add to openTrigList
				openTrigSet.add(trigId);
				//check if on or off
				if (!desc.getSwitch()){
					continue;
				}
				//repeats today
						//if not, turn off, and continue
				if (!desc.doesRepeatOnDay(dayStr)){
					this.updateTriggerStart(
							trigId, 
							new SimpleTime(OPEN_TIME_RANGE_WAKEUP_DEFAULT_HOUR , OPEN_TIME_RANGE_WAKEUP_DEFAULT_MINUTE), 
							false);
					continue;
				}
				
				//if duration will go over sleep time
						//if so, turn off, and continue
				SimpleTime startTime = desc.getStartTime();
				int minIntoDay = startTime.getHour() * 60 + startTime.getMinute() + desc.getDurationMinTotal();
				SimpleTime earliestTrigTime = new SimpleTime(minIntoDay / 60 , minIntoDay % 60);
				if (earliestTrigTime.isAfter(new SimpleTime(OPEN_TIME_RANGE_SLEEP_HOUR , OPEN_TIME_RANGE_SLEEP_MINUTE))){
					this.updateTriggerStart(
							trigId, 
							new SimpleTime(OPEN_TIME_RANGE_WAKEUP_DEFAULT_HOUR , OPEN_TIME_RANGE_WAKEUP_DEFAULT_MINUTE) , 
							false);
					
					continue;
				}
				
				if (triggerOnceOpenTimeRange && actTrig.hasTriggeredToday(this, trigId)){
					this.updateTriggerStart(
							trigId, 
							new SimpleTime(OPEN_TIME_RANGE_WAKEUP_DEFAULT_HOUR , OPEN_TIME_RANGE_WAKEUP_DEFAULT_MINUTE) , 
							false);
					
					continue;
				}
				
				this.triggerIfRequired(trigId, startTime, desc.getState(), desc.getDuration(), true);
				didCheck = true;
				
			}
			
			
		}
		
		Log.d(TAG, "timerangecheck is about to return " + didCheck);
		return didCheck;
	}
	
	//return value for triggerIfRequired doesn't really mean anything anymore. 
	private boolean triggerIfRequired(int trigId, SimpleTime startTime , int state , long duration, boolean open){
		Log.d(TAG, "triggerIfRequired entered");
		ActivityTrigger actTrig = new ActivityTrigger();
		Calendar instance = Calendar.getInstance();
		SimpleTime now = new SimpleTime();
		
		
		//count backwards till you hit either 1) opposite activity 2)start time 3)sleep filter?
		if (now.differenceInMinutes(startTime) * 60 * 1000 < duration){
			Log.d(TAG, "time from startTime is less than duration, returning");
			return false;
		}
		instance.set(Calendar.HOUR_OF_DAY, startTime.getHour());
		instance.set(Calendar.MINUTE, startTime.getMinute());
		instance.set(Calendar.SECOND, 0);
		long start = instance.getTimeInMillis();
		
		mResolver = getContentResolver();
		
		String[] projection = new String[]{"time", "mode"} ;
		String selection = "time >= '" + start +"'";
		String sortOrder = "time DESC"; //latest to earliest. for counting backwards
		Cursor c = mResolver.query(uriMobility, projection, selection, null, sortOrder);
		Log.d(TAG, "Cursor count: " + c.getCount());
		long oppTime = 0L;
		long oppTimeChecker = 0L;
		boolean gettinShifty = false;
		long elapsedTime = 0L;
		long currentTime = System.currentTimeMillis();
		long previousTime = System.currentTimeMillis();
		
		long tempMemory = 0L;
		
		if (c.moveToFirst()){
			do{
				currentTime = c.getLong(0);
				int mode = modeToInt(c.getString(1));
				long difference = previousTime - currentTime;
				if (mode == state && !gettinShifty){
					elapsedTime += difference;
					
				}
				else if (mode == state && gettinShifty) {
					elapsedTime += difference;
					elapsedTime += tempMemory;
					tempMemory = 0L;
					oppTimeChecker += difference;
					
				}
				else if (mode != state && !gettinShifty){
					//elapsedTime += difference;
					tempMemory += difference;
					oppTime += difference;
					gettinShifty = true;
					
				}
				else if (mode != state && gettinShifty){
					//elapsedTime += difference;
					tempMemory += difference;
					oppTime += difference;
				}
				
				previousTime = currentTime;
				Log.d(TAG, "duration: " + duration);
				Log.d(TAG , "elapsed time: " + elapsedTime + " ms");
				if (oppTime > FIVE_MIN){
					c.close();
					if (open){
						updateTriggerStart(trigId , now , true);
					}
					return false;
				}
				if (oppTimeChecker > FIVE_MIN){
					oppTime -= difference;
					oppTimeChecker -= difference;
				}
				if (oppTime <= 0 && gettinShifty){
					oppTime = 0L;
					oppTimeChecker = 0L;
					gettinShifty = false;
				}
				if (elapsedTime >= duration){
					Log.d(TAG, "about to notify trigger");
					this.cancelTriggerAlwaysAlarm(trigId);
					actTrig.notifyTrigger(this, trigId);
					c.close();
					if (open){
						updateTriggerStart(trigId , now , true);
					}
					
					return true;
				}
				
			}while (c.moveToNext());
			Log.d(TAG, "exited do..while loop");
		}
		c.close();
		return false;
		
		
	}
	
	
	//i can put the sleep filter here. If the start plus duration is past 9 oclock, 
	//then reset start time to 9, unless user wakes up before
	private boolean  updateTriggerStart(int trigId , SimpleTime time, boolean changeSwitchTo){
		Log.d(TAG, "updateTriggerStart entered");
		ActivityTrigger trig = new ActivityTrigger();
		ActTrigDesc desc = new ActTrigDesc();
		if(!desc.loadString(trig.getTrigger(this, trigId))) {
			return false;
		}
		 
		desc.setSwitch(changeSwitchTo);
		desc.setStartTime(time);
		trig.updateTrigger(this, trigId, desc.toString());
		return false;
	
		
	}
	

	/*
	 * sets wake up alarm for next day. 
	 */
	private void setWakeupAlarm(Set<Integer> openTrigs){
		Log.d(TAG, "setWakeupAlarm() entered");
		cancelSamplingAlarm(ACTION_ALRM_WAKEUP_CHECK);
		long now = System.currentTimeMillis();
		long target = now + 1000L * 60L * 60L * 24L * 7L; //7 days
		ActivityTrigger trig = new ActivityTrigger();
		for (Integer id: openTrigs){
			ActTrigDesc desc = new ActTrigDesc();
			desc.loadString(trig.getTrigger(this, id));
			long nextStartTime = this.getNextStartTime(desc);
			if (this.getNextStartTime(desc) < target){
				target = nextStartTime;
			}
		}
		
		Calendar instance = Calendar.getInstance();
		instance.setTimeInMillis(target);
		instance.set(Calendar.HOUR_OF_DAY, WAKEUP_SCAN_HOUR);
		instance.set(Calendar.MINUTE, WAKEUP_SCAN_MINUTE);
		long elapsedTime = instance.getTimeInMillis() - now;
		Log.d(TAG, "setting wakeup alarm, elapsedTime: " + elapsedTime);
		setSamplingAlarm(ACTION_ALRM_WAKEUP_CHECK , elapsedTime, 0);
	}
	//0 is inactive, 1 is active
	private int modeToInt(String mode){
		if (mode.equals("error") || mode.equals("still") || mode.equals("drive")){
			return 0;
		}
		else return 1;
	}
	
	private int modeToIntWakeupCheck(String mode){
		if (mode.equals("error") || mode.equals("still")){
			return 0;
		}
		else return 1;
	}
	
	
	
	
	
//	private void startSampling(LinkedList<Integer> actTrigs) {
//		
//
//		Log.i(TAG, "ActTrigService: Starting sampling");
//		
//		long inactivityBout = checkInactivitySpanFromMobility(); //   in this method i set an alarm to check inactivity again in the future.
//		triggerIfRequired(inactivityBout , actTrigs);
//		//setAlarm to sample again in a minute.
//		setSamplingAlarm(ACTION_ALRM_COUNT_TIMEOUT, COUNT_TIMEOUT, 0);
//	}
	
	private void stopSampling() {
//		if(!mCountingStarted) {
//			return;
//		}

		Log.i(TAG, "ActTrigService: Stopping sampling");
		
		//stopGPS() -- do something here
		
		//set Alarm for earliest next trigger to start service then.
		
		
		
		releaseWakeLock();
	}
	
	
	

	
	
	
	private void setSamplingAlarm(String action, long timeOut, int extra) {
		Log.i(TAG, "ActTrigService: Setting alarm: " + action);
		Log.i(TAG, "timeOut is: " + timeOut + " ms");
		cancelSamplingAlarm(action);
		
		Intent i = new Intent(action);
		i.putExtra(KEY_SAMPLING_ALARM_EXTRA, extra);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
				   			PendingIntent.FLAG_CANCEL_CURRENT);
    	
		AlarmManager alarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMan.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
    					SystemClock.elapsedRealtime() + timeOut, pi);	
	}
	
	
	
	
	
	private void handleTriggerAlwaysAlarm(int trigId) {
		Log.d(TAG, "handling TriggerAlways alarm for id: " + trigId);
		//Re-confirm that the trigger has not gone off
		//today. Just to prevent any issues due to 
		//async nature of alarms
		ActivityTrigger actTrig = new ActivityTrigger();
		if(!actTrig.hasTriggeredToday(this, trigId)) {
			actTrig.notifyTrigger(this, trigId);
		}
		
		//Set the alarm for the next time
		setTriggerAlwaysAlarm(trigId, actTrig.getTrigger(this, trigId));
	}
	
	private void cancelSamplingAlarm(String action) {
		Intent i = new Intent(action);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
							PendingIntent.FLAG_NO_CREATE);
		if(pi != null) {
			AlarmManager alarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
			alarmMan.cancel(pi);
			pi.cancel();
		}
	}
	
	private void cancelAllSamplingAlarms() {
		cancelSamplingAlarm(ACTION_ALRM_COUNT_TIMEOUT);
		cancelSamplingAlarm(ACTION_ALRM_WAKEUP_CHECK);
	}
	
	
	
	/*
	 * here I will check for earliest day/time of next closedTimeRange trigger.  
	 */
	private void setAlarmForSampleNextStartRangeOfTrigger(Set<Integer> closedTrigSet){
		if (closedTrigSet.size()==0){
			return;
		}
		Log.d(TAG, "setAlarmForSampleNextStartRangeOfTrigger() entered");
		Calendar cal = Calendar.getInstance();
		ActivityTrigger actTrig = new ActivityTrigger();
		long nowLong = cal.getTimeInMillis(); 
		long elapsedTime = 1000L * 60L * 60L * 24L * 7L; //1 week. a big enough time just to be safe
		
		
		for (Integer trigId: closedTrigSet){
			ActTrigDesc desc = new ActTrigDesc();
			if(!desc.loadString(actTrig.getTrigger(this, trigId))) {
				continue;
			}
			if (!desc.getSwitch()){
				continue;
			}
			if (desc.isRangeEnabled()){
				long nextTime = getNextStartTime(desc);
				long diff = nextTime - nowLong;
				if (diff < elapsedTime){
					elapsedTime = diff;
				}
			}
		}
		setSamplingAlarm(ACTION_ALRM_COUNT_TIMEOUT, elapsedTime, 0);
	}
	
	private long getNextStartTime(ActTrigDesc desc){
		Calendar instance = Calendar.getInstance();
		int today = instance.get(Calendar.DAY_OF_WEEK);
		LinkedHashMap<Integer,Boolean> map = desc.getRepeatDays();
		
		int daysAhead = 0;
		if (map.get(today) && desc.getStartTime().isAfter(new SimpleTime())){
			Log.d(TAG, "this trigger is supposed to trigger today, and is after now");
			instance.set(Calendar.HOUR_OF_DAY, desc.getStartTime().getHour());
			instance.set(Calendar.MINUTE, desc.getStartTime().getMinute());
			instance.set(Calendar.SECOND, 0);
		}
		else{
			//next start time is another day.
			while(true){
				++today;
				++daysAhead;
				if(today == 8){
					today = 1;
				}
				if (map.get(today)){ 
					break;
				}
			}
			instance.add(Calendar.DAY_OF_YEAR, daysAhead);
			instance.set(Calendar.HOUR, desc.getStartTime().getHour());
			instance.set(Calendar.MINUTE, desc.getStartTime().getMinute());
			instance.set(Calendar.SECOND, 0);
			
		}
		return instance.getTimeInMillis();
	}

	private Intent createTriggerAlwaysAlarmIntent(int trigId) {
    	Intent i = new Intent();
    	
    	i.setAction(ACTION_ALRM_TRIGGER_ALWAYS);
    	i.setData(Uri.parse(DATA_PREFIX_TRIG_ALWAYS_ALM + trigId)); //why is this here.
    	i.putExtra(KEY_TRIG_ID, trigId);
    	return i;
    }
	
	private void cancelTriggerAlwaysAlarm(int trigId) {
		Intent i = createTriggerAlwaysAlarmIntent(trigId);
		
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
				   		   	  PendingIntent.FLAG_NO_CREATE);
		
		if(pi != null) {
			//cancel the alarm
			AlarmManager alarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
			alarmMan.cancel(pi);
			pi.cancel();
		}
	}
	
	private void setTriggerAlwaysAlarm(int trigId, String trigDesc) {
		
		cancelTriggerAlwaysAlarm(trigId);
		
		ActTrigDesc desc = new ActTrigDesc();
		if(!desc.loadString(trigDesc)) {
			return;
		}
		
		if(!desc.shouldTriggerAlways()) {
			return;
		}
		
		Log.i(TAG, "ActTrigService: Setting trigger always alarm(" + 
				trigId + ", " + trigDesc + ")");
		Calendar target = Calendar.getInstance();
		String dayStr = ActTrigDesc.getDayOfWeekString(
				target.get(Calendar.DAY_OF_WEEK));
		if (desc.doesRepeatOnDay(dayStr)){
		
			target.set(Calendar.HOUR_OF_DAY, desc.getEndTime().getHour());
			target.set(Calendar.MINUTE, desc.getEndTime().getMinute());
			target.set(Calendar.SECOND, 0);
			
			
			
			if(System.currentTimeMillis() >= target.getTimeInMillis()) {
				long nextTime = this.getNextStartTime(desc);
				target.setTimeInMillis(nextTime);
				target.set(Calendar.HOUR_OF_DAY, desc.getEndTime().getHour());
				target.set(Calendar.MINUTE, desc.getEndTime().getMinute());
				target.set(Calendar.SECOND, 0);
			}
		} else{
			long nextTime = this.getNextStartTime(desc);
			target.setTimeInMillis(nextTime);
			target.set(Calendar.HOUR_OF_DAY, desc.getEndTime().getHour());
			target.set(Calendar.MINUTE, desc.getEndTime().getMinute());
			target.set(Calendar.SECOND, 0);
		}
		
		Intent i = createTriggerAlwaysAlarmIntent(trigId);
	    PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
	    				   PendingIntent.FLAG_CANCEL_CURRENT);
	    
	    AlarmManager alarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
	    
	    Log.i(TAG, "ActTrigService: Calculated target time: " +
				  target.getTime().toString());
	    
	    long alarmTime = target.getTimeInMillis();
	    
	    /* Convert the alarm time to elapsed real time. 
	     * If we dont do this, a time change in the system might
	     * set off all the alarms and a trigger might go off before
	     * we get a chance to cancel it
	     */
	    long elapsedRT = alarmTime - System.currentTimeMillis();
	    if(elapsedRT <= 0) {
	    	Log.i(TAG, "ActTrigService: negative elapsed realtime - "
	    			+ "alarm not setting: "
					+ trigId);
	    	return;
	    }
	    
	    Log.i(TAG, "ActTrigService: Setting alarm for " + elapsedRT
				 + " millis into the future");
	    
	    alarmMan.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
					 SystemClock.elapsedRealtime() + elapsedRT, pi);
	}

	private void acquireWakeLock() {
		if(!mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}
	}
	
	private void releaseWakeLock() {
		if(mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}
	
	private static void acquireRecvrWakeLock(Context context) {
		
    	if(mRecvrWakeLock == null) {
			PowerManager powerMan = (PowerManager) context.
									getSystemService(POWER_SERVICE);
			
			mRecvrWakeLock = powerMan.newWakeLock(
	    				PowerManager.PARTIAL_WAKE_LOCK, RECR_WAKE_LOCK_TAG);
			mRecvrWakeLock.setReferenceCounted(true);
    	}
    	
		if(!mRecvrWakeLock.isHeld()) {
			mRecvrWakeLock.acquire();
		}
	}
	
	private static void releaseRecvrWakeLock() {
	
		if(mRecvrWakeLock == null) {
			return;
		}
		
		if(mRecvrWakeLock.isHeld()) {
			mRecvrWakeLock.release();
		}
	}
	
	
	
	/* Receiver for all the alarms */
	public static class AlarmReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			acquireRecvrWakeLock(context);
			
			Intent i = new Intent(context, ActTrigService.class);
			i.setAction(ACTION_HANDLE_ALARM);
			i.replaceExtras(intent);
			i.putExtra(KEY_ALARM_ACTION, intent.getAction());
			
			context.startService(i);
		}
	}
	
	 
	
}
