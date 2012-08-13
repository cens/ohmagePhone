package org.ohmage.triggers.types.activity;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.triggers.utils.SimpleTime;

import android.text.format.DateUtils;
import android.util.Log;




/*
 * The class which can parse and store the JSON string of inactivity 
 * trigger description. 
 * 
 * Example of activity trigger description:
 * 
 * {
 * 		"switch": true,    //on and off switch
 * 		"duration": 30000, //in milliseconds
 * 		"state": 0,      // 0 for inactive, 1 active
 * 		"start": "11:00",
 * 		"repeat": ["Monday", "Tuesday"],
 * 		"time_range": {
 * 			"end": "13:00",
 * 			"trigger_always": true
 * 		}
 * }
 */

public class ActTrigDesc {

	private static final String tag = "ActTrigDesc";
	private static final String KEY_SWITCH = "switch";
	private static final String KEY_DURATION = "duration";
	private static final String KEY_STATE = "state";
	private static final String KEY_REPEAT = "repeat";
	private static final String KEY_TIME_RANGE = "time_range";
	private static final String KEY_START = "start";
	private static final String KEY_END = "end";
	private static final String KEY_TRIGGER_ALWAYS = "trigger_always";
	
	private boolean mSwitch = true;
	private long mDuration;
	private int mState;
	private SimpleTime mStartTime = new SimpleTime();
	private SimpleTime mEndTime = new SimpleTime();
	private boolean mRangeEnabled = false;
	private boolean mTriggerAlways = false;
	private LinkedHashMap<String, Boolean> mRepeatList = 
			new LinkedHashMap<String, Boolean>();
	private LinkedHashMap<Integer, Boolean> mRepeatListInts = 
			new LinkedHashMap<Integer,Boolean>();
	private static final int DAY_SHORT_LEN = 3;
	
	private void initialize(boolean repeatStatus) {
		setSwitch(true);
		mDuration = 0;
		mTriggerAlways = false;
		mRangeEnabled = false;
		
		mRepeatList.put(getDayOfWeekString(Calendar.SUNDAY), repeatStatus);
		mRepeatList.put(getDayOfWeekString(Calendar.MONDAY), repeatStatus);
		mRepeatList.put(getDayOfWeekString(Calendar.TUESDAY), repeatStatus);
		mRepeatList.put(getDayOfWeekString(Calendar.WEDNESDAY), repeatStatus);
		mRepeatList.put(getDayOfWeekString(Calendar.THURSDAY), repeatStatus);
		mRepeatList.put(getDayOfWeekString(Calendar.FRIDAY), repeatStatus);
		mRepeatList.put(getDayOfWeekString(Calendar.SATURDAY), repeatStatus);
	}
	
	public static String getDayOfWeekString(int dayOfWeek) {
		return DateUtils.getDayOfWeekString(dayOfWeek, DateUtils.LENGTH_LONG);
	}
	/*
	 * Parse an activity trigger description 
	 * and load the parameters into this object. 
	 */
	public ActTrigDesc(){
		initialize(true);
	}
	public boolean loadString(String desc) {
			
			initialize(false);
			
			if(desc == null) {
				return false;
			}
			
			try {
				JSONObject jDesc = new JSONObject(desc);
				mSwitch = jDesc.getBoolean(KEY_SWITCH);
				mDuration = jDesc.getInt(KEY_DURATION);
				mState = jDesc.getInt(KEY_STATE);
				if (!mStartTime.loadString(jDesc.getString(KEY_START))){
					return false;
				}
				JSONArray repeats = jDesc.getJSONArray(KEY_REPEAT);
				if(repeats.length() == 0) {
					return false;
				}
				
				for(int i = 0; i < repeats.length(); i++) {
					String day = repeats.getString(i);
					
					if(!mRepeatList.containsKey(day)) {
						return false;
					}
					
					mRepeatList.put(day, true);
				}
					
				if(jDesc.has(KEY_TIME_RANGE)) {
					
					JSONObject jRange = jDesc.getJSONObject(KEY_TIME_RANGE);
					
					if(!mEndTime.loadString(jRange.getString(KEY_END))) {
						return false;
					}
					
					mTriggerAlways = jRange.getBoolean(KEY_TRIGGER_ALWAYS);
					
					mRangeEnabled = true;
				}
			}	
			catch (JSONException e) {
				Log.d(tag, "json exception thrown");
				return false;
			}
		
			return true;
			
	}
	
	//getters and setters
	
	public boolean getSwitch() {
		return mSwitch;
	}

	public void setSwitch(boolean mSwitch) {
		this.mSwitch = mSwitch;
	}
	
	public long getDuration(){
		return mDuration;
	}
	
	public void setDuration(long duration){
		mDuration = duration;
	}
	
	public String getDurationString(){
		String result = "";
		int durationMin = (int) (this.getDuration() / 1000 / 60);
		int hours = durationMin/60;
		int min = durationMin % 60;
		if (hours == 1){
			result += "1 hour ";
		}
		else if (hours > 1){
			result += hours + " hours ";
		} 
		if (min == 1){
			result += "1 minute";
		}
		else if (min > 1){
			result += "" + min + " minutes";
		}
		return result;
	}
	public int getDurationHours(){
		int durationMin = (int) (this.getDuration() / 1000 / 60);
		int hours = durationMin/60;
		return hours;
	}
	
	public int getDurationMin(){
		int durationMin = (int) (this.getDuration() / 1000 / 60);
		
		int min = durationMin % 60;
		return min;
	}
	
	public int getDurationMinTotal(){
		return (int) (this.getDuration() / 1000 / 60);
	}
	
	public int getState(){
		return mState;
	}
	
