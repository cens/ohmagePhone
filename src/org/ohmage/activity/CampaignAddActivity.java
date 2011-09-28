package org.ohmage.activity;


import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.activity.CampaignListFragment.OnCampaignActionListener;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.db.DbContract.Campaigns;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;


public class CampaignAddActivity extends FragmentActivity implements OnCampaignActionListener{
	
	static final String TAG = "CampaignAddActivity";
	
	ActionBarControl mActionBar;
	
	SharedPreferencesHelper mSharedPreferencesHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.campaign_add);
		
		mActionBar = (ActionBarControl) findViewById(R.id.action_bar);
		
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
		Toast.makeText(this, "The Surveys action should not be exposed in this activity!", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "onCampaignActionSurveys should not be exposed in this activity.");
	}

	@Override
	public void onCampaignActionError(String campaignUrn, int status) {
		Toast.makeText(this, "The error action should not be exposed in this activity!", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "onCampaignActionError should not be exposed in this activity.");
	}
}
