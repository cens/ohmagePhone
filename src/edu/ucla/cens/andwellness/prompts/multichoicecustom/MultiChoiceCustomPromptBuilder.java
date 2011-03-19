package edu.ucla.cens.andwellness.prompts.multichoicecustom;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities;
import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.Utilities.KVPair;
import edu.ucla.cens.andwellness.prompts.Prompt;
import edu.ucla.cens.andwellness.prompts.PromptBuilder;

public class MultiChoiceCustomPromptBuilder implements PromptBuilder {

	@Override
	public void build(	Prompt prompt, String id, String displayType,
						String displayLabel, String promptText, String abbreviatedText,
						String explanationText, String defaultValue, String condition,
						String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {
		
		// TODO deal with null arguments
		
		MultiChoiceCustomPrompt multiChoiceCustomPrompt = (MultiChoiceCustomPrompt) prompt;
		multiChoiceCustomPrompt.setId(id);
		multiChoiceCustomPrompt.setDisplayType(displayType);
		multiChoiceCustomPrompt.setDisplayLabel(displayLabel);
		multiChoiceCustomPrompt.setPromptText(promptText);
		multiChoiceCustomPrompt.setAbbreviatedText(abbreviatedText);
		multiChoiceCustomPrompt.setExplanationText(explanationText);
		multiChoiceCustomPrompt.setDefaultValue(defaultValue);
		multiChoiceCustomPrompt.setCondition(condition);
		multiChoiceCustomPrompt.setSkippable(skippable);
		multiChoiceCustomPrompt.setSkipLabel(skipLabel);
		
		//add entries from db to properties
		
		multiChoiceCustomPrompt.setChoices(properties);
	}

}
