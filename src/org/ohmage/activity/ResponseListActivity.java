package org.ohmage.activity;

import java.util.Calendar;

import org.ohmage.R;
import org.ohmage.controls.DateFilterControl;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.DateFilterControl.DateFilterChangeListener;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Survey;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;

public class ResponseListActivity extends FragmentActivity{
	
	static final String TAG = "ResponseListActivitiy";
	private FilterControl mCampaignFilter;
	private FilterControl mSurveyFilter;
	private DateFilterControl mDateFilter;
	
	@Override 
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.response_list);
		setupFilters();
	}
	
	public void setupFilters(){
		//Set filters
		mDateFilter = (DateFilterControl)findViewById(R.id.date_filter);
		mCampaignFilter = (FilterControl)findViewById(R.id.campaign_filter);
		mSurveyFilter = (FilterControl)findViewById(R.id.survey_filter);
		
		mDateFilter.setCalendarUnit(Calendar.DATE);
	
		final ContentResolver cr = getContentResolver();
		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(String curCampaignValue) {
				Cursor surveyCursor;
	
				//Create Cursor
				if(curCampaignValue.equals("all")){
					surveyCursor = cr.query(Survey.getSurveys(), null, null, null, Survey.TITLE);
				}
				else{
					surveyCursor = cr.query(Survey.getSurveysByCampaignURN(curCampaignValue), null, null, null, null);
				}
	
				//Update SurveyFilter
				//Concatenate Campain_URN and Survey_ID with a colon for survey filer values,
				//in order to handle 'All Campaign' case.
				mSurveyFilter.clearAll();
				for(surveyCursor.moveToFirst();!surveyCursor.isAfterLast();surveyCursor.moveToNext()){
					mSurveyFilter.add(new Pair<String, String>(
							surveyCursor.getString(surveyCursor.getColumnIndex(Survey.TITLE)),
							surveyCursor.getString(surveyCursor.getColumnIndex(Survey.CAMPAIGN_URN)) + 
							":" +
							surveyCursor.getString(surveyCursor.getColumnIndex(Survey.SURVEY_ID))
							));
				}
				mSurveyFilter.add(0, new Pair<String, String>("All Surveys", mCampaignFilter.getValue() + ":" + "all"));
				surveyCursor.close();
			}
		});
	
		mSurveyFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(String curValue) {
			}
		});
		
		mDateFilter.setOnChangeListener(new DateFilterChangeListener() {
			
			@Override
			public void onFilterChanged(Calendar curValue) {
			}
		});
		
		Cursor campaigns = cr.query(Campaign.getCampaigns(), null, null, null, null);
		mCampaignFilter.populate(campaigns, Campaign.NAME, Campaign.URN);
		mCampaignFilter.add(0, new Pair<String, String>("All Campaigns", "all"));	
	}
}
