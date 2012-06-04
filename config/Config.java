package org.ohmage;

import org.ohmage.Config;
import org.ohmage.OhmageApplication;

import android.preference.PreferenceManager;

public class Config {

	public static final String DEFAULT_SERVER_URL = @CONFIG.SERVER_URL@;
	public static final boolean IS_SINGLE_CAMPAIGN = @CONFIG.SINGLE_CAMPAIGN@;
	public static final boolean ALLOWS_FEEDBACK = @CONFIG.ALLOWS_FEEDBACK@;
	public static final boolean ADMIN_MODE = @CONFIG.ADMIN_MODE@;
	public static final boolean REMINDER_ADMIN_MODE = @CONFIG.REMINDER_ADMIN_MODE@;
	public static final boolean LOG_ANALYTICS = @CONFIG.LOG_ANALYTICS@;
	public static final String LOG_LEVEL = @CONFIG.LOG_LEVEL@;

	private static final String KEY_SERVER_URL = "key_server_url";

	private static String serverUrl;

	public static String serverUrl() {
		if(serverUrl == null)
			serverUrl = PreferenceManager.getDefaultSharedPreferences(OhmageApplication.getContext()).getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
		return serverUrl;
	}

	public static void setServerUrl(String url) {
		serverUrl = url;
		PreferenceManager.getDefaultSharedPreferences(OhmageApplication.getContext()).edit().putString(KEY_SERVER_URL, url).commit();
	}
}