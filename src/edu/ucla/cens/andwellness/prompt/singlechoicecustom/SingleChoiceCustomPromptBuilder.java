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
package edu.ucla.cens.andwellness.prompt.singlechoicecustom;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.prompt.Prompt;
import edu.ucla.cens.andwellness.prompt.PromptBuilder;

public class SingleChoiceCustomPromptBuilder implements PromptBuilder {

	@Override
	public void build(	Prompt prompt, String id, String displayType,
						String displayLabel, String promptText, String abbreviatedText,
						String explanationText, String defaultValue, String condition,
						String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {
		
		// TODO deal with null arguments
		
		SingleChoiceCustomPrompt singleChoiceCustomPrompt = (SingleChoiceCustomPrompt) prompt;
		singleChoiceCustomPrompt.setId(id);
		singleChoiceCustomPrompt.setDisplayType(displayType);
		singleChoiceCustomPrompt.setDisplayLabel(displayLabel);
		singleChoiceCustomPrompt.setPromptText(promptText);
		singleChoiceCustomPrompt.setAbbreviatedText(abbreviatedText);
		singleChoiceCustomPrompt.setExplanationText(explanationText);
		singleChoiceCustomPrompt.setDefaultValue(defaultValue);
		singleChoiceCustomPrompt.setCondition(condition);
		singleChoiceCustomPrompt.setSkippable(skippable);
		singleChoiceCustomPrompt.setSkipLabel(skipLabel);
		
		//add entries from db to properties
		
		singleChoiceCustomPrompt.setChoices(properties);
	}

}
