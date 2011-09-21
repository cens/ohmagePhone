package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.activity.ResponseListFragment.OnResponseActionListener;
import org.ohmage.controls.DateFilterControl;
import org.ohmage.controls.DateFilterControl.DateFilterChangeListener;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Survey;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import java.util.Calendar;

/**
 * The ResponseListActivity will show a list of responses using the {@link ResponseListFragment} and the 
 * {@link ResponseListCursorAdapter}.
 * 
 * It can handle 3 different extras to specify a filter
 * {@link #EXTRA_CAMPAIGN_URI_FILTER}, {@link #EXTRA_SURVEY_FILTER}, and {@value #EXTRA_DATE_FLITER}
 * 
 * TODO: allow the activity to start based on different uris as well as different extras
 * TODO: init the fragment with the extras in onCreate
 * 
 * @author cketcham
 *
 */
public class ResponseListActivity extends FragmentActivity implements OnResponseActionListener, LoaderManager.LoaderCallbacks<Cursor> {

	static final String TAG = "ResponseListActivitiy";

	/**
	 * Filters the response list by the given campaign uri
	 */
	public static final String EXTRA_CAMPAIGN_URI_FILTER = "extra_campaign_uri_filter";

	/**
	 * Filters the response list by the given survey id. If {@link #EXTRA_CAMPAIGN_URI_FILTER} is not
	 * specified, this extra will be ignored. No checking will be done to make sure this survey is valid
	 */
	public static final String EXTRA_SURVEY_FILTER = "extra_survey_filter";

	/**
	 * Filters the response list by the given date. Only responses that happened on the date will be
	 * returned
	 */
	public static final String EXTRA_DATE_FLITER = "extra_date_filter";

	private static final int CAMPAIGN_FILTER_LOADER_ID = 0;
	private static final int SURVEY_FILTER_LOADER_ID = 1;

	private FilterControl mCampaignFilter;
	private FilterControl mSurveyFilter;
	private DateFilterControl mDateFilter;

	@Override 
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.response_list);
		setupFilters();

		if(getIntent().hasExtra(EXTRA_CAMPAIGN_URI_FILTER)) {
			String campaignFilter = getIntent().getStringExtra(EXTRA_CAMPAIGN_URI_FILTER);
			String surveyFilter = getIntent().getStringExtra(EXTRA_SURVEY_FILTER);
			if(getResponseListFragment() != null)
				getResponseListFragment().setFilters(campaignFilter, surveyFilter);
		}

		// Set the date if it was specified. If not, we default to showing the current date
		Calendar cal = Calendar.getInstance();
		if(getIntent().hasExtra(EXTRA_DATE_FLITER))
			cal.setTimeInMillis(getIntent().getLongExtra(EXTRA_DATE_FLITER, 0));

		mDateFilter.setDate(cal);

		// Start loading the campaigns and surveys for the filter. 
		getSupportLoaderManager().initLoader(CAMPAIGN_FILTER_LOADER_ID, null, this);
		getSupportLoaderManager().initLoader(SURVEY_FILTER_LOADER_ID, getIntent().getExtras(), ResponseListActivity.this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch(id) {
			case CAMPAIGN_FILTER_LOADER_ID:
				return new CursorLoader(this, Campaign.getCampaigns(), new String [] { Campaign.URN, Campaign.NAME }, 
						Campaign.STATUS + "=" + Campaign.STATUS_READY, null, Campaign.NAME);
			case SURVEY_FILTER_LOADER_ID:
				
				// If we are looking at All Campaigns, this query will return an empty cursor since we are searching for the campaign uri
				// that looks like 'null'.
				String campaignFilter = "null";
				if(args != null && args.containsKey(EXTRA_CAMPAIGN_URI_FILTER))
					campaignFilter = args.getString(EXTRA_CAMPAIGN_URI_FILTER);
				else if(mCampaignFilter.getValue() != null)
					campaignFilter = mCampaignFilter.getValue();
				
				return new CursorLoader(this, Survey.getSurveys(), new String [] { Survey.SURVEY_ID, Survey.TITLE }, 
						Survey.CAMPAIGN_URN + "=?", new String[] { campaignFilter }, Survey.TITLE);
			default:
				return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch(loader.getId()) {
			case CAMPAIGN_FILTER_LOADER_ID:
				// Now that the campaigns loaded, we can show the filters
				mCampaignFilter.setVisibility(View.VISIBLE);
				mSurveyFilter.setVisibility(View.VISIBLE);
				mDateFilter.setVisibility(View.VISIBLE);

				// Populate the filter
				mCampaignFilter.populate(data, Campaign.NAME, Campaign.URN);
				mCampaignFilter.add(0, new Pair<String,String>("All Campaigns", null));

				// initialize the filter if this is the first time
				if(!loader.isReset())
					mCampaignFilter.setValue(getIntent().getStringExtra(EXTRA_CAMPAIGN_URI_FILTER));

				break;
			case SURVEY_FILTER_LOADER_ID:

				// Populate the filter
				mSurveyFilter.populate(data, Survey.TITLE, Survey.SURVEY_ID);
				mSurveyFilter.add(0, new Pair<String, String>("All Surveys", null));
				
				// initialize the filter if this is the first time
				if(!loader.isReset())
					mSurveyFilter.setValue(getIntent().getStringExtra(EXTRA_SURVEY_FILTER));

				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mCampaignFilter.clearAll();
		mSurveyFilter.clearAll();
	}

	private ResponseListFragment getResponseListFragment() {
		return (ResponseListFragment) getSupportFragmentManager().findFragmentById(R.id.responses);
	}

	public void setupFilters(){
		//Set filters
		mDateFilter = (DateFilterControl)findViewById(R.id.date_filter);
		mCampaignFilter = (FilterControl)findViewById(R.id.campaign_filter);
		mSurveyFilter = (FilterControl)findViewById(R.id.survey_filter);

		mDateFilter.setVisibility(View.GONE);
		mCampaignFilter.setVisibility(View.GONE);
		mSurveyFilter.setVisibility(View.GONE);

		mDateFilter.setCalendarUnit(Calendar.DATE);

		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(boolean selfChange, String curCampaignValue) {
				if(selfChange)
					return;
				
				ResponseListFragment f = getResponseListFragment();
				if(f != null)
					f.setFilters(curCampaignValue, null);

				getSupportLoaderManager().restartLoader(SURVEY_FILTER_LOADER_ID, null, ResponseListActivity.this);
			}
		});

		mSurveyFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(boolean selfChange, String curValue) {
				if(selfChange)
					return;

				ResponseListFragment f = getResponseListFragment();
				if(f != null)
					f.setFilters(mCampaignFilter.getValue(), curValue);
			}
		});

		mDateFilter.setOnChangeListener(new DateFilterChangeListener() {

			@Override
			public void onFilterChanged(Calendar curValue) {
				ResponseListFragment f = getResponseListFragment();
				if(f != null)
					f.setDateBounds(curValue.getTimeInMillis(), curValue.getTimeInMillis() + DateUtils.DAY_IN_MILLIS);
			}
		});
	}

	@Override
	public void onResponseActionView(Uri responseUri) {
		startActivity(new Intent(Intent.ACTION_VIEW, responseUri));
	}

	@Override
	public void onResponseActionUpload(Uri responseUri) {
		Toast.makeText(this, "The Upload action should not be exposed in this activity!", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "onResponseActionUpload should not be exposed in this activity.");		
	}

	@Override
	public void onResponseActionError(Uri responseUri) {
		Toast.makeText(this, "The Error action should not be exposed in this activity!", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "onResponseActionError should not be exposed in this activity.");
	}
}
