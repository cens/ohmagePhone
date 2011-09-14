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
package org.ohmage.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ohmage.CampaignXmlHelper;
import org.ohmage.OhmageApplication;
import org.ohmage.PromptXmlParser;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.Survey;
import org.ohmage.triggers.glue.LocationTriggerAPI;
import org.ohmage.triggers.glue.TriggerFramework;
import org.ohmage.triggers.notif.Notifier;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import edu.ucla.cens.mobility.glue.MobilityInterface;
import edu.ucla.cens.systemlog.Log;

public class SurveyListActivity extends ListActivity {
	
	private static final String TAG = "SurveyListActivity";
	
	private List<Survey> mSurveys;
	private List<String> mActiveSurveyTitles;
	private SurveyListAdapter adapter;
	private String mCampaignUrn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(this);
		
		if (preferencesHelper.isUserDisabled()) {
        	((OhmageApplication) getApplication()).resetAll();
        }
		
		if (!preferencesHelper.isAuthenticated()) {
			Log.i(TAG, "no credentials saved, so launch Login");
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return;
		} else {
			mSurveys = null; 
			mActiveSurveyTitles = new ArrayList<String>();
			
			Intent intent = getIntent();
			
			if (intent != null) {
				
				mCampaignUrn = intent.getStringExtra("campaign_urn");
				setTitle(intent.getStringExtra("campaign_name"));
			}
			
			try {
				mSurveys = PromptXmlParser.parseSurveys(CampaignXmlHelper.loadCampaignXmlFromDb(this, mCampaignUrn));
			} catch (NotFoundException e) {
				Log.e(TAG, "Error parsing surveys from xml", e);
			} catch (XmlPullParserException e) {
				Log.e(TAG, "Error parsing surveys from xml", e);
			} catch (IOException e) {
				Log.e(TAG, "Error parsing surveys from xml", e);
			}
			
			adapter = new SurveyListAdapter(this, mSurveys, mActiveSurveyTitles, R.layout.survey_list_item, R.layout.survey_list_header, mCampaignUrn);
			setListAdapter(adapter);
		}
	}	
	
	
	@Override
	protected void onStart() {
		super.onStart();
		
		registerReceiver(mSurveyListChangedReceiver, new IntentFilter(TriggerFramework.ACTION_ACTIVE_SURVEY_LIST_CHANGED));
		
		updateList();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		/*List<String> activeSurveyTitles = Arrays.asList(TriggerFramework.getActiveSurveys(this));
		
		for (Survey survey : mSurveys) {
			if (activeSurveyTitles.contains(survey.getTitle())) {
				survey.setTriggered(true);
			} else {
				survey.setTriggered(false);
			}
		}*/
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		unregisterReceiver(mSurveyListChangedReceiver);
	}

	private void updateList() {
		mActiveSurveyTitles.clear();
		
		Collections.addAll(mActiveSurveyTitles, TriggerFramework.getActiveSurveys(this, mCampaignUrn));
		
		adapter.notifyDataSetChanged();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		if (((SurveyListAdapter)getListAdapter()).getItemGroup(position) == SurveyListAdapter.GROUP_UNAVAILABLE) {
			Toast.makeText(this, "This survey can only be taken at certain times.", Toast.LENGTH_LONG).show();
		} else {
			Survey survey = (Survey) getListView().getItemAtPosition(position);
			
			Intent intent = new Intent(this, SurveyActivity.class);
			intent.putExtra("campaign_urn", mCampaignUrn);
			intent.putExtra("survey_id", survey.getId());
			intent.putExtra("survey_title", survey.getTitle());
			intent.putExtra("survey_submit_text", survey.getSubmitText());
			startActivity(intent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.survey_list_menu, menu);
	    if (SharedPreferencesHelper.IS_SINGLE_CAMPAIGN) {
	    	menu.findItem(R.id.status).setVisible(true);
	    	menu.findItem(R.id.mobility_settings).setVisible(true);
	    } else {
	    	menu.findItem(R.id.status).setVisible(false);
	    	menu.findItem(R.id.mobility_settings).setVisible(false);
	    }
	  	return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.trigger_settings:
			List<String> surveyTitles = new ArrayList<String>();
			for(Survey survey: mSurveys) {
				surveyTitles.add(survey.getTitle());
			}
			TriggerFramework.launchTriggersActivity(this, mCampaignUrn, surveyTitles.toArray(new String[surveyTitles.size()]));
			return true;
			
		case R.id.mobility_settings:
			MobilityInterface.showMobilityOptions(this);
			//Toast.makeText(this, "Mobility is not available.", Toast.LENGTH_SHORT).show();
			return true;
			
		case R.id.location_trace_settings:
			LocationTriggerAPI.launchLocationTracingSettingsActivity(this);
			return true;
			
		case R.id.status:
			//WakefulIntentService.sendWakefulWork(this, UploadService.class);
			intent = new Intent(this, StatusActivity.class);
			startActivityForResult(intent, 1);
			return true;
			
		case R.id.mapview:
			intent = new Intent(this, MapViewActivity.class);
			startActivityForResult(intent, 1);			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == 1) {
			if (resultCode == 125) {
				finish();
				startActivity(new Intent(SurveyListActivity.this, LauncherActivity.class));
			}
		}
	}

	private BroadcastReceiver mSurveyListChangedReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(TriggerFramework.ACTION_ACTIVE_SURVEY_LIST_CHANGED)) {
				if (intent.getStringExtra(Notifier.KEY_CAMPAIGN_URN).equals(mCampaignUrn)) {
					updateList();
				}
			}
		}
	};
}
