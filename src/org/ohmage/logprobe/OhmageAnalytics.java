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

package org.ohmage.logprobe;

import android.content.Context;
import android.database.Cursor;

import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.logprobe.LogProbe.Status;
import org.ohmage.triggers.base.TriggerDB;

/**
 * Ohmage Specific Analytics logging
 * 
 * @author cketcham
 */
public class OhmageAnalytics {

    public static enum TriggerStatus {
        DELETE,
        ADD,
        TRIGGER,
        UPDATE,
        IGNORE
    }

    /**
     * Log information about when a prompt is being shown
     * 
     * @param type
     * @param id
     * @param status
     */
    public static void prompt(String type, String id, Status status) {
        if (LogProbe.logAnalytics)
            OhmageAnalyticsProbeWriter.prompt(type, id, status);
    }

    /**
     * Log information about when a prompt is being shown
     * 
     * @param abstractPrompt
     * @param status
     */
    public static void prompt(AbstractPrompt abstractPrompt, Status status) {
        if (abstractPrompt != null && LogProbe.logAnalytics)
            OhmageAnalyticsProbeWriter.prompt(abstractPrompt.getClass().getSimpleName(),
                    abstractPrompt.getId(), status);
    }

    /**
     * Logs trigger service events
     * 
     * @param context
     * @param action
     * @param trigId
     */
    public static void trigger(Context context, TriggerStatus action, int trigId) {
        if (!LogProbe.logAnalytics)
            return;

        TriggerDB db = new TriggerDB(context);
        db.open();
        String campaign = db.getCampaignUrn(trigId);
        String trigType = db.getTriggerType(trigId);
        int count = 0;
        if (campaign != null && trigType != null) {
            Cursor c = db.getTriggers(campaign, trigType);
            if (c.moveToFirst())
                count = c.getCount();
            c.close();
        }
        db.close();

        // If it is a remove action then there is one less in the count
        if (TriggerStatus.DELETE == action)
            count--;

        OhmageAnalyticsProbeWriter.trigger(action, trigType, count, campaign);
    }
}
