package org.ohmage.activity;


import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.fragments.CampaignListFragment;
import org.ohmage.fragments.CampaignListFragment.OnCampaignActionListener;
import org.ohmage.ui.BaseSingleFragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class CampaignAddActivity extends BaseSingleFragmentActivity implements OnCampaignActionListener{
	
	static final String TAG = "CampaignAddActivity";

	// action bar commands
	protected static final int ACTION_REFRESH_CAMPAIGNS = 1;
	
	SharedPreferencesHelper mSharedPreferencesHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentFragment(new CampaignListFragment());
		
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
		
		// throw some actions on it
		getActionBar().addActionBarCommand(ACTION_REFRESH_CAMPAIGNS, "refresh", R.drawable.dashboard_title_refresh);

		// and attach handlers for said actions
		getActionBar().setOnActionListener(new ActionListener() {
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
				getActionBar().setProgressVisible(true);
			}

			@Override
			protected void onPostExecute(CampaignReadResponse response) {
				super.onPostExecute(response);
				getActionBar().setProgressVisible(false);
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
