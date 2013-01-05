/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Preference helper for application wide settings. Any user specific settings are
 * specified in {@link UserPreferencesHelper} which is cleared on logout
 * @author cketcham
 *
 */
public class ConfigHelper {

	private static final String KEY_VERSION_CODE = "version_code";
	private static final String KEY_IS_FIRST_RUN = "is_first_run";
	private static final String KEY_MOBILITY_VERSION = "mobility_version";
	private static final String KEY_SERVER_URL = "key_server_url";

	private static String serverUrl;
	private final SharedPreferences mPreferences;

	public ConfigHelper(Context context) {
		mPreferences = getSharedPreferences(context);
	}

	private static SharedPreferences getSharedPreferences(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public int getLastVersionCode() {
		return mPreferences.getInt(KEY_VERSION_CODE, -1);
	}

	public boolean setLastVersionCode(int versionCode) {
		return mPreferences.edit().putInt(KEY_VERSION_CODE, versionCode).commit();
	}

	public boolean isFirstRun() {
		return mPreferences.getBoolean(KEY_IS_FIRST_RUN, true);
	}

	public boolean setFirstRun(boolean isFirstRun) {
		return mPreferences.edit().putBoolean(KEY_IS_FIRST_RUN, isFirstRun).commit();
	}

	public int getLastMobilityVersion() {
		return mPreferences.getInt(KEY_MOBILITY_VERSION, -1);
	}

	public boolean setMobilityVersion(int mobilityVersion) {
		return mPreferences.edit().putInt(KEY_MOBILITY_VERSION, mobilityVersion).commit();
	}

	public static void setServerUrl(String url) {
		serverUrl = url;
		PreferenceManager.getDefaultSharedPreferences(OhmageApplication.getContext()).edit().putString(KEY_SERVER_URL, url).commit();
	}

	public static String serverUrl() {
		if(serverUrl == null)
			serverUrl = PreferenceManager.getDefaultSharedPreferences(OhmageApplication.getContext()).getString(KEY_SERVER_URL, null);
		return serverUrl;
	}

    public static boolean isSingleCampaignMode() {
        UserPreferencesHelper prefHelper = new UserPreferencesHelper(OhmageApplication.getContext());
        return prefHelper.isSingleCampaignMode();
    }
}
