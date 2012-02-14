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

import android.content.Context;

/**
 * Ohmage Specific Analytics logging
 * <p>
 * Logs prompt messages in the form:<br>
 * analytics_prompt: loginName context prompt_type prompt_id [ON/OFF]<br>
 * Ex. analytics_prompt: ohmage.cameron SurveyActivity SingleChoicePrompt
 * SnackPeriod OFF
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
}
