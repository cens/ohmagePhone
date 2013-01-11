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
import android.database.Cursor;
import android.preference.PreferenceManager;

import org.ohmage.db.DbContract.Responses;

import java.util.Calendar;

public class UserPreferencesHelper {

    private static final boolean DEFAULT_SHOW_FEEDBACK = true;
    private static final boolean DEFAULT_SHOW_PROFILE = true;
    private static final boolean DEFAULT_SHOW_UPLOAD_QUEUE = true;
    private static final boolean DEFAULT_SHOW_MOBILITY = false;
    private static final boolean DEFAULT_SHOW_MOBILITY_FEEDBACK = false;

    public static final String KEY_SHOW_FEEDBACK = "key_show_feedback";
    public static final String KEY_SHOW_PROFILE = "key_show_profile";
    public static final String KEY_SHOW_UPLOAD_QUEUE = "key_show_upload_queue";
    public static final String KEY_SHOW_MOBILITY = "key_show_mobility";
    public static final String KEY_SHOW_MOBILITY_FEEDBACK = "key_show_mobility_feedback";
    private static final String KEY_BASELINE_END_TIME = "key_baseline_end_time";
    private static final String KEY_BASELINE_START_TIME = "key_baseline_start_time";
    private static final String KEY_UPLOAD_PROBES_WIFI_ONLY = "key_upload_probes_wifi_only";
    private static final String KEY_UPLOAD_RESPONSES_WIFI_ONLY = "key_upload_responses_wifi_only";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD_HASHED = "hashedPassword";
    private static final String KEY_IS_DISABLED = "is_disabled";
    private static final String KEY_LAST_PROBE_UPLOAD_TIMESTAMP = "last_probe_upload_timestamp";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";
    private static final String KEY_LAST_SURVEY_TIMESTAMP = "last_timestamp_";
    private static final String KEY_LAST_FEEDBACK_REFRESH_TIMESTAMP = "last_fb_refresh_timestamp";
    private static final String KEY_CAMPAIGN_REFRESH_TIME = "campaign_refresh_time";
    public static final String KEY_SINGLE_CAMPAIGN_MODE = "key_single_campaign_mode";

    private final SharedPreferences mPreferences;

    public UserPreferencesHelper(Context activity) {
        mPreferences = getUserSharedPreferences(activity);
    }

    public SharedPreferences getUserSharedPreferences(Context context) {
        return context.getSharedPreferences(getPreferencesName(context), Context.MODE_PRIVATE);
    }

    public static String getPreferencesName(Context context) {
        return context.getPackageName() + "_user_preferences";
    }

    public boolean clearAll() {
        return mPreferences.edit().clear().commit();
    }

    public boolean showFeedback() {
        return mPreferences.getBoolean(KEY_SHOW_FEEDBACK, DEFAULT_SHOW_FEEDBACK);
    }

    public boolean setShowFeedback(Boolean value) {
        return mPreferences.edit().putBoolean(KEY_SHOW_FEEDBACK, value).commit();
    }

    public boolean showProfile() {
        return mPreferences.getBoolean(KEY_SHOW_PROFILE, DEFAULT_SHOW_PROFILE);
    }

    public boolean setShowProfile(Boolean value) {
        return mPreferences.edit().putBoolean(KEY_SHOW_PROFILE, value).commit();
    }

    public boolean showUploadQueue() {
        return mPreferences.getBoolean(KEY_SHOW_UPLOAD_QUEUE, DEFAULT_SHOW_UPLOAD_QUEUE);
    }

    public boolean setShowUploadQueue(Boolean value) {
        return mPreferences.edit().putBoolean(KEY_SHOW_UPLOAD_QUEUE, value).commit();
    }

    public boolean showMobility() {
        return mPreferences.getBoolean(KEY_SHOW_MOBILITY, DEFAULT_SHOW_MOBILITY);
    }

    public boolean setShowMobility(Boolean value) {
        return mPreferences.edit().putBoolean(KEY_SHOW_MOBILITY, value).commit();
    }

    public boolean showMobilityFeedback() {
        return mPreferences.getBoolean(KEY_SHOW_MOBILITY_FEEDBACK, DEFAULT_SHOW_MOBILITY_FEEDBACK);
    }

    /**
     * Returns the baseline, or a time 10 weeks after the start time if it is
     * set, or a time 1 month ago.
     * 
     * @param context
     * @return
     */
    public static long getBaseLineEndTime(Context context) {
        long base = PreferenceManager.getDefaultSharedPreferences(context).getLong(
                KEY_BASELINE_END_TIME, -1);
        if (base == -1) {
            // If baseline is not set, we set it to 10 weeks after the baseline
            // start time
            long startTime = getBaseLineStartTime(context);
            Calendar cal = Calendar.getInstance();
            if (startTime != 0) {
                // If start time is set, end time is 10 weeks after it
                cal.setTimeInMillis(startTime);
                cal.add(Calendar.DATE, 70);
            } else {
                // If no start time is set, end time is 1 month ago
                Utilities.clearTime(cal);
                cal.add(Calendar.MONTH, -1);
            }
            base = cal.getTimeInMillis();
        }
        return base;
    }

