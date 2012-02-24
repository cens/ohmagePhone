package org.ohmage.activity;


import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.async.CampaignReadLoaderCallbacks;
import org.ohmage.async.CampaignXmlDownloadTask;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.fragments.CampaignListFragment.OnCampaignActionListener;
import org.ohmage.ui.BaseSingleFragmentActivity;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;


public class BaseCampaignListActivity extends BaseSingleFragmentActivity implements OnCampaignActionListener, ActionListener{

	static final String TAG = "BaseCampaignListActivity";

	// action bar commands
	protected static final int ACTION_REFRESH_CAMPAIGNS = 0;

	SharedPreferencesHelper mSharedPreferencesHelper;

	private CampaignReadLoaderCallbacks mCampaignReadLoader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSharedPreferencesHelper = new SharedPreferencesHelper(this);

		mCampaignReadLoader = new CampaignReadLoaderCallbacks(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mCampaignReadLoader.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mCampaignReadLoader.onSaveInstanceState(outState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mCampaignReadLoader.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		mCampaignReadLoader.onCreate();

		// throw some actions on it
		getActionBar().addActionBarCommand(ACTION_REFRESH_CAMPAIGNS, "refresh", R.drawable.dashboard_title_refresh);

		// and attach handlers for said actions
		getActionBar().setOnActionListener(this);
	}

	@Override
	public void onCampaignActionView(String campaignUrn) {
		Intent i = new Intent(this, CampaignInfoActivity.class);
		i.setData(Campaigns.buildCampaignUri(campaignUrn));
		startActivity(i);
	}

	@Override
	public void onCampaignActionDownload(final String campaignUrn) {
		new CampaignXmlDownloadTask(BaseCampaignListActivity.this, campaignUrn, mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword()).startLoading();
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
				Campaign.setRemote(BaseCampaignListActivity.this, campaignUrnForDialogs);
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

	@Override
	public void onActionClicked(int commandID) {
		switch(commandID) {
			case ACTION_REFRESH_CAMPAIGNS:
				mCampaignReadLoader.forceLoad();
				break;
		}
	}
}
