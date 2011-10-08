package org.ohmage.activity;


import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.activity.CampaignListFragment.OnCampaignActionListener;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaigns;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;


public class CampaignAddActivity extends FragmentActivity implements OnCampaignActionListener{
	
	static final String TAG = "CampaignAddActivity";

	// action bar commands
	protected static final int ACTION_REFRESH_CAMPAIGNS = 1;
	
	ActionBarControl mActionBar;
	
	SharedPreferencesHelper mSharedPreferencesHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.campaign_add);
		
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		
//		((CampaignListFragment)getSupportFragmentManager().findFragmentById(R.id.campaigns)).setMode(CampaignListFragment.MODE_ADD_CAMPAIGNS);
		
//		new CampaignReadTask(this) {
//
//			@Override
//			protected void onPreExecute() {
//				super.onPreExecute();
//			}
//
//			@Override
//			protected void onPostExecute(CampaignReadResponse response) {
//				super.onPostExecute(response);
//			}
//			
//		}.execute(mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword());
		
		// get a reference to the action bar so we can attach to it
		mActionBar = (ActionBarControl) findViewById(R.id.action_bar);
		
		// throw some actions on it
		mActionBar.addActionBarCommand(ACTION_REFRESH_CAMPAIGNS, "refresh", R.drawable.dashboard_title_refresh);

		// and attach handlers for said actions
		mActionBar.setOnActionListener(new ActionListener() {
			@Override
			public void onActionClicked(int commandID) {
				switch(commandID) {
					case ACTION_REFRESH_CAMPAIGNS:
						refreshCampaigns();
						break;
				}
			}
		});
	}
	
	private void refreshCampaigns() {
		new CampaignReadTask(this) {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				mActionBar.setProgressVisible(true);
			}

			@Override
			protected void onPostExecute(CampaignReadResponse response) {
				super.onPostExecute(response);
				mActionBar.setProgressVisible(false);
			}
			
		}.execute(mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword());
	}
	
	@Override
	public void onCampaignActionView(String campaignUrn) {
		Intent i = new Intent(this, CampaignInfoActivity.class);
		i.setData(Campaigns.buildCampaignUri(campaignUrn));
		startActivity(i);
	}
	
	@Override
	public void onCampaignActionDownload(String campaignUrn) {
		new CampaignXmlDownloadTask(this, campaignUrn).execute(mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword());
	}

	@Override
	public void onCampaignActionSurveys(String campaignUrn) {
//		Toast.makeText(this, "The Surveys action should not be exposed in this activity!", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "onCampaignActionSurveys should not be exposed in this activity.");
	}

	@Override
	public void onCampaignActionError(String campaignUrn, int status) {
//		Toast.makeText(this, "The error action should not be exposed in this activity!", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "onCampaignActionError should not be exposed in this activity.");
	}
}
