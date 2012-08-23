package org.ohmage.activity;


import edu.ucla.cens.systemlog.Log;

import org.ohmage.fragments.CampaignListFragment;

import android.os.Bundle;

public class CampaignAddActivity extends BaseCampaignListActivity {

	static final String TAG = "CampaignAddActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentFragment(CampaignListFragment.newInstance(CampaignListFragment.MODE_ADD_CAMPAIGNS));
	}

	@Override
	public void onCampaignActionSurveys(String campaignUrn) {
		Log.e(TAG, "onCampaignActionSurveys should not be exposed in this activity.");
	}

	@Override
	public void onCampaignActionError(String campaignUrn, int status) {
		Log.e(TAG, "onCampaignActionError should not be exposed in this activity.");
	}
}
