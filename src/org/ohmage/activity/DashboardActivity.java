package org.ohmage.activity;

import org.ohmage.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DashboardActivity extends Activity {
	private static final String TAG = "DashboardActivity";
	
	private Button mCampaignBtn;
	private Button mSurveysBtn;
	private Button mFeedbackBtn;
	private Button mUploadQueueBtn;
	private Button mProfileBtn;
	// Button settingsBtn = (Button) findViewById(R.id.dash_settings_btn);
	private Button mHelpBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.dashboard_activity);
		
		// nab a reference to the action bar so we can set it up a little bit
		// ActionBarControl actionBar = (ActionBarControl)findViewById(R.id.action_bar);

		// gather up all the buttons and tie them to the dashboard button listener
		// you'll specify what the buttons do in DashboardButtonListener rather than here
		mCampaignBtn = (Button) findViewById(R.id.dash_campaigns_btn);
		mSurveysBtn = (Button) findViewById(R.id.dash_surveys_btn);
		mFeedbackBtn = (Button) findViewById(R.id.dash_feedback_btn);
		mUploadQueueBtn = (Button) findViewById(R.id.dash_uploadqueue_btn);
		mProfileBtn = (Button) findViewById(R.id.dash_profile_btn);
		// Button settingsBtn = (Button) findViewById(R.id.dash_settings_btn);
		mHelpBtn = (Button) findViewById(R.id.dash_help_btn);
		
		DashboardButtonListener buttonListener = new DashboardButtonListener();
		
		mCampaignBtn.setOnClickListener(buttonListener);
		mSurveysBtn.setOnClickListener(buttonListener);
		mFeedbackBtn.setOnClickListener(buttonListener);
		mUploadQueueBtn.setOnClickListener(buttonListener);
		mProfileBtn.setOnClickListener(buttonListener);
		// settingsBtn.setOnClickListener(buttonListener);
		mHelpBtn.setOnClickListener(buttonListener);
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		//This is to prevent users from clicking an icon multiple times when there is delay on Dashboard somehow.
		enableAllButtons();
	}
	
	private void enableAllButtons(){
		mCampaignBtn.setClickable(true);
		mSurveysBtn.setClickable(true);
		mFeedbackBtn.setClickable(true);
		mUploadQueueBtn.setClickable(true);
		mProfileBtn.setClickable(true);
		mHelpBtn.setClickable(true);
	}
	
	private void disableAllButtons(){
		mCampaignBtn.setClickable(false);
		mSurveysBtn.setClickable(false);
		mFeedbackBtn.setClickable(false);
		mUploadQueueBtn.setClickable(false);
		mProfileBtn.setClickable(false);
		mHelpBtn.setClickable(false);		
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
					startActivity(new Intent(DashboardActivity.this, RHTabHost.class));
					break;
					
				case R.id.dash_uploadqueue_btn:
					startActivity(new Intent(c, UploadQueueActivity.class));
					break;
					
				case R.id.dash_profile_btn:
					// startActivity(new Intent(c, StatusActivity.class));
					startActivity(new Intent(c, ProfileActivity.class));
					break;
					
					/*					
				case R.id.dash_settings_btn:
					startActivity(new Intent(c, HelpActivity.class));
					break;
					*/
					
				case R.id.dash_help_btn:
					startActivity(new Intent(c, HelpActivity.class));
					break;
			}
		}
	}
}
