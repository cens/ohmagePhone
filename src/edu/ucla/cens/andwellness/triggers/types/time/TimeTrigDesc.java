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
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.DateUtils;
import edu.ucla.cens.andwellness.triggers.utils.SimpleTime;



/**
 * {
 * 		 "time": "12:45",
 * 		 "start": "11:45",
 * 		 "end": "13:45",
 * 		 "repeat": ["Monday", "Tuesday"],
 * }
 */
public class TimeTrigDesc {	
	private static final int DAY_SHORT_LEN = 3;
	
	private static final String KEY_TIME = "time";
	private static final String KEY_START = "start";
	private static final String KEY_END = "end";
	private static final String KEY_REPEAT = "repeat";
	private static final String VAL_RANDOM = "random";
	
	private SimpleTime mTrigTime = new SimpleTime();
	private SimpleTime mRangeStart = new SimpleTime();
	private SimpleTime mRangeEnd = new SimpleTime();
	private boolean mIsRandomized = false;
	private boolean mIsRangeEnabled = false;
	/*
	 * TODO: store the repeat days in using the constants in Calendar 
	 * and convert them to the textual representation when required.
	 */
	private LinkedHashMap<String, Boolean> mRepeatList = 
    						new LinkedHashMap<String, Boolean>();

	public TimeTrigDesc() {
		initialize(true);
	}
	
	private void initialize(boolean repeatStatus) {
		mIsRandomized = false;
		mIsRangeEnabled = false;
		
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
	
	public boolean loadString(String desc) {
		
		initialize(false);
		
		if(desc == null) {
			return false;
		}
		
		try {
			JSONObject jDesc = new JSONObject(desc);
			
			String time = jDesc.getString(KEY_TIME);
			if(time.equalsIgnoreCase(VAL_RANDOM)) {
				mIsRandomized = true;
			}
			else { 
				if(!mTrigTime.loadString(time)) {
					return false;
				}
			}
			
			if(jDesc.has(KEY_START)) {
				
				if(!jDesc.has(KEY_END)) {
					return false;
				}
				
				String start = jDesc.getString(KEY_START);
				if(!mRangeStart.loadString(start)) {
					return false;
				}
				
				String end = jDesc.getString(KEY_END);
				if(!mRangeEnd.loadString(end)) {
					return false;
				}
				
				mIsRangeEnabled = true;
			}
			else if(jDesc.has(KEY_END)) {
				//"End" without start - error
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
			
		} catch (JSONException e) {
			return false;
		} 
		
		return true;
	}
	
	public String toString() {
		
		JSONObject jDesc = new JSONObject();
		
		try {
			jDesc.put(KEY_TIME, mIsRandomized ? 
								VAL_RANDOM : mTrigTime.toString(false));
			
			if(mIsRangeEnabled) {
				jDesc.put(KEY_START, mRangeStart.toString(false));
				jDesc.put(KEY_END, mRangeEnd.toString(false));
			}
			
			JSONArray repeats = new JSONArray();
			for(String day : mRepeatList.keySet()) {
				if(mRepeatList.get(day)) {
					repeats.put(day);
				}
			}
			
			jDesc.put(KEY_REPEAT, repeats);
			
		} catch (JSONException e) {
			return null;
		}
		
		return jDesc.toString();
	}
	

	public SimpleTime getTriggerTime() {
		
		return new SimpleTime(mTrigTime);
	}
	
	public boolean setTriggerTime(SimpleTime time) {
		
		mTrigTime.copy(time);
		return true;
	}
	
	public boolean isRandomized() {
		return mIsRandomized;
	}
	
	public void setRandomized(boolean randomize) {
		mIsRandomized = randomize;
	}
	
	public boolean isRangeEnabled() {
		return mIsRangeEnabled;
	}
	
	public void setRangeEnabled(boolean enable) {
		mIsRangeEnabled = enable;
	}
	
	public SimpleTime getRangeStart() {
		return new SimpleTime(mRangeStart);
	}
	
	public void setRangeStart(SimpleTime time) {
		mRangeStart.copy(time);
	}
	
	public SimpleTime getRangeEnd() {
		return new SimpleTime(mRangeEnd);
	}
	
	public void setRangeEnd(SimpleTime time) {
		mRangeEnd.copy(time);
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
	
	public boolean validate() {
		
		if(mIsRangeEnabled) {
			
			if(!mRangeEnd.isAfter(mRangeStart)) {
				return false;
			}
			
			if(!mIsRandomized) {
				
				if(mTrigTime.isAfter(mRangeEnd) ||
				   mTrigTime.isBefore(mRangeStart)) {
					return false;
				}
			}
		}
		
		if(getRepeatDaysCount() == 0) {
			return false;
		}
		
		return true;
	}
}
