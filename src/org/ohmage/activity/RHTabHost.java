package org.ohmage.activity;

import java.util.Calendar;
import java.util.Date;

import org.ohmage.R;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbProvider;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class RHTabHost extends TabActivity {
	
	public static final String EXTRA_CAMPAIGN_URN = "campaign_urn";
	public static final String EXTRA_SURVEY_ID = "survey_id";
	
	TabHost mTabHost;
	
	private static int mCampaignFilterIndex;
	private static int mSurveyFilterIndex;
	private static Calendar mDateFilterIndex;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.response_history_tab_view);
		
		mDateFilterIndex = Calendar.getInstance();
		
		mTabHost = getTabHost();
		Intent intent = null; 
		
		
		//In case previous shown activity has campaign urn or survey id to set them into filters.
		String campaignUrn = getIntent().getStringExtra(EXTRA_CAMPAIGN_URN);
		String surveyId = getIntent().getStringExtra(EXTRA_SURVEY_ID);
		
		intent = new Intent().setClass(this, RHCalendarViewActivity.class);
		intent.putExtra(EXTRA_CAMPAIGN_URN, campaignUrn);
		intent.putExtra(EXTRA_SURVEY_ID, surveyId);
		setupTab(intent, "Calendar");
		
		intent = new Intent().setClass(this, RHMapViewActivity.class);
		setupTab(intent, "Map");
		
		mTabHost.setCurrentTab(0);
		
		mCampaignFilterIndex = 0;
		mSurveyFilterIndex = 0;
	} 
	
	private void setupTab(final Intent intent, final String tag){
		View tabview = createTabView(mTabHost.getContext(), tag);
		TabSpec setContent = mTabHost.newTabSpec(tag).setIndicator(tabview).setContent(intent);
		mTabHost.addTab(setContent);
	}
	
	private static View createTabView(final Context context, final String text){
		View view = LayoutInflater.from(context).inflate(R.layout.rh_tabs_bg, null);
		TextView tv = (TextView) view.findViewById(R.id.rh_tabs_text);
		tv.setText(text);
		return view;
	}
	
	public static void setCampaignFilterIndex(int idx){
		mCampaignFilterIndex = idx;
	}
	
	public static void setSurveyFilterIndex(int idx){
		mSurveyFilterIndex = idx;
	}
	
	public static void setDateFilterValue(Calendar cal){
		mDateFilterIndex = cal;
	}
	
	public static int getCampaignFilterIndex(){
		return mCampaignFilterIndex;
	}
	
	public static int getSurveyFilterIndex(){
		return mSurveyFilterIndex;
	}
	
	public static Calendar getDateFilterValue(){
		return mDateFilterIndex;
	}
}
