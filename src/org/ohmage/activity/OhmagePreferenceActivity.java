package org.ohmage.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

import org.ohmage.ConfigHelper;
import org.mobilizingcs.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.db.Models.Campaign;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.Log;
import org.ohmage.logprobe.LogProbe.Status;

public class OhmagePreferenceActivity extends PreferenceActivity  {
	private static final String TAG = "OhmagePreferenceActivity";

	private static final String KEY_REMINDERS = "key_reminders";
	private static final String KEY_ADMIN_SETTINGS = "key_admin_settings";

	private static final String STATUS_CAMPAIGN_URN = "status_campaign_urn";
	private static final String STATUS_SERVER_URL = "status_server_url";
	private static final String STATUS_FEEDBACK_VISIBILITY = "status_feedback_visibility";
	private static final String STATUS_PROFILE_VISIBILITY = "status_profile_visibility";
	private static final String STATUS_UPLOAD_QUEUE_VISIBILITY = "status_upload_queue_visibility";
	private static final String STATUS_MOBILITY_VISIBILITY = "status_mobility_visibility";

	private static final String INFO_OHMAGE_VERSION = "info_ohmage_version";

	protected static final int CODE_ADMIN_SETTINGS = 0;

	private PreferenceScreen mReminders;
	private PreferenceScreen mAdmin;

	private UserPreferencesHelper mUserPreferenceHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(UserPreferencesHelper.getPreferencesName(this));
        prefMgr.setSharedPreferencesMode(MODE_PRIVATE);

		mUserPreferenceHelper = new UserPreferencesHelper(this);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		mReminders = (PreferenceScreen) findPreference(KEY_REMINDERS);
		mReminders.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				String urn = Campaign.getSingleCampaign(OhmagePreferenceActivity.this);
				if(!TextUtils.isEmpty(urn)) {
					Intent triggers = Campaign.launchTriggerIntent(OhmagePreferenceActivity.this, Campaign.getSingleCampaign(OhmagePreferenceActivity.this));
					startActivity(triggers);
				} else
					Toast.makeText(OhmagePreferenceActivity.this, R.string.preferences_no_single_campaign, Toast.LENGTH_LONG).show();
				return true;
			}
		});

		mAdmin = (PreferenceScreen) findPreference(KEY_ADMIN_SETTINGS);
		mAdmin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivityForResult(new Intent(OhmagePreferenceActivity.this, AdminPincodeActivity.class), CODE_ADMIN_SETTINGS);
				return true;
			}
		});

		findPreference(STATUS_SERVER_URL).setSummary(ConfigHelper.serverUrl());

		try {
			findPreference(INFO_OHMAGE_VERSION).setSummary(getPackageManager().getPackageInfo("org.ohmage", 0).versionName);
		} catch (Exception e) {
			Log.e(TAG, "unable to retrieve version", e);
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		Analytics.activity(this, Status.ON);

		// Hide and show reminders setting if we are in single campaign mode or not
		if(ConfigHelper.isSingleCampaignMode()) {
			getPreferenceScreen().addPreference(mReminders);
		} else {
			getPreferenceScreen().removePreference(mReminders);
		}

		setStatusInfo();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Analytics.activity(this, Status.OFF);
	}

	private void setStatusInfo() {
		Preference campaignUrnStatus = findPreference(STATUS_CAMPAIGN_URN);
		if(ConfigHelper.isSingleCampaignMode()) {
			campaignUrnStatus.setTitle(R.string.preferences_single_campaign_status);
			campaignUrnStatus.setSummary(Campaign.getSingleCampaign(this));
			if(campaignUrnStatus.getSummary() == null)
				campaignUrnStatus.setSummary(R.string.unknown);
		} else {
			campaignUrnStatus.setTitle(R.string.preferences_muli_campaign_status);
			campaignUrnStatus.setSummary(null);
		}

		findPreference(STATUS_FEEDBACK_VISIBILITY).setSummary(mUserPreferenceHelper.showFeedback() ? R.string.shown : R.string.hidden);
		findPreference(STATUS_PROFILE_VISIBILITY).setSummary(mUserPreferenceHelper.showProfile() ? R.string.shown : R.string.hidden);
		findPreference(STATUS_UPLOAD_QUEUE_VISIBILITY).setSummary(mUserPreferenceHelper.showUploadQueue() ? R.string.shown : R.string.hidden);
		findPreference(STATUS_MOBILITY_VISIBILITY).setSummary(mUserPreferenceHelper.showMobility() ? R.string.shown : R.string.hidden);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
			case CODE_ADMIN_SETTINGS:
				if(resultCode == RESULT_OK)
					startActivity(new Intent(OhmagePreferenceActivity.this, AdminSettingsActivity.class));
				break;
		}
	}
}
