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
package edu.ucla.cens.andwellness.prompt.remoteactivity;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.prompt.Prompt;
import edu.ucla.cens.andwellness.prompt.PromptBuilder;

public class RemoteActivityPromptBuilder implements PromptBuilder
{
	public static final String PACKAGE_ID = "package";
	public static final String ACTIVITY_ID = "activity";
	public static final String ACTION_ID = "action";
	public static final String AUTOLAUNCH_ID = "autolaunch";
	public static final String NUM_RETRIES_ID = "retries";
	public static final String MIN_RUNS_ID = "min_runs";
	public static final String INPUT_ID = "input";
	
	@Override
	public void build(Prompt prompt, String id, String displayType,
			String displayLabel, String promptText, String abbreviatedText,
			String explanationText, String defaultValue, String condition,
			String skippable, String skipLabel, ArrayList<KVLTriplet> properties)
	{
		RemoteActivityPrompt remoteActivityPrompt = (RemoteActivityPrompt) prompt;
		remoteActivityPrompt.setId(id);
		remoteActivityPrompt.setDisplayType(displayType);
		remoteActivityPrompt.setDisplayLabel(displayLabel);
		remoteActivityPrompt.setPromptText(promptText);
		remoteActivityPrompt.setAbbreviatedText(abbreviatedText);
		remoteActivityPrompt.setExplanationText(explanationText);
		remoteActivityPrompt.setDefaultValue(defaultValue);
		remoteActivityPrompt.setCondition(condition);
		remoteActivityPrompt.setSkippable(skippable);
		remoteActivityPrompt.setSkipLabel(skipLabel);
		
		for(KVLTriplet property : properties)
		{
			if(property.key.equals(PACKAGE_ID))
			{
				remoteActivityPrompt.setPackage(property.label);
			}
			else if(property.key.equals(ACTIVITY_ID))
			{
				remoteActivityPrompt.setActivity(property.label);
			}
			else if(property.key.equals(ACTION_ID))
			{
				remoteActivityPrompt.setAction(property.label);
			}
			else if(property.key.equals(AUTOLAUNCH_ID))
			{
				remoteActivityPrompt.setAutolaunch(property.label.equalsIgnoreCase("true"));
			}
			else if(property.key.equals(NUM_RETRIES_ID))
			{
				remoteActivityPrompt.setRetries(Integer.parseInt(property.label));
			}
			else if(MIN_RUNS_ID.equals(property.key)) {
				remoteActivityPrompt.setMinRuns(Integer.parseInt(property.label));
			}
			else if(INPUT_ID.equals(property.key)) {
				remoteActivityPrompt.setInput(property.label);
			}
		}

		remoteActivityPrompt.clearTypeSpecificResponseData();
	}
}
