package org.ohmage.activity;

import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.fragments.CampaignListFragment.OnCampaignActionListener;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
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
	}

	private void refreshCampaigns() {
		mSharedPreferencesHelper.setLastCampaignRefreshTime(System.currentTimeMillis());
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
		Toast.makeText(this, "The Download action should not be exposed in this activity!", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "onCampaignActionDownload should not be exposed in this activity.");
	}

	@Override
	public void onCampaignActionSurveys(String campaignUrn) {
		Intent intent = new Intent(this, SurveyListActivity.class);
		intent.putExtra(CampaignFilter.EXTRA_CAMPAIGN_URN, campaignUrn);
		startActivity(intent);
	}

	@Override
	public void onCampaignActionError(String campaignUrn, int status) {
		
		Bundle bundle = new Bundle();
		bundle.putString("campaign_urn", campaignUrn);
		showDialog(status, bundle);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int, android.os.Bundle)
	 */
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		String message = "This campaign is unavailable.";
		
		switch (id) {
		case Campaign.STATUS_STOPPED:
			message = "This campaign is stopped.";
			break;
		case Campaign.STATUS_OUT_OF_DATE:
			message = "This campaign is out of date";
			break;
		case Campaign.STATUS_INVALID_USER_ROLE:
			message = "Invalid user role.";
			break;
		case Campaign.STATUS_NO_EXIST:
			message = "This campaign no longer exists.";
			break;
		}
		
		builder.setMessage(message)
				.setCancelable(true)
				.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						ContentResolver cr = getContentResolver();
						cr.delete(Campaigns.CONTENT_URI, Campaigns.CAMPAIGN_URN + "= '" + campaignUrnForDialogs + "'", null);
					}
				}).setNegativeButton("Ignore", null);
		
		return builder.create();
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		super.onPrepareDialog(id, dialog, args);
		campaignUrnForDialogs = args.getString("campaign_urn");
	}
	
	private String campaignUrnForDialogs;
}
