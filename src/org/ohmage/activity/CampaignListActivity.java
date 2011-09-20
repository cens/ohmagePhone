package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.activity.CampaignListFragment.OnCampaignActionListener;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaign;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class CampaignListActivity extends FragmentActivity implements OnCampaignActionListener {
	
	static final String TAG = "CampaignListActivity";
	
	SharedPreferencesHelper mSharedPreferencesHelper;

	private ActionBarControl mActionBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.campaign_list);
		
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		
//		((CampaignListFragment)getSupportFragmentManager().findFragmentById(R.id.campaigns)).setMode(CampaignListFragment.MODE_MY_CAMPAIGNS);
		
		// get a reference to the action bar so we can attach to it
		mActionBar = (ActionBarControl) findViewById(R.id.action_bar);
		
		// throw some actions on it
		mActionBar.addActionBarCommand(1, "add campaign", R.drawable.dashboard_title_add);
		mActionBar.addActionBarCommand(2, "refresh", R.drawable.dashboard_title_refresh);

		// and attach handlers for said actions
		mActionBar.setOnActionListener(new ActionListener() {
			@Override
			public void onActionClicked(int commandID) {
				switch(commandID) {
					case 1:
						startActivity(new Intent(CampaignListActivity.this, CampaignAddActivity.class));
						break;
					case 2:
						refreshCampaigns();
						break;
				}
			}
		});
		
		refreshCampaigns();
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
		i.setData(Campaign.getCampaignByURN(campaignUrn));
		startActivity(i);
	}

	@Override
	public void onCampaignActionDownload(String campaignUrn) {
		Toast.makeText(this, "The Download action should not be exposed in this activity!", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "onCampaignActionDownload should not be exposed in this activity.");
	}

	@Override
	public void onCampaignActionSurveys(String campaignUrn) {
		Intent intent = new Intent(this, SurveyListActivity.class);
		intent.putExtra("campaign_urn", campaignUrn);
		startActivity(intent);
	}

	@Override
	public void onCampaignActionError(String campaignUrn) {
		Toast.makeText(this, "Showing Error Dialog", Toast.LENGTH_SHORT).show();
	}

}
