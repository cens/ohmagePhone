package org.ohmage.feedback.visualization;

import java.util.Calendar;
import java.util.HashMap;

import org.ohmage.R;
import org.ohmage.activity.RHCalendarViewActivity;
import org.ohmage.controls.DateFilterControl;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Response;
import org.ohmage.db.DbContract.Surveys;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Pair;

import com.google.android.maps.MapActivity;

public class ResponseHistory extends MapActivity {
//	public FilterControl mCampaignFilter;
//	public FilterControl mSurveyFilter;
//	public DateFilterControl mDateFilter;
	
	/**
	 * Initialize Campaign filter and Survey filter for mapview and calendarview
	 * 
	 */
//	public void setupFilters(){
//		//Set filters
//		mCampaignFilter = (FilterControl)findViewById(R.id.campaign_filter);
//		mSurveyFilter = (FilterControl)findViewById(R.id.survey_filter);
//
//		final ContentResolver cr = getContentResolver();
//		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
//			@Override
//			public void onFilterChanged(String curCampaignValue) {
//				Cursor surveyCursor;
//
//				//Create Cursor
//				if(curCampaignValue.equals("all")){
//					surveyCursor = cr.query(Survey.getSurveys(), null, null, null, Survey.TITLE);
//				}
//				else{
//					surveyCursor = cr.query(Survey.getSurveysByCampaignURN(curCampaignValue), null, null, null, null);
//				}
//
//				//Update SurveyFilter
//				//Concatenate Campain_URN and Survey_ID with a colon for survey filer values,
//				//in order to handle 'All Campaign' case.
//				mSurveyFilter.clearAll();
//				for(surveyCursor.moveToFirst();!surveyCursor.isAfterLast();surveyCursor.moveToNext()){
//					mSurveyFilter.add(new Pair<String, String>(
//							surveyCursor.getString(surveyCursor.getColumnIndex(Survey.TITLE)),
//							surveyCursor.getString(surveyCursor.getColumnIndex(Survey.CAMPAIGN_URN)) + 
//							":" +
//							surveyCursor.getString(surveyCursor.getColumnIndex(Survey.SURVEY_ID))
//							));
//				}
//				mSurveyFilter.add(0, new Pair<String, String>("All Surveys", mCampaignFilter.getValue() + ":" + "all"));
//				surveyCursor.close();
//			}
//		});
//
//		mSurveyFilter.setOnChangeListener(new FilterChangeListener() {
//			@Override
//			public void onFilterChanged(String curValue) {
//			}
//		});
//
//		Cursor campaigns = cr.query(Campaign.getCampaigns(), null, null, null, null);
//		mCampaignFilter.populate(campaigns, Campaign.NAME, Campaign.URN);
//		mCampaignFilter.add(0, new Pair<String, String>("All Campaigns", "all"));	}

//	/**
//	 * Generates a HashMap having day(key) and the number of responses(value)
//	 * 
//	 * @param cursor Response cursor
//	 * @return
//	 */
//	
//	public HashMap<String, Integer> getResponseMap(Cursor cursor){
//
//		HashMap<String, Integer> map = new HashMap<String, Integer>();
//		Calendar cal = Calendar.getInstance();
//		
//		for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()){
//			Long time = cursor.getLong(cursor.getColumnIndex(Response.DATE));
//			
//			cal.setTimeInMillis(time);
//			Integer day = new Integer(cal.get(Calendar.DAY_OF_MONTH));
//			if(map.containsKey(day)){
//				Integer value = map.get(day) + 1;
//				map.put(day.toString(), value);
//			}
//			else{
//				map.put(day.toString(),1);
//			}
//		}
//		return map;
//	}
//	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	public void onDraw() {
		// TODO Auto-generated method stub
		
	}
	
	
}

