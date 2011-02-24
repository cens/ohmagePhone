package edu.ucla.cens.andwellness.triggers.notif;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import edu.ucla.cens.andwellness.triggers.config.NotifConfig;

/*
 * {
 * 		"duration": 60
 * 		"suppression": 30
 * 		"repeat": [5, 10, 30] 
 * }
 */
public class NotifDesc {

	private static final String PREF_FILE_NAME = 
				"edu.ucla.cens.triggers.notif.NotifDesc";
	private static final String PREF_KEY_GLOBAL_NOTIF_DESC = "notif_desc";

	private static final int REPEAT_MIN = 1;
	
	private static final String KEY_DURATION = "duration";
	private static final String KEY_SUPPRESSION = "suppression";
	private static final String KEY_REPEAT = "repeat";
	
	private int mDuration;
	private int mSuppress;
	private LinkedList<Integer> mRepeatList = new LinkedList<Integer>();
	
	private void initialze() {
		mDuration = 0;
		mSuppress = 0;
		mRepeatList.clear();
	}
	
	public boolean loadString(String desc) {
		
		initialze();
		
		if(desc == null) {
			return false;
		}
		
		try {
			JSONObject jDesc = new JSONObject(desc);
			
			mDuration = jDesc.getInt(KEY_DURATION);
			mSuppress = jDesc.getInt(KEY_SUPPRESSION);
			
			if(jDesc.has(KEY_REPEAT)) {
				
				mRepeatList.clear();
				
				JSONArray repeats = jDesc.getJSONArray(KEY_REPEAT);
				
				for(int i = 0; i < repeats.length(); i++) {
					mRepeatList.add(repeats.getInt(i));
				}
			}
			
		} catch (JSONException e) {
			return false;
		}
		
		return true;
	}
	
	public int getDuration() {
		return mDuration;
	}
	
	public void setDuration(int duration) {
		mDuration = duration;
	}
	
	public int getSuppression() {
		return mSuppress;
	}
	
	public void setSuppression(int suppress) {
		mSuppress = suppress;
	}
	
	public List<Integer> getSortedRepeats() {
		LinkedList<Integer> ret = new LinkedList<Integer>(mRepeatList);
		
		Collections.sort(ret, new Comparator<Integer>() {

			@Override
			public int compare(Integer a, Integer b) {
				
				return (a - b);
			}
		});
		
		return ret;
	}
	
	public void setRepeats(List<Integer> repeats) {
		mRepeatList.clear();
		
		for(Integer repeat : repeats) {
			if(!mRepeatList.contains(repeat)) {
				mRepeatList.add(repeat);
			}
		}
	}
	
	public static String getGlobalDesc(Context context) {
		
		SharedPreferences prefs = context.getSharedPreferences(
								  PREF_FILE_NAME, Context.MODE_PRIVATE);
		
		String notifDesc = prefs.getString(PREF_KEY_GLOBAL_NOTIF_DESC, 
											NotifConfig.defaultConfig);
		
		return notifDesc;
	}
	
	public static void setGlobalDesc(Context context, String desc) {
	
		SharedPreferences prefs = context.getSharedPreferences(
				  					PREF_FILE_NAME, Context.MODE_PRIVATE);
		
		Editor editor = prefs.edit();
		editor.putString(PREF_KEY_GLOBAL_NOTIF_DESC, desc);
		editor.commit();
	}
	
	public static String getDefaultDesc(Context context) {
		return getGlobalDesc(context);
	}
	
	public int getMinAllowedRepeat() {
		return REPEAT_MIN;
	}
	
	public String toString() {
		
		JSONObject jDesc = new JSONObject();
		
		try {
			jDesc.put(KEY_DURATION, mDuration);
			jDesc.put(KEY_SUPPRESSION, mSuppress);
			
			JSONArray repeats = new JSONArray();
			for(int repeat : mRepeatList) {
				repeats.put(repeat);
			}
			
			if(repeats.length() > 0) {
				jDesc.put(KEY_REPEAT, repeats);
			}
			
		} catch (JSONException e) {
			return null;
		}
		
		return jDesc.toString();
	}
	
}
