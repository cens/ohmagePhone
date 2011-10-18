package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.adapters.ResponseListCursorAdapter;
import org.ohmage.controls.DateFilterControl;
import org.ohmage.controls.DateFilterControl.DateFilterChangeListener;
import org.ohmage.fragments.ResponseListFragment;
import org.ohmage.fragments.ResponseListFragment.OnResponseActionListener;
import org.ohmage.ui.CampaignSurveyFilterActivity;
import org.ohmage.ui.OhmageFilterable.CampaignFilterable;
import org.ohmage.ui.OhmageFilterable.SurveyFilterable;
import org.ohmage.ui.OhmageFilterable.TimeFilter;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Calendar;

/**
 * The ResponseListActivity will show a list of responses using the {@link ResponseListFragment} and the 
 * {@link ResponseListCursorAdapter}.
 * 
 * It can handle different extras to specify a filter
 * {@link CampaignFilterable#EXTRA_CAMPAIGN_URN}, {@link SurveyFilterable#EXTRA_SURVEY_ID}, {@link TimeFilter#EXTRA_DAY}, {@link TimeFilter#EXTRA_MONTH}, {@link TimeFilter#EXTRA_YEAR}
 * 
 * TODO: allow the activity to start based on different uris as well as different extras
 * 
 * @author cketcham
 *
 */
public class ResponseListActivity extends CampaignSurveyFilterActivity implements OnResponseActionListener {

	static final String TAG = "ResponseListActivitiy";

	private DateFilterControl mDateFilter;

	@Override 
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.response_list);
		
        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment newFragment = new ResponseListFragment();
            newFragment.setArguments(intentToFragmentArguments(getIntent()));
            ft.add(R.id.response_list_fragment, newFragment);
            ft.commit();
        }
		
		setupFilters();
	}

	private ResponseListFragment getResponseListFragment() {
		return (ResponseListFragment) getSupportFragmentManager().findFragmentById(R.id.response_list_fragment);
	}

	@Override
	protected void onCampaignFilterChanged(String filter) {
		super.onCampaignFilterChanged(filter);
		getResponseListFragment().setCampaignUrn(filter);
	}
	
	@Override
	protected void onSurveyFilterChanged(String filter) {
		super.onSurveyFilterChanged(filter);
		getResponseListFragment().setSurveyId(filter);
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

		// Set the date if it was specified. If not, we default to showing the current date
		mDateFilter.setDate(getIntent().getIntExtra(TimeFilter.EXTRA_DAY, -1), getIntent().getIntExtra(TimeFilter.EXTRA_MONTH, -1), getIntent().getIntExtra(TimeFilter.EXTRA_YEAR, -1));
		mDateFilter.setOnChangeListener(new DateFilterChangeListener() {

			@Override
			public void onFilterChanged(Calendar curValue) {
				ResponseListFragment f = getResponseListFragment();
				if(f != null)
					f.setDate(curValue.get(Calendar.DATE), curValue.get(Calendar.MONTH), curValue.get(Calendar.YEAR));
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
