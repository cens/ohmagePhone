package edu.ucla.cens.andwellness;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities.KVPair;

public class SingleChoicePromptBuilder implements PromptBuilder {

	@Override
	public void build(	Prompt prompt, String id, String displayType,
						String displayLabel, String promptText, String abbreviatedText,
						String explanationText, String defaultValue, String condition,
						String skippable, String skipLabel, ArrayList<KVPair> properties) {
		
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
