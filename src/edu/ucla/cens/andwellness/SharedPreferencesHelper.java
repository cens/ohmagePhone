package edu.ucla.cens.andwellness;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {
	
	public static final int CAMPAIGN_XML_RESOURCE_ID = R.raw.nih_all;
	
	private static final String PREFERENCES_NAME = "preferences_name";
	public static final String PREFERENCES_CREDENTIALS = "preferences_credentials";
	public static final String PREFERENCES_TRIGGERS = "preferences_triggers";
	public static final String PREFERENCES_SUBMISSIONS = "preferences_submissions";
	
	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD_HASHED = "hashedPassword";
	private static final String KEY_IS_FIRST_RUN = "is_first_run";
	private static final String KEY_IS_AUTHENTICATED = "is_authenticated";
	private static final String KEY_LAST_MOBILITY_UPLOAD_TIMESTAMP = "last_mobility_upload_timestamp";
	private static final String KEY_CAMPAIGN_NAME = "campaign_name";
	private static final String KEY_CAMPAIGN_VERSION = "campaign_version";

	private SharedPreferences mPreferences;
	
	public SharedPreferencesHelper(Context context) {
		mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
	}
	
	public SharedPreferencesHelper(Context context, String prefname, String username) {
		mPreferences = context.getSharedPreferences(prefname + username, Context.MODE_PRIVATE);
	}
	
	public String getUsername() {
		return mPreferences.getString(KEY_USERNAME, "");
	}
	
	public void putUsername(String username) {
		mPreferences.edit().putString(KEY_USERNAME, username).commit();
	}
	
	public String getHashedPassword() {
		return mPreferences.getString(KEY_PASSWORD_HASHED, "");
	}
	
	public void putHashedPassword(String hashedPassword) {
		mPreferences.edit().putString(KEY_PASSWORD_HASHED, hashedPassword).commit();
	}
	
	public void clearCredentials() {
		mPreferences.edit().remove(KEY_USERNAME).remove(KEY_PASSWORD_HASHED).commit();
	}
	
	public boolean isFirstRun() {
		return mPreferences.getBoolean(KEY_IS_FIRST_RUN, true);
	}
	
	public void setFirstRun(boolean isFirstRun) {
		mPreferences.edit().putBoolean(KEY_IS_FIRST_RUN, isFirstRun).commit();
	}
	
	public Long getLastMobilityUploadTimestamp() {
		return mPreferences.getLong(KEY_LAST_MOBILITY_UPLOAD_TIMESTAMP, 0);
	}
	
	public void putLastMobilityUploadTimestamp(Long timestamp) {
		mPreferences.edit().putLong(KEY_LAST_MOBILITY_UPLOAD_TIMESTAMP, timestamp).commit();
	}
	
	public String getCampaignName() {
		return mPreferences.getString(KEY_CAMPAIGN_NAME, "");
	}
	
	public void putCampaignName(String campaignName) {
		mPreferences.edit().putString(KEY_CAMPAIGN_NAME, campaignName).commit();
	}
	
	public String getCampaignVersion() {
		return mPreferences.getString(KEY_CAMPAIGN_VERSION, "");
	}
	
	public void putCampaignVersion(String campaignVersion) {
		mPreferences.edit().putString(KEY_CAMPAIGN_VERSION, campaignVersion).commit();
	}

	/*public boolean isAuthenticated() {
		return mPreferences.getBoolean(KEY_IS_AUTHENTICATED, false);
	}
	
	public void setAuthenticated(boolean isAuthenticated) {
		mPreferences.edit().putBoolean(KEY_IS_AUTHENTICATED, isAuthenticated).commit();
	}*/
}
