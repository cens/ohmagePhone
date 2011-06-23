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
package edu.ucla.cens.andwellness.prompt.singlechoice;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.prompt.Prompt;
import edu.ucla.cens.andwellness.prompt.PromptBuilder;

public class SingleChoicePromptBuilder implements PromptBuilder {

	@Override
	public void build(	Prompt prompt, String id, String displayType,
						String displayLabel, String promptText, String abbreviatedText,
						String explanationText, String defaultValue, String condition,
						String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {
		
		// TODO deal with null arguments
		
		SingleChoicePrompt singleChoicePrompt = (SingleChoicePrompt) prompt;
		singleChoicePrompt.setId(id);
		singleChoicePrompt.setDisplayType(displayType);
		singleChoicePrompt.setDisplayLabel(displayLabel);
		singleChoicePrompt.setPromptText(promptText);
		singleChoicePrompt.setAbbreviatedText(abbreviatedText);
		singleChoicePrompt.setExplanationText(explanationText);
		singleChoicePrompt.setDefaultValue(defaultValue);
		singleChoicePrompt.setCondition(condition);
		singleChoicePrompt.setSkippable(skippable);
		singleChoicePrompt.setSkipLabel(skipLabel);
		singleChoicePrompt.setChoices(properties);
	}

}
