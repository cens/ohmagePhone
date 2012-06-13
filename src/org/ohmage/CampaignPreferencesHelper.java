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

/**
 * Creates preferences specific to certain campaigns
 * 
 * @author cketcham
 */
public class CampaignPreferencesHelper {

    public static final String KEY_SHOW_INSTRUCTIONS = "key_show_instructions";

    private final SharedPreferences mPreferences;

    public CampaignPreferencesHelper(Context context, String campaignUrn) {
        mPreferences = getPreferences(context, campaignUrn);
    }

    public SharedPreferences getPreferences(Context context, String campaignUrn) {
        return context.getSharedPreferences(context.getPackageName() + "_" + campaignUrn
                + "_preferences", Context.MODE_PRIVATE);
    }

    public boolean clearAll() {
        return mPreferences.edit().clear().commit();
    }

    public boolean showInstructions() {
        return mPreferences.getBoolean(KEY_SHOW_INSTRUCTIONS, true);
    }

    public boolean setShowInstructions(boolean show) {
        return mPreferences.edit().putBoolean(KEY_SHOW_INSTRUCTIONS, show).commit();
    }
}
