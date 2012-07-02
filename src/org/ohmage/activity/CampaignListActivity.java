package org.ohmage.activity;

import edu.ucla.cens.systemlog.Log;

import org.ohmage.R;
import org.ohmage.fragments.CampaignListFragment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class CampaignListActivity extends BaseCampaignListActivity {

	static final String TAG = "CampaignListActivity";

	protected static final int ACTION_ADD_CAMPAIGNS = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentFragment(new CampaignListFragment());
	}

	@Override
	public void onContentChanged() {
		// throw some actions on it
		getActionBarControl().addActionBarCommand(ACTION_ADD_CAMPAIGNS, getString(R.string.campaign_list_add_action_button_description), R.drawable.btn_title_add);

		super.onContentChanged();
	}

	@Override
	public void onActionClicked(int commandID) {
		switch(commandID) {
			case ACTION_ADD_CAMPAIGNS:
				startActivity(new Intent(CampaignListActivity.this, CampaignAddActivity.class));
				break;
			default:
				super.onActionClicked(commandID);
		}
	}

	@Override
	public void onCampaignActionDownload(String campaignUrn) {
		Toast.makeText(this, R.string.campaign_list_download_action_invalid, Toast.LENGTH_SHORT).show();
		Log.e(TAG, "onCampaignActionDownload should not be exposed in this activity.");
	}
}
