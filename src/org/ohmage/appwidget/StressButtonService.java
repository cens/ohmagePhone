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

import edu.ucla.cens.systemlog.Log;

import org.ohmage.CampaignXmlHelper;
import org.ohmage.Config;
import org.ohmage.OhmageApplication;
import org.ohmage.PromptXmlParser;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.activity.LoginActivity;
import org.ohmage.activity.SurveyActivity;
import org.ohmage.db.Models.Campaign;
import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.prompt.SurveyElement;
import org.xmlpull.v1.XmlPullParserException;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Toast;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

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
		
		Calendar now = Calendar.getInstance();
		long launchTime = now.getTimeInMillis();
		
		final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(this);
		
		if (preferencesHelper.isUserDisabled()) {
        	((OhmageApplication) getApplication()).resetAll();
        }
		
		if (!preferencesHelper.isAuthenticated()) {
			Log.i(TAG, "no credentials saved, so launch Login");
			startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			return;
		}
		
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(100);

		List<SurveyElement> prompts = null;
		

		String campaignUrn = intent.getStringExtra("campaign_urn");
		if(Config.IS_SINGLE_CAMPAIGN) {
			campaignUrn = Campaign.getSingleCampaign(this);
		}
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

			if (((AbstractPrompt)prompts.get(0)).getResponseObject() == null) {
				mHandler.post(new DisplayToast("There is a bug: default value not being set!"));
			} else {
				((AbstractPrompt)prompts.get(0)).setDisplayed(true);
				((AbstractPrompt)prompts.get(0)).setSkipped(false);
				Log.i(TAG, ((AbstractPrompt)prompts.get(0)).getResponseJson());
				SurveyActivity.storeResponse(this, surveyId, launchTime, campaignUrn, surveyTitle, prompts);
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

		@Override
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
}
