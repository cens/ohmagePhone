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
package org.ohmage.appwidget;

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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Toast;
import org.ohmage.OhmageApplication;
import org.ohmage.CampaignXmlHelper;
import org.ohmage.PromptXmlParser;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.activity.LoginActivity;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.SurveyElement;
import org.ohmage.service.SurveyGeotagService;
import org.ohmage.triggers.glue.TriggerFramework;
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
        	((OhmageApplication) getApplication()).resetAll();
        }
		
		if (!preferencesHelper.isAuthenticated()) {
			Log.i(TAG, "no credentials saved, so launch Login");
			startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			return;
		}
		
		List<SurveyElement> prompts = null;
		
		String campaignUrn = intent.getStringExtra("campaign_urn");
		String surveyId = intent.getStringExtra("survey_id");
        String surveyTitle = intent.getStringExtra("survey_title");
        
        try {
			prompts = PromptXmlParser.parseSurveyElements(CampaignXmlHelper.loadCampaignXmlFromDb(this, campaignUrn), surveyId);
		} catch (NotFoundException e) {
			Log.e(TAG, "Error parsing prompts from xml", e);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Error parsing prompts from xml", e);
		} catch (IOException e) {
			Log.e(TAG, "Error parsing prompts from xml", e);
		}
		
		if (prompts != null && prompts.size() > 0) {
		
			startService(new Intent(this, SurveyGeotagService.class));
	
			if (((AbstractPrompt)prompts.get(0)).getResponseObject() == null) {
				mHandler.post(new DisplayToast("There is a bug: default value not being set!"));
			} else {
				((AbstractPrompt)prompts.get(0)).setDisplayed(true);
				((AbstractPrompt)prompts.get(0)).setSkipped(false);
				Log.i(TAG, ((AbstractPrompt)prompts.get(0)).getResponseJson());
				storeResponse(campaignUrn, surveyId, surveyTitle, launchTime, prompts);
				//Toast.makeText(this, "Registered stressful event.", Toast.LENGTH_SHORT).show();
				mHandler.post(new DisplayToast("Registered stressful event!"));
				
			}
		} else {
			mHandler.post(new DisplayToast("Problem loading stress button survey!"));
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

	private void storeResponse(String campaignUrn, String surveyId, String surveyTitle, String launchTime, List<SurveyElement> prompts) {
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
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
			surveyLaunchContextJson.put("active_triggers", TriggerFramework.getActiveTriggerInfo(this, campaignUrn, surveyTitle));
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
		
//		DbHelper dbHelper = new DbHelper(this);
//		if (loc != null) {
//			dbHelper.addResponseRow(campaign, campaignVersion, username, date, time, timezone, SurveyGeotagService.LOCATION_VALID, loc.getLatitude(), loc.getLongitude(), loc.getProvider(), loc.getAccuracy(), loc.getTime(), surveyId, surveyLaunchContext, response);
//		} else {
//			dbHelper.addResponseRowWithoutLocation(campaign, campaignVersion, username, date, time, timezone, surveyId, surveyLaunchContext, response);
//		}
		
		Response candidate = new Response();
		
		candidate.campaignUrn = campaignUrn;
		candidate.username = username;
		candidate.date = date;
		candidate.time = time;
		candidate.timezone = timezone;
		candidate.surveyId = surveyId;
		candidate.surveyLaunchContext = surveyLaunchContext;
		candidate.response = response;
		candidate.status = Response.STATUS_STANDBY;
		
		if (loc != null) {
			candidate.locationStatus = SurveyGeotagService.LOCATION_VALID;
			candidate.locationLatitude = loc.getLatitude();
			candidate.locationLongitude = loc.getLongitude();
			candidate.locationProvider = loc.getProvider();
			candidate.locationAccuracy = loc.getAccuracy();
			candidate.locationTime = loc.getTime();
		} else {
			candidate.locationStatus = SurveyGeotagService.LOCATION_UNAVAILABLE;
			candidate.locationLatitude = -1;
			candidate.locationLongitude = -1;
			candidate.locationProvider = null;
			candidate.locationAccuracy = -1;
			candidate.locationTime = -1;
			candidate.status = Response.STATUS_WAITING_FOR_LOCATION;
		}

		ContentResolver cr = getContentResolver();
		cr.insert(Responses.CONTENT_URI, candidate.toCV());
	}
}
