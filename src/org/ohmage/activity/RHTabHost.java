package org.ohmage.activity;

import org.ohmage.R;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class RHTabHost extends TabActivity {
	TabHost mTabHost;
	
	private static int mCampaignFilterIndex;
	private static int mSurveyFilterIndex;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.response_history_tab_view);
		
		
		mTabHost = getTabHost();

		Intent intent = null; 
		
		intent = new Intent().setClass(this, RHCalendarViewActivity.class);
		setupTab(intent, "Calendar");
		
		intent = new Intent().setClass(this, RHMapViewActivity.class);
		setupTab(intent, "Map");
		mTabHost.setCurrentTab(0);
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
	
	public static int getCampaignFilterIndex(){
		return mCampaignFilterIndex;
	}
	
	public static int getSurveyFilterIndex(){
		return mSurveyFilterIndex;
	}
}
