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
	private SharedPreferencesHelper mPreferencesHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPreferencesHelper = new SharedPreferencesHelper(this);
		mUserPreferenceHelper = new UserPreferencesHelper(this);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		mReminders = (PreferenceScreen) findPreference(KEY_REMINDERS);

		if(SharedPreferencesHelper.IS_SINGLE_CAMPAIGN) {
			mReminders.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Campaign.launchTriggerActivity(OhmagePreferenceActivity.this, mPreferencesHelper.getCampaignUrn());
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
			campaignUrnStatus.setTitle("Single-Campaign Mode");
			campaignUrnStatus.setSummary(mPreferencesHelper.getCampaignUrn());
		} else {
			campaignUrnStatus.setTitle("Multi-Campaign Mode");
			campaignUrnStatus.setSummary(null);
		}

		findPreference(STATUS_FEEDBACK_VISIBILITY).setSummary(mUserPreferenceHelper.showFeedback() ? "Shown" : "Hidden");
		findPreference(STATUS_PROFILE_VISIBILITY).setSummary(mUserPreferenceHelper.showProfile() ? "Shown" : "Hidden");
		findPreference(STATUS_UPLOAD_QUEUE_VISIBILITY).setSummary(mUserPreferenceHelper.showUploadQueue() ? "Shown" : "Hidden");
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
