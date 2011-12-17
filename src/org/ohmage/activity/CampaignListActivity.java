package org.ohmage.activity;

import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.fragments.CampaignListFragment;
import org.ohmage.fragments.CampaignListFragment.OnCampaignActionListener;
import org.ohmage.ui.BaseSingleFragmentActivity;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class CampaignListActivity extends BaseSingleFragmentActivity implements OnCampaignActionListener {
	
	static final String TAG = "CampaignListActivity";
	
	SharedPreferencesHelper mSharedPreferencesHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentFragment(new CampaignListFragment());
		
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		
//		((CampaignListFragment)getSupportFragmentManager().findFragmentById(R.id.campaigns)).setMode(CampaignListFragment.MODE_MY_CAMPAIGNS);

		// throw some actions on it
		getActionBar().addActionBarCommand(1, "add campaign", R.drawable.dashboard_title_add);
		getActionBar().addActionBarCommand(2, "refresh", R.drawable.dashboard_title_refresh);

		// and attach handlers for said actions
		getActionBar().setOnActionListener(new ActionListener() {
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
		Toast.makeText(this, R.string.campaign_list_download_action_invalid, Toast.LENGTH_SHORT).show();
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
	protected Dialog onCreateDialog(final int id, Bundle args) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch (id) {
		case Campaign.STATUS_STOPPED:
			builder.setMessage(R.string.campaign_list_campaign_stopped);
			break;
		case Campaign.STATUS_OUT_OF_DATE:
			builder.setMessage(R.string.campaign_list_campaign_out_of_date);
			break;
		case Campaign.STATUS_INVALID_USER_ROLE:
			builder.setMessage(R.string.campaign_list_campaign_invalid_user_role);
			break;
		case Campaign.STATUS_NO_EXIST:
			builder.setMessage(R.string.campaign_list_campaign_no_exist);
			break;
		default:
			builder.setMessage(R.string.campaign_list_campaign_unavailable);
		}

		builder.setCancelable(true)
		.setNegativeButton(R.string.ignore, null)
		.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(id == Campaign.STATUS_OUT_OF_DATE)
					Campaign.setRemote(CampaignListActivity.this, campaignUrnForDialogs);
				else {
					ContentResolver cr = getContentResolver();
					cr.delete(Campaigns.CONTENT_URI, Campaigns.CAMPAIGN_URN + "=?", new String[] { campaignUrnForDialogs });
				}
			}
		});

		return builder.create();
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		super.onPrepareDialog(id, dialog, args);
		campaignUrnForDialogs = args.getString("campaign_urn");
	}
	
	private String campaignUrnForDialogs;
}
