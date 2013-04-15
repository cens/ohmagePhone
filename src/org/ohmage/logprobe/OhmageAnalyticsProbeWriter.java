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

import android.os.RemoteException;

import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApplication;
import org.ohmage.logprobe.LogProbe.Status;
import org.ohmage.logprobe.OhmageAnalytics.TriggerStatus;
import org.ohmage.probemanager.ProbeBuilder;

/**
 * Ohmage Specific probe writer which has additional Analytics logging. Uses the
 * probewriter from {@link LogProbe#probeWriter} which will be created in
 * {@link OhmageApplication#onCreate()}
 * 
 * @author cketcham
 */
public class OhmageAnalyticsProbeWriter {

    private static final String OBSERVER_ID = "org.ohmage.Analytics";
    private static final int OBSERVER_VERSION = 2;

    private static final String STREAM_PROMPT = "prompt";
    private static final int STREAM_PROMPT_VERSION = 2;

    private static final String STREAM_TRIGGER = "trigger";
    private static final int STREAM_TRIGGER_VERSION = 2;

    public static void prompt(String type, String id, Status status) {
        try {
            ProbeBuilder probe = new ProbeBuilder(OBSERVER_ID, OBSERVER_VERSION);
            probe.setStream(STREAM_PROMPT, STREAM_PROMPT_VERSION);

            JSONObject data = new JSONObject();
            data.put("type", type);
            data.put("id", id);
            data.put("status", status);
            LogProbe.probeWriter.addDeviceId(data);
            probe.setData(data.toString());
            probe.withId().now();

            probe.write(LogProbe.probeWriter);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void trigger(TriggerStatus action, String type, int count, String campaign) {
        try {
            ProbeBuilder probe = new ProbeBuilder(OBSERVER_ID, OBSERVER_VERSION);
            probe.setStream(STREAM_TRIGGER, STREAM_TRIGGER_VERSION);

            JSONObject data = new JSONObject();
            data.put("action", action);
            data.put("type", type);
            data.put("count", count);
            data.put("campaign", campaign);
            LogProbe.probeWriter.addDeviceId(data);
            probe.setData(data.toString());
            probe.withId().now();

            probe.write(LogProbe.probeWriter);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
