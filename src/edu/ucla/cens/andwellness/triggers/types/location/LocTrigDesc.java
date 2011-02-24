package edu.ucla.cens.andwellness.triggers.types.location;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import edu.ucla.cens.andwellness.triggers.config.LocTrigConfig;
import edu.ucla.cens.andwellness.triggers.utils.SimpleTime;

/*
 * e.g.:
 * {
 * 		"location": "Home",
 * 		"min_reentry_interval": 30
 * 		"time_range": {
 * 			"start": "11:00",
 * 			"end": "13:00"
 * 			"trigger_always": true
 * 		}
 * }
 */
public class LocTrigDesc {

	private static final String KEY_LOCATION = "location";
	private static final String KEY_TIME_RANGE = "time_range";
	private static final String KEY_START = "start";
	private static final String KEY_END = "end";
	private static final String KEY_TRIGGER_ALWAYS = "trigger_always";
	private static final String KEY_MIN_INTERVAL_REENTRY = "min_reentry_interval";
	
	private String mLocation;
	private SimpleTime mStartTime = new SimpleTime();
	private SimpleTime mEndTime = new SimpleTime();
	private boolean mRangeEnabled = false;
	private boolean mTriggerAlways = false;
	private int mMinInterval;
	
	private void initialize() {
		mLocation = null;
		mTriggerAlways = false;
		mRangeEnabled = false;
		mMinInterval = 0;
	}
	
	public boolean loadString(String desc) {
		
		initialize();
		
		if(desc == null) {
			return false;
		}
		
		try {
			JSONObject jDesc = new JSONObject(desc);
			
			mLocation = jDesc.getString(KEY_LOCATION);
			mMinInterval = jDesc.getInt(KEY_MIN_INTERVAL_REENTRY);
			
			if(jDesc.has(KEY_TIME_RANGE)) {
			
				JSONObject jRange = jDesc.getJSONObject(KEY_TIME_RANGE);

				if(!mStartTime.loadString(jRange.getString(KEY_START))) {
					return false;
				}
				
				if(!mEndTime.loadString(jRange.getString(KEY_END))) {
					return false;
				}
				
				mTriggerAlways = jRange.getBoolean(KEY_TRIGGER_ALWAYS);
				
				mRangeEnabled = true;
			}
			
		} catch (JSONException e) {
			return false;
		}
	
		return true;
	}
	
	public String getLocation() {
		return mLocation;
	}
	
	public void setLocation(String loc) {
		mLocation = loc;
	}
	
	public int getMinReentryInterval() {
		return mMinInterval;
	}
	
	public static int getGlobalMinReentryInterval(Context context) {
		return LocTrigConfig.MIN_REENTRY;
	}
	
	public void setMinReentryInterval(int interval) {
		mMinInterval = interval;
	}
	
	public boolean isRangeEnabled() {
		return mRangeEnabled;
	}
	
	public void setRangeEnabled(boolean enable) {
		mRangeEnabled = enable;
	}
	
	public SimpleTime getStartTime() {
		return new SimpleTime(mStartTime);
	}
	
	public void setStartTime(SimpleTime time) {
		mStartTime.copy(time);
	}
	
	public SimpleTime getEndTime() {
		return new SimpleTime(mEndTime);
	}
	
	public void setEndTime(SimpleTime time) {
		mEndTime.copy(time);
	}

	public boolean shouldTriggerAlways() {
		return mTriggerAlways;
	}
	
	public void setTriggerAlways(boolean enable) {
		mTriggerAlways = enable;
	}
	
	public String toString() {
		JSONObject jDesc = new JSONObject();
		
		try {
			jDesc.put(KEY_LOCATION, mLocation);
			jDesc.put(KEY_MIN_INTERVAL_REENTRY, mMinInterval);
			
			if(mRangeEnabled) {
				JSONObject jRange = new JSONObject();
				
				jRange.put(KEY_START, mStartTime.toString(false));
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
		
		if(mLocation == null || mLocation.length() == 0) {
			return false;
		}
		
		if(mTriggerAlways && !mRangeEnabled) {
			return false;
		}
		
		if(mRangeEnabled && !mEndTime.isAfter(mStartTime)) {
			return false;
		}
		
		return true;
	}
}
