package org.ohmage.activity;

import java.util.ArrayList;

import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.activity.SurveyListFragment.OnSurveyActionListener;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Survey;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import android.widget.Toast;

public class SurveyListActivity extends FragmentActivity implements OnSurveyActionListener {
	
	static final String TAG = "SurveyListActivity";
	
	private FilterControl mCampaignFilter;
	private FilterControl mPendingFilter;
	
	private SharedPreferencesHelper mSharedPreferencesHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.survey_list);
		
		mCampaignFilter = (FilterControl) findViewById(R.id.campaign_filter);
		mPendingFilter = (FilterControl) findViewById(R.id.pending_filter);

		ContentResolver cr = getContentResolver();
		String select = Campaign.STATUS + "=" + Campaign.STATUS_READY;
		Cursor data = cr.query(Campaign.CONTENT_URI, new String [] {Campaign._ID, Campaign.NAME, Campaign.URN}, select, null, Campaign.NAME);
		mCampaignFilter.populate(data, Campaign.NAME, Campaign.URN);
		mCampaignFilter.add(0, new Pair<String, String>("All Campaigns", SurveyListFragment.FILTER_ALL_CAMPAIGNS));
		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
			
			@Override
			public void onFilterChanged(boolean selfChange, String curValue) {
				((SurveyListFragment)getSupportFragmentManager().findFragmentById(R.id.surveys)).setCampaignFilter(curValue);
			}
		});
		
		mPendingFilter.add(new Pair<String, String>("All Surveys", "all"));
		mPendingFilter.add(new Pair<String, String>("Pending Surveys", "pending"));
		mPendingFilter.setOnChangeListener(new FilterChangeListener() {
			
			@Override
			public void onFilterChanged(boolean selfChange, String curValue) {
				if (curValue.equalsIgnoreCase("pending")) {
					((SurveyListFragment)getSupportFragmentManager().findFragmentById(R.id.surveys)).setShowPending(true);
				} else {
					((SurveyListFragment)getSupportFragmentManager().findFragmentById(R.id.surveys)).setShowPending(false);
				}
				
			}
		});
		
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		
		String campaignUrn = getIntent().getStringExtra("campaign_urn");
		boolean showPending = getIntent().getBooleanExtra("show_pending", false);
		
		if (campaignUrn != null) {
			mCampaignFilter.setValue(campaignUrn);
		}
		
		mPendingFilter.setValue(showPending ? "pending" : "all");
	}

	@Override
	public void onSurveyActionView(Uri surveyUri) {
		Intent i = new Intent(this, SurveyInfoActivity.class);
		i.setData(surveyUri);
		startActivity(i);
	}

	@Override
	public void onSurveyActionStart(Uri surveyUri) {
		Cursor cursor = getContentResolver().query(surveyUri, null, null, null, null);
		if (cursor.moveToFirst()) {
			Intent intent = new Intent(this, SurveyActivity.class);
			intent.putExtra("campaign_urn", cursor.getString(cursor.getColumnIndex(Survey.CAMPAIGN_URN)));
			intent.putExtra("survey_id", cursor.getString(cursor.getColumnIndex(Survey.SURVEY_ID)));
			intent.putExtra("survey_title", cursor.getString(cursor.getColumnIndex(Survey.TITLE)));
			intent.putExtra("survey_submit_text", cursor.getString(cursor.getColumnIndex(Survey.SUBMIT_TEXT)));
			startActivity(intent);
		} else {
			Toast.makeText(this, "onSurveyActionStart: Error: Empty cursor returned.", Toast.LENGTH_SHORT).show();
		}
		cursor.close();
	}

	@Override
	public void onSurveyActionUnavailable(Uri surveyUri) {
		Toast.makeText(this, "This survey can only be taken when triggered.", Toast.LENGTH_SHORT).show();
	}

}
