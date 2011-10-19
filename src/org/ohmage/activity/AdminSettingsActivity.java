package org.ohmage.activity;

import org.ohmage.AccountHelper;
import org.ohmage.R;

import android.app.Dialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class AdminSettingsActivity extends PreferenceActivity  {

	private static final String KEY_UPDATE_PASSWORD = "key_update_password";
	private static final String KEY_LOGOUT = "key_logout";

	private PreferenceScreen mUpdatePassword;
	private PreferenceScreen mLogout;

	private AccountHelper mAccountHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.admin_settings);

		mAccountHelper = new AccountHelper(this);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.admin_preferences);

		mUpdatePassword = (PreferenceScreen) findPreference(KEY_UPDATE_PASSWORD);
		mUpdatePassword.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				mAccountHelper.updatePassword();
				return true;
			}
		});

		mLogout = (PreferenceScreen) findPreference(KEY_LOGOUT);
		mLogout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				mAccountHelper.logout();
				return true;
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = mAccountHelper.onCreateDialog(id);
		if(dialog == null)
			dialog = super.onCreateDialog(id);
		return dialog;
	}
}