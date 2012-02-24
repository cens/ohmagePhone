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

package edu.ucla.cens.systemlog;

import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.triggers.base.TriggerDB;

import android.content.Context;
import android.database.Cursor;

/**
 * Ohmage Specific Analytics logging
 * <p>
 * Logs prompt messages in the form:<br>
 * analytics_prompt: loginName context prompt_type prompt_id [ON/OFF]<br>
 * Ex. analytics_prompt: ohmage.cameron SurveyActivity SingleChoicePrompt
 * SnackPeriod OFF
 * </p>
 * <p>
 * Logs trigger service events such as updating, triggering, and ignoring in the
 * form:<br>
 * analytics_trigger: loginName context trigger_action trigger_type
 * campaign_urn<br>
 * Ex. analytics_trigger: ohmage.cameron TimeTrigEditActivity UPDATE
 * TimeTrigger urn:campaign:ca:ucla:cens:cameron_lots_of_surveys
 * </p>
 * <p>
 * Logs trigger service events such as adding and removing in the form:<br>
 * analytics_trigger: loginName context trigger_action trigger_type
 * trigger_count campaign_urn<br>
 * Ex. analytics_trigger: ohmage.cameron TriggerListActivity DELETE
 * LocationTrigger 0 urn:campaign:ca:ucla:cens:cameron_lots_of_surveys
 * </p>
 * 
 * @author cketcham
 */
public class OhmageAnalytics extends Analytics {

	/**
	 * Log information about when a prompt is being shown
	 * 
	 * @param context
	 * @param type
	 * @param id
	 * @param status
	 */
	public static void prompt(Context context, String type, String id, Status status) {
		log(context, "prompt", new StringBuilder().append(type).append(" ")
				.append(id).append(" ")
				.append(status));
	}

	/**
	 * Log information about when a prompt is being shown
	 * 
	 * @param context
	 * @param abstractPrompt
	 * @param status
	 */
	public static void prompt(Context context, AbstractPrompt abstractPrompt, Status status) {
		if (abstractPrompt != null)
			prompt(context, abstractPrompt.getClass().getSimpleName(), abstractPrompt.getId(),
					status);
	}

	/**
	 * Logs trigger service events
	 * 
	 * @param context
	 * @param action
	 * @param trigId
	 */
	public static void trigger(Context context, TriggerStatus action, int trigId) {
		StringBuilder builder = new StringBuilder().append(action).append(" ");

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

		builder.append(trigType).append(" ");

		// If it is a remove or add action, we append the trigger count
		if (TriggerStatus.DELETE == action)
			builder.append(count - 1).append(" ");
		else if (TriggerStatus.ADD == action)
			builder.append(count).append(" ");

		log(context, "trigger", builder.append(campaign));
	}

	public static enum TriggerStatus {
		DELETE,
		ADD,
		TRIGGER,
		UPDATE,
		IGNORE
	}
}
