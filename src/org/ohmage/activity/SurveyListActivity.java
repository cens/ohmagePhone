package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.activity.SurveyListFragment.OnSurveyActionListener;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Survey;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SurveyListActivity extends FragmentActivity implements OnSurveyActionListener {
	
	static final String TAG = "SurveyListActivity";
	
	private FilterControl mCampaignFilter;
	
	private SharedPreferencesHelper mSharedPreferencesHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.survey_list);
		
		mCampaignFilter = (FilterControl) findViewById(R.id.campaign_filter);

		ContentResolver cr = getContentResolver();
		String select = Campaign.STATUS + "=" + Campaign.STATUS_READY;
		Cursor data = cr.query(Campaign.CONTENT_URI, new String [] {Campaign._ID, Campaign.NAME, Campaign.URN}, select, null, Campaign.NAME);
		mCampaignFilter.populate(data, Campaign.NAME, Campaign.URN);
		mCampaignFilter.add(0, new Pair<String, String>("All Campaigns", SurveyListFragment.FILTER_ALL_CAMPAIGNS));
		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
			
			@Override
			public void onFilterChanged(String curValue) {
				// TODO Auto-generated method stub
				((SurveyListFragment)getSupportFragmentManager().findFragmentById(R.id.surveys)).setCampaignFilter(curValue);
			}
		});
		
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		
		String campaignUrn = getIntent().getStringExtra("campaign_urn");
		
		if (campaignUrn != null) {
			mCampaignFilter.setValue(campaignUrn);
		}
	}

	@Override
	public void onSurveyActionView(Survey survey) {
		Toast.makeText(this, "Launching Survey Info Activity", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onSurveyActionStart(Survey survey) {
		Toast.makeText(this, "Launching Survey Activity", Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(this, SurveyActivity.class);
		intent.putExtra("campaign_urn", survey.mCampaignUrn);
		intent.putExtra("survey_id", survey.mSurveyID);
		intent.putExtra("survey_title", survey.mTitle);
		intent.putExtra("survey_submit_text", survey.mSubmitText);
		startActivity(intent);
	}

	@Override
	public void onSurveyActionUnavailable(Survey survey) {
		Toast.makeText(this, "This survey can only be taken when triggered.", Toast.LENGTH_SHORT).show();
	}

}
