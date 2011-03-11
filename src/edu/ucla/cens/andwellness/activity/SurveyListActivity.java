package edu.ucla.cens.andwellness.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import edu.ucla.cens.andwellness.PromptXmlParser;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.Survey;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;
import edu.ucla.cens.mobility.glue.MobilityInterface;

public class SurveyListActivity extends ListActivity {
	
	private static final String TAG = "SurveyListActivity";
	
	private List<Survey> mSurveys;
	private List<String> mActiveSurveyTitles;
	private SurveyAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(this);
		
		if (!preferencesHelper.isAuthenticated()) {
			Log.i(TAG, "no credentials saved, so launch Login");
			startActivity(new Intent(this, LoginActivity.class));
			finish();
		}
		
		mSurveys = null; 
		mActiveSurveyTitles = new ArrayList<String>();
		
		try {
			mSurveys = PromptXmlParser.parseSurveys(getResources().openRawResource(SharedPreferencesHelper.CAMPAIGN_XML_RESOURCE_ID));
		} catch (NotFoundException e) {
			Log.e(TAG, "Error parsing surveys from xml", e);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Error parsing surveys from xml", e);
		} catch (IOException e) {
			Log.e(TAG, "Error parsing surveys from xml", e);
		}
		
		adapter = new SurveyAdapter(this, mSurveys, mActiveSurveyTitles, R.layout.survey_list_item, R.layout.survey_list_header);
		
		setListAdapter(adapter);
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
		
		Collections.addAll(mActiveSurveyTitles, TriggerFramework.getActiveSurveys(this));
		
		adapter.notifyDataSetChanged();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Survey survey = (Survey) getListView().getItemAtPosition(position);
		
		Intent intent = new Intent(this, SurveyActivity.class);
		intent.putExtra("survey_id", survey.getId());
		intent.putExtra("survey_title", survey.getTitle());
		intent.putExtra("survey_submit_text", survey.getSubmitText());
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	  	return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.trigger_settings:
			List<String> surveyTitles = new ArrayList<String>();
			for(Survey survey: mSurveys) {
				surveyTitles.add(survey.getTitle());
			}
			TriggerFramework.launchTriggersActivity(this, surveyTitles.toArray(new String[surveyTitles.size()]));
			return true;
		case R.id.mobility_settings:
			MobilityInterface.showMobilityOptions(this);
			//Toast.makeText(this, "Mobility is not available.", Toast.LENGTH_SHORT).show();
			return true;
			
		case R.id.status:
			//WakefulIntentService.sendWakefulWork(this, UploadService.class);
			Intent intent = new Intent(this, StatusActivity.class);
			startActivityForResult(intent, 1);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == 1) {
			if (resultCode == 123) {
				finish();
			}
		}
	}

	private BroadcastReceiver mSurveyListChangedReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(TriggerFramework.ACTION_ACTIVE_SURVEY_LIST_CHANGED)) {
				updateList();
			}
		}
	};

}
