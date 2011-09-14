package org.ohmage.feedback.visualization;

import org.ohmage.R;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Response;
import org.ohmage.db.DbContract.Survey;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Pair;

import com.google.android.maps.MapActivity;

public class ResponseHistory extends MapActivity {
	FilterControl mCampaignFilter;
	FilterControl mSurveyFilter;
	
	/**
	 * Initialize Campaign filter and Survey filter for mapview and calendarview
	 * 
	 */
	public void setupFilters(){
		//Set filters
		mCampaignFilter = (FilterControl)findViewById(R.id.campaign_filter);
		mSurveyFilter = (FilterControl)findViewById(R.id.survey_filter);
		
		final ContentResolver cr = getContentResolver();
		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(String curValue) {
				Cursor surveys;
				if(curValue.equals("all")){
					surveys = cr.query(Survey.getSurveys(), null, null, null, Survey.TITLE);
					mSurveyFilter.populate(surveys, Survey.TITLE, Survey.SURVEY_ID);
				}
				else{
					surveys = cr.query(Survey.getSurveysByCampaignURN(curValue), null, null, null, null);
					mSurveyFilter.populate(surveys, Survey.TITLE, Survey.SURVEY_ID) ;					
				}
			}
		});
		
		Cursor campaigns = cr.query(Campaign.getCampaigns(), null, null, null, null);
		mCampaignFilter.populate(campaigns, Campaign.NAME, Campaign.URN);
		mCampaignFilter.add(0, new Pair<String, String>("All Campaigns", "all"));
		mSurveyFilter.add(0, new Pair<String, String>("All Surveys", "all"));
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
}

