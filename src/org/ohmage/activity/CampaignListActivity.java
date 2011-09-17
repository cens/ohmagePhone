package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.activity.CampaignListFragment.OnCampaignActionListener;

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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.campaign_list);
		
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		
//		((CampaignListFragment)getSupportFragmentManager().findFragmentById(R.id.campaigns)).setMode(CampaignListFragment.MODE_MY_CAMPAIGNS);
		
		((Button) findViewById(R.id.add_campaigns)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(CampaignListActivity.this, CampaignAddActivity.class));
			}
		});
		
		refreshCampaigns();
	}

	private void refreshCampaigns() {
		new CampaignReadTask(this) {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
			}

			@Override
			protected void onPostExecute(CampaignReadResponse response) {
				super.onPostExecute(response);
			}
			
		}.execute(mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword());
	}

	@Override
	public void onCampaignActionView(String campaignUrn) {
		Toast.makeText(this, "Launching Campaign Info Activity", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCampaignActionDownload(String campaignUrn) {
		Toast.makeText(this, "The Download action should not be exposed in this activity!", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "onCampaignActionDownload should not be exposed in this activity.");
	}

	@Override
	public void onCampaignActionSurveys(String campaignUrn) {
		Toast.makeText(this, "Launching Survey List Activity", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCampaignActionError(String campaignUrn) {
		Toast.makeText(this, "Showing Error Dialog", Toast.LENGTH_SHORT).show();
	}

}
