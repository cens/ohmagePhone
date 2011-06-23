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
package edu.ucla.cens.andwellness.appwidget;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Toast;
import edu.ucla.cens.andwellness.AndWellnessApplication;
import edu.ucla.cens.andwellness.CampaignXmlHelper;
import edu.ucla.cens.andwellness.PromptXmlParser;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.activity.LoginActivity;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;
import edu.ucla.cens.andwellness.prompt.Prompt;
import edu.ucla.cens.andwellness.service.SurveyGeotagService;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;
import edu.ucla.cens.systemlog.Log;

public class StressButtonService extends IntentService {
	
	private static final String TAG = "StressButtonService";
	
	private Handler mHandler;
	
	public StressButtonService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mHandler = new Handler();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(100);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar now = Calendar.getInstance();
		String launchTime = dateFormat.format(now.getTime());
		
		final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(this);
		
		if (preferencesHelper.isUserDisabled()) {
        	((AndWellnessApplication) getApplication()).resetAll();
        }
		
		if (!preferencesHelper.isAuthenticated()) {
			Log.i(TAG, "no credentials saved, so launch Login");
			startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			return;
		}
		
		List<Prompt> prompts = null;
		
		String surveyId = "stressButton";
        String surveyTitle = "Stress";
        
        try {
			prompts = PromptXmlParser.parsePrompts(CampaignXmlHelper.loadDefaultCampaign(this), surveyId);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (prompts != null && prompts.size() > 0) {
		
			startService(new Intent(this, SurveyGeotagService.class));
	
			if (((AbstractPrompt)prompts.get(0)).getResponseObject() == null) {
				Toast.makeText(this, "There is a bug: default value not being set!", Toast.LENGTH_SHORT).show();
			} else {
				((AbstractPrompt)prompts.get(0)).setDisplayed(true);
				((AbstractPrompt)prompts.get(0)).setSkipped(false);
				Log.i(TAG, prompts.get(0).getResponseJson());
				storeResponse(surveyId, surveyTitle, launchTime, prompts);
				//Toast.makeText(this, "Registered stressful event.", Toast.LENGTH_SHORT).show();
				mHandler.post(new DisplayToast("Registered stressful event!"));
				
			}
		}
	}
	
	private class DisplayToast implements Runnable{
		String mText;

		public DisplayToast(String text){
			mText = text;
		}

		public void run(){
			/*LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.stress_button_toast, null);

			ImageView image = (ImageView) layout.findViewById(R.id.image);
			image.setImageResource(R.drawable.stress);
			TextView text = (TextView) layout.findViewById(R.id.text);
			text.setText(mText);

			Toast toast = new Toast(getApplicationContext());
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.setDuration(Toast.LENGTH_SHORT);
			toast.setView(layout);
			toast.show();*/
			
			Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
		}
	}

	private void storeResponse(String surveyId, String surveyTitle, String launchTime, List<Prompt> prompts) {
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		String campaign = helper.getCampaignName();
		String campaignVersion = helper.getCampaignUrn();
		String username = helper.getUsername();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar now = Calendar.getInstance();
		String date = dateFormat.format(now.getTime());
		long time = now.getTimeInMillis();
		String timezone = TimeZone.getDefault().getID();
		
		LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (loc == null || System.currentTimeMillis() - loc.getTime() > SurveyGeotagService.LOCATION_STALENESS_LIMIT || loc.getAccuracy() > SurveyGeotagService.LOCATION_ACCURACY_THRESHOLD) {
			Log.w(TAG, "gps provider disabled or location stale or inaccurate");
			loc = null;
		}
		
		//get launch context from trigger glue
		JSONObject surveyLaunchContextJson = new JSONObject();
		try {
			surveyLaunchContextJson.put("launch_time", launchTime);
			surveyLaunchContextJson.put("active_triggers", TriggerFramework.getActiveTriggerInfo(this, surveyTitle));
		} catch (JSONException e1) {
			throw new RuntimeException(e1);
		}
		String surveyLaunchContext = surveyLaunchContextJson.toString();
		
		JSONArray responseJson = new JSONArray();
		for (int i = 0; i < prompts.size(); i++) {
			JSONObject itemJson = new JSONObject();
			try {
				itemJson.put("prompt_id", ((AbstractPrompt)prompts.get(i)).getId());
				itemJson.put("value", ((AbstractPrompt)prompts.get(i)).getResponseObject());
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			responseJson.put(itemJson);
		}
		String response = responseJson.toString();
		
		DbHelper dbHelper = new DbHelper(this);
		if (loc != null) {
			dbHelper.addResponseRow(campaign, campaignVersion, username, date, time, timezone, SurveyGeotagService.LOCATION_VALID, loc.getLatitude(), loc.getLongitude(), loc.getProvider(), loc.getAccuracy(), loc.getTime(), surveyId, surveyLaunchContext, response);
		} else {
			dbHelper.addResponseRowWithoutLocation(campaign, campaignVersion, username, date, time, timezone, surveyId, surveyLaunchContext, response);
		}
	}
}
