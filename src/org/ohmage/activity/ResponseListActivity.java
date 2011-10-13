package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.activity.ResponseListFragment.OnResponseActionListener;
import org.ohmage.controls.DateFilterControl;
import org.ohmage.controls.DateFilterControl.DateFilterChangeListener;
import org.ohmage.ui.CampaignSurveyFilterActivity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.util.Log;
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
public class ResponseListActivity extends CampaignSurveyFilterActivity implements OnResponseActionListener {

	static final String TAG = "ResponseListActivitiy";


	/**
	 * Filters the response list by the given date. Only responses that happened on the date will be
	 * returned
	 */
	public static final String EXTRA_DATE_FLITER = "extra_date_filter";

	private DateFilterControl mDateFilter;

	@Override 
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.response_list);
		setupFilters();

		// Set the date if it was specified. If not, we default to showing the current date
		Calendar cal = Calendar.getInstance();
		if(getIntent().hasExtra(EXTRA_DATE_FLITER))
			cal.setTimeInMillis(getIntent().getLongExtra(EXTRA_DATE_FLITER, 0));

		mDateFilter.setDate(cal);
	}

	private ResponseListFragment getResponseListFragment() {
		return (ResponseListFragment) getSupportFragmentManager().findFragmentById(R.id.responses);
	}

	@Override
	protected void onCampaignFilterChanged(String filter) {
		super.onCampaignFilterChanged(filter);
		getResponseListFragment().setCampaignFilter(filter);
	}
	
	@Override
	protected void onSurveyFilterChanged(String filter) {
		super.onSurveyFilterChanged(filter);
		getResponseListFragment().setSurveyFilter(filter);
	}
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		super.onLoadFinished(loader, data);
		mDateFilter.setVisibility(View.VISIBLE);
	}
	
	public void setupFilters(){
		//Set filters
		mDateFilter = (DateFilterControl)findViewById(R.id.date_filter);
		mDateFilter.setVisibility(View.INVISIBLE);
		mDateFilter.setCalendarUnit(Calendar.DATE);

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
	public void onResponseActionError(Uri responseUri, int status) {
		Toast.makeText(this, "The Error action should not be exposed in this activity!", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "onResponseActionError should not be exposed in this activity.");
	}
}
