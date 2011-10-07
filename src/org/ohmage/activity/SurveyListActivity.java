package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.activity.SurveyListFragment.OnSurveyActionListener;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Campaign;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SurveyListActivity extends FragmentActivity implements OnSurveyActionListener {
	
	static final String TAG = "SurveyListActivity";
	
	private FilterControl mCampaignFilter;
	private Button mAllButton;
	private Button mPendingButton;
	private boolean mShowPending = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.survey_list);
		
		mCampaignFilter = (FilterControl) findViewById(R.id.campaign_filter);
		
		mAllButton = (Button) findViewById(R.id.all_surveys_button);
		mPendingButton = (Button) findViewById(R.id.pending_surveys_button);

		ContentResolver cr = getContentResolver();
		String select = Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY;
		Cursor data = cr.query(Campaigns.CONTENT_URI, new String [] {Campaigns._ID, Campaigns.CAMPAIGN_NAME, Campaigns.CAMPAIGN_URN}, select, null, Campaigns.CAMPAIGN_NAME);
		mCampaignFilter.populate(data, Campaigns.CAMPAIGN_NAME, Campaigns.CAMPAIGN_URN);
		mCampaignFilter.add(0, new Pair<String, String>("All Campaigns", SurveyListFragment.FILTER_ALL_CAMPAIGNS));
		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
			
			@Override
			public void onFilterChanged(boolean selfChange, String curValue) {
				((SurveyListFragment)getSupportFragmentManager().findFragmentById(R.id.surveys)).setCampaignFilter(curValue);
			}
		});
		
		mAllButton.setOnClickListener(mPendingListener);
		mPendingButton.setOnClickListener(mPendingListener);
		
		String campaignUrn = getIntent().getStringExtra("campaign_urn");
		
		if (campaignUrn != null) {
			mCampaignFilter.setValue(campaignUrn);
		}
		
		mShowPending = getIntent().getBooleanExtra("show_pending", false);
		
		updateView();
	}
	
	View.OnClickListener mPendingListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.all_surveys_button:
				mShowPending = false;
				updateView();
				break;
				
			case R.id.pending_surveys_button:
				mShowPending = true;
				updateView();
				break;
			}
		}
	};
	
	private void updateView() {
		((SurveyListFragment)getSupportFragmentManager().findFragmentById(R.id.surveys)).setShowPending(mShowPending);
		mAllButton.setBackgroundResource(mShowPending ? R.drawable.tab_bg_unselected : R.drawable.tab_bg_selected);
		mPendingButton.setBackgroundResource(mShowPending ? R.drawable.tab_bg_selected : R.drawable.tab_bg_unselected);
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
			intent.putExtra("campaign_urn", cursor.getString(cursor.getColumnIndex(Surveys.CAMPAIGN_URN)));
			intent.putExtra("survey_id", cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_ID)));
			intent.putExtra("survey_title", cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_TITLE)));
			intent.putExtra("survey_submit_text", cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_SUBMIT_TEXT)));
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
