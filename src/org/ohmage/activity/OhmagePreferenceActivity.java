package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.db.Models.Campaign;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class OhmagePreferenceActivity extends PreferenceActivity  {

    private static final CharSequence KEY_REMINDERS = "key_reminders";
	private PreferenceScreen mReminders;
	private SharedPreferencesHelper mPreferencesHelper;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPreferencesHelper = new SharedPreferencesHelper(this);
        
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
    }
}
