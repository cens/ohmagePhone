package edu.ucla.cens.andwellness.triggers.base;

import java.util.LinkedHashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * Description of a trigger action (currently, only the list if surveys).
 * e.g.:
 * {
 * 		"surveys": ["Sleep", "Stress"]
 * }
 */
public class TriggerActionDesc {

	private static final String KEY_SURVEYS = "surveys";
	
	private LinkedHashSet<String> mSurveyList
							 = new LinkedHashSet<String>();

	private void initialize() {
		mSurveyList.clear();
	}
	
	public boolean loadString(String desc) {
		
		initialize();
		
		if(desc == null) {
			return false;
		}
		
		try {
			JSONObject jDesc = new JSONObject(desc);
			
			if(jDesc.has(KEY_SURVEYS)) {
				
				mSurveyList.clear();
			
				JSONArray surveys = jDesc.getJSONArray(KEY_SURVEYS);
				
				for(int i = 0; i < surveys.length(); i++) {
					mSurveyList.add(surveys.getString(i));
				}
			}
			
		} catch (JSONException e) {
			return false;
		}
		
		return true;
	}
	
	public String[] getSurveys() {
		return mSurveyList.toArray(new String[mSurveyList.size()]);
	}
	
	public void setSurveys(String[] surveys) {
		mSurveyList.clear();
		for(int i = 0; i < surveys.length; i++) {
			mSurveyList.add(surveys[i]);
		}
	}
	
	public boolean hasSurvey(String survey) {
		return mSurveyList.contains(survey) ? true
											: false;
	}
	
	public void clearAllSurveys() {
		mSurveyList.clear();
	}
	
	public void addSurvey(String survey) {
		mSurveyList.add(survey);
	}
	
	public int getCount() {
		return mSurveyList.size();
	}
	
	public String toString() {
		
		JSONObject jDesc = new JSONObject();
		
		JSONArray surveys = new JSONArray();
		for(String survey : mSurveyList) {
			surveys.put(survey);
		}
		
		if(surveys.length() > 0) {
			try {
				jDesc.put(KEY_SURVEYS, surveys);
			} catch (JSONException e) {
				return null;
			}
		}
	
		return jDesc.toString();
	}
	
	public static String getDefaultDesc() {
		return new JSONObject().toString();
	}
}
