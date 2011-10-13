package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.controls.DateFilterControl;
import org.ohmage.fragments.TestResponseFragment;
import org.ohmage.ui.CampaignSurveyFilterActivity;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;
import org.ohmage.ui.OhmageFilterable.CampaignFilterable;
import org.ohmage.ui.OhmageFilterable.CampaignSurveyFilter;
import org.ohmage.ui.OhmageFilterable.SurveyFilterable;
import org.ohmage.ui.OhmageFilterable.TimeFilter;
import org.ohmage.ui.OhmageFilterable.TimeFilterable;
import org.ohmage.ui.TabManager;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.Calendar;

/**
 * <p>The {@link ResponseHistoryActivity} shows a tab view which lets users switch between a calendar
 * and map view to show the response history</p>
 * 
 * <p>The {@link ResponseHistoryActivity} accepts {@link CampaignFilter#EXTRA_CAMPAIGN_URN}, {@link CampaignSurveyFilter# EXTRA_SURVEY_ID},
 *  {@link TimeFilter#EXTRA_MONTH}, and {@link TimeFilter#EXTRA_YEAR} as extras</p>
 * @author cketcham
 *
 */
public class ResponseHistoryActivity extends CampaignSurveyFilterActivity {

	private static final String TAG = "RHTabHost";

	TabHost mTabHost;
	TabManager mTabManager;

	private DateFilterControl mTimeFilter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.response_history_layout);

		mTimeFilter = (DateFilterControl) findViewById(R.id.date_filter);
		mTimeFilter.setMonth(getIntent().getIntExtra(TimeFilter.EXTRA_MONTH, -1), getIntent().getIntExtra(TimeFilter.EXTRA_YEAR, -1));
		mTimeFilter.setOnChangeListener(new DateFilterControl.DateFilterChangeListener() {

			@Override
			public void onFilterChanged(Calendar curValue) {
				Log.d(TAG, "calendar changed");
				((TimeFilterable)mTabManager.getCurrentTab().getFragment()).setMonth(curValue.get(Calendar.MONTH), curValue.get(Calendar.YEAR));
			}
		});

		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabManager = new TabManager(this, mTabHost, R.id.realtabcontent);
		mTabManager.setOnTabChangedListener(new TabManager.TabChangedListener() {

			@Override
			public void onTabChanged(String tabId) {
				((CampaignFilterable) mTabManager.getCurrentTab().getFragment()).setCampaignUrn(getCampaignUrn());
				((SurveyFilterable) mTabManager.getCurrentTab().getFragment()).setSurveyId(getSurveyId());
				((TimeFilterable)mTabManager.getCurrentTab().getFragment()).setMonth(getCurrentMonth(), getCurrentYear());
				mTimeFilter.setCalendarUnit(Calendar.MONTH);
			}
		});

		Bundle calendarBundle = intentToFragmentArguments(getIntent());
		calendarBundle.remove(TimeFilter.EXTRA_DAY);
		mTabManager.addTab(mTabHost.newTabSpec("calendar").setIndicator(createTabView("Calendar")),
				TestResponseFragment.class, calendarBundle);
		mTabManager.addTab(mTabHost.newTabSpec("map").setIndicator(createTabView("Map")),
				TestResponseFragment.class, intentToFragmentArguments(getIntent()));

		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}
	}

	private View createTabView(final String text){
		View view = LayoutInflater.from(this).inflate(R.layout.rh_tabs_bg, null);
		TextView tv = (TextView) view.findViewById(R.id.rh_tabs_text);
		tv.setText(text);
		return view;
	}

	protected int getCurrentMonth() {
		return mTimeFilter.getValue().get(Calendar.MONTH);
	}

	protected int getCurrentYear() {
		return mTimeFilter.getValue().get(Calendar.YEAR);
	}

	@Override
	protected void onCampaignFilterChanged(String filter) {
		Log.d(TAG, "campaign changed");

		super.onCampaignFilterChanged(filter);
		((CampaignFilterable) mTabManager.getCurrentTab().getFragment()).setCampaignUrn(filter);
	}

	@Override
	protected void onSurveyFilterChanged(String filter) {
		Log.d(TAG, "survey changed");

		super.onSurveyFilterChanged(filter);
		((SurveyFilterable) mTabManager.getCurrentTab().getFragment()).setSurveyId(filter);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tab", mTabHost.getCurrentTabTag());
	}
}
