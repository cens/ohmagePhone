package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.db.Models.Campaign;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

public class OhmagePreferenceActivity extends PreferenceActivity  {

	private static final String KEY_REMINDERS = "key_reminders";
	private static final String KEY_ADMIN_SETTINGS = "key_admin_settings";

	private static final String STATUS_CAMPAIGN_URN = "status_campaign_urn";
	private static final String STATUS_FEEDBACK_VISIBILITY = "status_feedback_visibility";
	private static final String STATUS_PROFILE_VISIBILITY = "status_profile_visibility";
	private static final String STATUS_UPLOAD_QUEUE_VISIBILITY = "status_upload_queue_visibility";

	protected static final int CODE_ADMIN_SETTINGS = 0;

	private PreferenceScreen mReminders;
	private PreferenceScreen mAdmin;

	private UserPreferencesHelper mUserPreferenceHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUserPreferenceHelper = new UserPreferencesHelper(this);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		mReminders = (PreferenceScreen) findPreference(KEY_REMINDERS);

		if(SharedPreferencesHelper.IS_SINGLE_CAMPAIGN) {
			mReminders.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					String urn = Campaign.getSingleCampaign(OhmagePreferenceActivity.this);
					if(!TextUtils.isEmpty(urn))
						Campaign.launchTriggerActivity(OhmagePreferenceActivity.this, Campaign.getSingleCampaign(OhmagePreferenceActivity.this));
					else
						Toast.makeText(OhmagePreferenceActivity.this, R.string.preferences_no_single_campaign, Toast.LENGTH_LONG).show();
					return true;
				}
			});
		} else {
			getPreferenceScreen().removePreference(mReminders);
		}

		mAdmin = (PreferenceScreen) findPreference(KEY_ADMIN_SETTINGS);
		mAdmin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivityForResult(new Intent(OhmagePreferenceActivity.this, AdminPincodeActivity.class), CODE_ADMIN_SETTINGS);
				return true;
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		setStatusInfo();
	}

	private void setStatusInfo() {
		Preference campaignUrnStatus = findPreference(STATUS_CAMPAIGN_URN);
		if(SharedPreferencesHelper.IS_SINGLE_CAMPAIGN) {
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