	public void setState(int state){
		mState = state;
	}
	public String getStateString(){
		
		if (mState == 0){
			return "Inactive";
		}
		else return "Active";
		
	}
	public boolean isRangeEnabled() {
		return mRangeEnabled;
	}
	
	/*
	 * Set whether there is a time range
	 */
	public void setRangeEnabled(boolean enable) {
		mRangeEnabled = enable;
	}
	
	/*
	 * Get the start time if a time range is enabled
	 */
	public SimpleTime getStartTime() {
		return new SimpleTime(mStartTime);
	}
	
	/*
	 * Set the start time. 
	 */
	public void setStartTime(SimpleTime time) {
		mStartTime.copy(time);
	}
	
	/*
	 * Get the end time if a time range is enabled
	 */
	public SimpleTime getEndTime() {
		return new SimpleTime(mEndTime);
	}
	
	/*
	 * Set the end time
	 */
	public void setEndTime(SimpleTime time) {
		mEndTime.copy(time);
	}

	/*
	 * Check if this trigger is set to go off 
	 * at the end of time range even if the 
	 * requirement is not reached.
	 */
	public boolean shouldTriggerAlways() {
		return mTriggerAlways;
	}
	
	/*
	 * Set whether this trigger must go off 
	 * at the end of time range even if the 
	 * requirement is not reached.
	 */
	public void setTriggerAlways(boolean enable) {
		mTriggerAlways = enable;
	}
	
	public LinkedHashMap<String, Boolean> getRepeat() {
		LinkedHashMap<String, Boolean> ret 
				= new LinkedHashMap<String, Boolean>(); 
		
		ret.putAll(mRepeatList);
		
		return ret;
	}
	
	public void setRepeatStatus(String day, boolean status) {
		mRepeatList.put(day, status);
	}
	
	public LinkedHashMap<Integer,Boolean> getRepeatDays(){
		LinkedHashMap<Integer,Boolean> result = new LinkedHashMap<Integer,Boolean>();
		Set<String> days = mRepeatList.keySet();
		for (String s: days){
			if (s.equals(getDayOfWeekString(Calendar.SUNDAY))){
				result.put(Calendar.SUNDAY, mRepeatList.get(s));
			}
			if (s.equals(getDayOfWeekString(Calendar.MONDAY))){
				result.put(Calendar.MONDAY, mRepeatList.get(s));
			}
			if (s.equals(getDayOfWeekString(Calendar.TUESDAY))){
				result.put(Calendar.TUESDAY, mRepeatList.get(s));
			}
			if (s.equals(getDayOfWeekString(Calendar.WEDNESDAY))){
				result.put(Calendar.WEDNESDAY, mRepeatList.get(s));
			}
			if (s.equals(getDayOfWeekString(Calendar.THURSDAY))){
				result.put(Calendar.THURSDAY, mRepeatList.get(s));
			}
			if (s.equals(getDayOfWeekString(Calendar.FRIDAY))){
				result.put(Calendar.FRIDAY, mRepeatList.get(s));
			}
			if (s.equals(getDayOfWeekString(Calendar.SATURDAY))){
				result.put(Calendar.SATURDAY, mRepeatList.get(s));
			}
		}
		return result;
		
	}
	
	private int getRepeatDaysCount() {
		int nRepeatDays = 0;
		for(String day : mRepeatList.keySet()) {
			if(mRepeatList.get(day)) {
				nRepeatDays++;
			}
		}
		
		return nRepeatDays;
	}
	
	public String getRepeatDescription() {
		String ret = "";
		
		int nRepeatDays = getRepeatDaysCount();
		
		if(nRepeatDays == 7) {
			ret = "Everyday";
		}
		else {	
			int i = 0;
			for(String day : mRepeatList.keySet()) {
				if(mRepeatList.get(day)) {
					int strLen = day.length();
					
					if(strLen > DAY_SHORT_LEN) {
						strLen = DAY_SHORT_LEN;
					}
					
					ret += day.substring(0, strLen);
					
					i++;
					if(i < nRepeatDays) {
						ret += ", ";
					}
				}
			}
		}
		
		return ret;
	}
	
	public boolean doesRepeatOnDay(String day) {
		
		return mRepeatList.get(day);
	}
	
	
	
	/*
	 * Convert the trigger description represented by
	 * this object to a JSON string.
	 */
	public String toString() {
		JSONObject jDesc = new JSONObject();
		
		try {
			jDesc.put(KEY_SWITCH, mSwitch);
			jDesc.put(KEY_DURATION, mDuration);
			jDesc.put(KEY_STATE, mState);
			jDesc.put(KEY_START, mStartTime.toString(false));
			JSONArray repeats = new JSONArray();
			for(String day : mRepeatList.keySet()) {
				if(mRepeatList.get(day)) {
					repeats.put(day);
				}
			}
			
			jDesc.put(KEY_REPEAT, repeats);
			if(mRangeEnabled) {
				JSONObject jRange = new JSONObject();
				
				jRange.put(KEY_END, mEndTime.toString(false));
				jRange.put(KEY_TRIGGER_ALWAYS, mTriggerAlways);
				
				jDesc.put(KEY_TIME_RANGE, jRange);
			}
		} catch (JSONException e) {
			return null;
		}
		
		return jDesc.toString();
	}
	
	public boolean validate() {
		
		if(mDuration <= 0) {
			return false;
		}
		
		if (mState != 0 && mState != 1){
			return false;
		}
		
		if(mTriggerAlways && !mRangeEnabled) {
			return false;
		}
		
		if(getRepeatDaysCount() == 0) {
			return false;
		}
		
		if(mRangeEnabled && !mEndTime.isAfter(mStartTime)) {
			return false;
		}
		
		return true;
	}



	



	
	
}