    /**
     * Returns the baseline, or 1 week after the first response, or 0 if not set
     * 
     * @param context
     * @return
     */
    public static long getBaseLineStartTime(Context context) {
        long startTime = PreferenceManager.getDefaultSharedPreferences(context).getLong(
                KEY_BASELINE_START_TIME, -1);
        // If the base line start time isn't set, we set it to 1 week after the
        // first response
        if (startTime == -1) {
            Cursor c = context.getContentResolver().query(Responses.CONTENT_URI, new String[] {
                Responses.RESPONSE_TIME
            }, null, null, Responses.RESPONSE_TIME + " ASC");
            if (c.moveToFirst()) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(c.getLong(0));
                Utilities.clearTime(cal);
                cal.add(Calendar.DATE, 7);
                startTime = cal.getTimeInMillis();
            } else {
                startTime = 0;
            }
            c.close();
        }
        return startTime;
    }

    public static void clearBaseLineTime(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(KEY_BASELINE_END_TIME)
                .remove(KEY_BASELINE_START_TIME).commit();
    }

    public String getUsername() {
        return mPreferences.getString(KEY_USERNAME, "");
    }

    public boolean putUsername(String username) {
        return mPreferences.edit().putString(KEY_USERNAME, username).commit();
    }

    public String getHashedPassword() {
        return mPreferences.getString(KEY_PASSWORD_HASHED, "");
    }

    public boolean putHashedPassword(String hashedPassword) {
        return mPreferences.edit().putString(KEY_PASSWORD_HASHED, hashedPassword).commit();
    }

    public boolean clearCredentials() {
        return mPreferences.edit().remove(KEY_USERNAME).remove(KEY_PASSWORD_HASHED).commit();
    }

    public Long getLastProbeUploadTimestamp() {
        return mPreferences.getLong(KEY_LAST_PROBE_UPLOAD_TIMESTAMP, 0);
    }

    public boolean putLastProbeUploadTimestamp(Long timestamp) {
        return mPreferences.edit().putLong(KEY_LAST_PROBE_UPLOAD_TIMESTAMP, timestamp).commit();
    }

    public Long getLoginTimestamp() {
        return mPreferences.getLong(KEY_LOGIN_TIMESTAMP, 0);
    }

    public boolean putLoginTimestamp(Long timestamp) {
        return mPreferences.edit().putLong(KEY_LOGIN_TIMESTAMP, timestamp).commit();
    }

    public Long getLastSurveyTimestamp(String surveyId) {
        return mPreferences.getLong(KEY_LAST_SURVEY_TIMESTAMP + surveyId, 0);
    }

    public boolean putLastSurveyTimestamp(String surveyId, Long timestamp) {
        return mPreferences.edit().putLong(KEY_LAST_SURVEY_TIMESTAMP + surveyId, timestamp)
                .commit();
    }

    public boolean isAuthenticated() {
        if (getUsername().length() > 0 && getHashedPassword().length() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isUserDisabled() {
        return mPreferences.getBoolean(KEY_IS_DISABLED, false);
    }

    public boolean setUserDisabled(boolean isDisabled) {
        return mPreferences.edit().putBoolean(KEY_IS_DISABLED, isDisabled).commit();
    }

    public long getLastCampaignRefreshTime() {
        return mPreferences.getLong(KEY_CAMPAIGN_REFRESH_TIME, 0);
    }

    public boolean setLastCampaignRefreshTime(long time) {
        return mPreferences.edit().putLong(KEY_CAMPAIGN_REFRESH_TIME, time).commit();
    }

    public boolean getUploadProbesWifiOnly() {
        return mPreferences.getBoolean(KEY_UPLOAD_PROBES_WIFI_ONLY, true);
    }

    public boolean setUploadProbesWifiOnly(boolean wifiOnly) {
        return mPreferences.edit().putBoolean(KEY_UPLOAD_PROBES_WIFI_ONLY, wifiOnly).commit();
    }

    public boolean getUploadResponsesWifiOnly() {
        return mPreferences.getBoolean(KEY_UPLOAD_RESPONSES_WIFI_ONLY, false);
    }

    public boolean setUploadResponsesWifiOnly(boolean wifiOnly) {
        return mPreferences.edit().putBoolean(KEY_UPLOAD_RESPONSES_WIFI_ONLY, wifiOnly).commit();
    }

    public boolean isSingleCampaignMode() {
        return mPreferences.getBoolean(KEY_SINGLE_CAMPAIGN_MODE, OhmageApplication.getContext()
                .getResources().getBoolean(R.bool.single_campaign_mode));
    }
}
