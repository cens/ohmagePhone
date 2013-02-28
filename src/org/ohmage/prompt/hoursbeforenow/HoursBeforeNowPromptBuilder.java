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
package org.ohmage.prompt.hoursbeforenow;

import org.ohmage.Utilities.KVLTriplet;
import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.PromptBuilder;

import java.util.ArrayList;


public class HoursBeforeNowPromptBuilder implements PromptBuilder {

	@Override
	public void build(Prompt prompt, String id,
			String displayLabel, String promptText, String abbreviatedText,
			String explanationText, String defaultValue, String condition,
			String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {
		
		HoursBeforeNowPrompt hoursBeforeNowPrompt = (HoursBeforeNowPrompt) prompt;
		hoursBeforeNowPrompt.setId(id);
		hoursBeforeNowPrompt.setDisplayLabel(displayLabel);
		hoursBeforeNowPrompt.setPromptText(promptText);
		hoursBeforeNowPrompt.setAbbreviatedText(abbreviatedText);
		hoursBeforeNowPrompt.setExplanationText(explanationText);
		hoursBeforeNowPrompt.setDefaultValue(defaultValue);
		hoursBeforeNowPrompt.setCondition(condition);
		hoursBeforeNowPrompt.setSkippable(skippable);
		hoursBeforeNowPrompt.setSkipLabel(skipLabel);
		hoursBeforeNowPrompt.setProperties(properties);
		
		for (KVLTriplet property : properties) {
			if (property.key.equals("min")) {
				hoursBeforeNowPrompt.setMinimum(Integer.parseInt(property.label));
			} else if (property.key.equals("max")) {
				hoursBeforeNowPrompt.setMaximum(Integer.parseInt(property.label));
			}
		}
		
		hoursBeforeNowPrompt.clearTypeSpecificResponseData();

	}

}
