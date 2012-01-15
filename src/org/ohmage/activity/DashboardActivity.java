package org.ohmage.activity;

import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.async.CampaignReadTask;
import org.ohmage.ui.BaseActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DashboardActivity extends BaseActivity {
	private static final String TAG = "DashboardActivity";
	
	private Button mCampaignBtn;
	private Button mSurveysBtn;
	private Button mFeedbackBtn;
	private Button mUploadQueueBtn;
	private Button mProfileBtn;
	private Button mHelpBtn;
	private Button mMobilityBtn;
	private Button mSettingsBtn;

	private SharedPreferencesHelper mSharedPreferencesHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.dashboard_layout);
		getActionBar().setShowLogo(true);
		
		// gather up all the buttons and tie them to the dashboard button listener
		// you'll specify what the buttons do in DashboardButtonListener rather than here
		mCampaignBtn = (Button) findViewById(R.id.dash_campaigns_btn);
		mSurveysBtn = (Button) findViewById(R.id.dash_surveys_btn);
		mFeedbackBtn = (Button) findViewById(R.id.dash_feedback_btn);
		mUploadQueueBtn = (Button) findViewById(R.id.dash_uploadqueue_btn);
		mProfileBtn = (Button) findViewById(R.id.dash_profile_btn);
		mHelpBtn = (Button) findViewById(R.id.dash_help_btn);
		mMobilityBtn = (Button) findViewById(R.id.dash_mobility_btn);

		DashboardButtonListener buttonListener = new DashboardButtonListener();

		mCampaignBtn.setOnClickListener(buttonListener);
		mSurveysBtn.setOnClickListener(buttonListener);
		mFeedbackBtn.setOnClickListener(buttonListener);
		mUploadQueueBtn.setOnClickListener(buttonListener);
		mProfileBtn.setOnClickListener(buttonListener);
		mHelpBtn.setOnClickListener(buttonListener);
		mMobilityBtn.setOnClickListener(buttonListener);

		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		
		// refresh campaigns
		refreshCampaigns();
	}
	
	private void ensureUI() {
		if(SharedPreferencesHelper.IS_SINGLE_CAMPAIGN) {
			mCampaignBtn.setVisibility(View.GONE);
		} else {
			mCampaignBtn.setVisibility(View.VISIBLE);
		}
		
		UserPreferencesHelper userPrefs = new UserPreferencesHelper(this);
		
		if(userPrefs.showProfile())
			mProfileBtn.setVisibility(View.VISIBLE);
		else
			mProfileBtn.setVisibility(View.GONE);

		if(userPrefs.showFeedback())
			mFeedbackBtn.setVisibility(View.VISIBLE);
		else
			mFeedbackBtn.setVisibility(View.GONE);
		
		if(userPrefs.showUploadQueue())
			mUploadQueueBtn.setVisibility(View.VISIBLE);
		else
			mUploadQueueBtn.setVisibility(View.GONE);
		
		if(userPrefs.showMobility())
			mMobilityBtn.setVisibility(View.VISIBLE);
		else
			mMobilityBtn.setVisibility(View.GONE);
	}

	private void refreshCampaigns() {
		getSupportLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<CampaignReadResponse>() {

			@Override
			public Loader<CampaignReadResponse> onCreateLoader(int id, Bundle args) {
				if(mSharedPreferencesHelper.getLastCampaignRefreshTime() + DateUtils.MINUTE_IN_MILLIS * 5 < System.currentTimeMillis()) {
					getActionBar().setProgressVisible(true);
					return new CampaignReadTask(DashboardActivity.this, mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword());

				}
				return null;
			}

			@Override
			public void onLoadFinished(Loader<CampaignReadResponse> loader,
					CampaignReadResponse data) {
				mSharedPreferencesHelper.setLastCampaignRefreshTime(System.currentTimeMillis());
				getActionBar().setProgressVisible(false);
			}

			@Override
			public void onLoaderReset(Loader<CampaignReadResponse> loader) {
			}
		});
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		//This is to prevent users from clicking an icon multiple times when there is delay on Dashboard somehow.
		enableAllButtons();
		
		ensureUI();
	}
	
	private void enableAllButtons(){
		mCampaignBtn.setClickable(true);
		mSurveysBtn.setClickable(true);
		mFeedbackBtn.setClickable(true);
		mUploadQueueBtn.setClickable(true);
		mProfileBtn.setClickable(true);
		mHelpBtn.setClickable(true);
		mMobilityBtn.setClickable(true);
	}
	
	private void disableAllButtons(){
		mCampaignBtn.setClickable(false);
		mSurveysBtn.setClickable(false);
		mFeedbackBtn.setClickable(false);
		mUploadQueueBtn.setClickable(false);
		mProfileBtn.setClickable(false);
		mHelpBtn.setClickable(false);	
		mMobilityBtn.setClickable(false);
	}
	
	protected class DashboardButtonListener implements OnClickListener {		
		@Override
		public void onClick(View v) {
			Context c = v.getContext();
			Intent intent;;
			disableAllButtons();
			switch (v.getId()) {
				case R.id.dash_campaigns_btn:
					startActivity(new Intent(c, CampaignListActivity.class));
					break;
					
				case R.id.dash_surveys_btn:
					startActivity(new Intent(c, SurveyListActivity.class));
					break;
					
				case R.id.dash_feedback_btn:
					startActivity(new Intent(DashboardActivity.this, ResponseHistoryActivity.class));
					break;
					
				case R.id.dash_uploadqueue_btn:
					startActivity(new Intent(c, UploadQueueActivity.class));
					break;
					
				case R.id.dash_profile_btn:
					// startActivity(new Intent(c, StatusActivity.class));
					startActivity(new Intent(c, ProfileActivity.class));
					break;

				case R.id.dash_help_btn:
					startActivity(new Intent(c, HelpActivity.class));
					break;
					
				case R.id.dash_mobility_btn:
					startActivity(new Intent(c, MobilityActivity.class));
					break;
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		menu.add(0,1,0,R.string.menu_settings);
		menu.findItem(1).setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()){
			case 1:
				startActivity(new Intent(this, OhmagePreferenceActivity.class));
				return true;
		}
		return false;
	}
}
