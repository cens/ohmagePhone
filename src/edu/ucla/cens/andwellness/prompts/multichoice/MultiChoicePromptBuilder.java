package edu.ucla.cens.andwellness.prompts.multichoice;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.prompts.Prompt;
import edu.ucla.cens.andwellness.prompts.PromptBuilder;

public class MultiChoicePromptBuilder implements PromptBuilder {

	@Override
	public void build(	Prompt prompt, String id, String displayType,
						String displayLabel, String promptText, String abbreviatedText,
						String explanationText, String defaultValue, String condition,
						String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {
		
		// TODO deal with null arguments
		
		MultiChoicePrompt multiChoicePrompt = (MultiChoicePrompt) prompt;
		multiChoicePrompt.setId(id);
		multiChoicePrompt.setDisplayType(displayType);
		multiChoicePrompt.setDisplayLabel(displayLabel);
		multiChoicePrompt.setPromptText(promptText);
		multiChoicePrompt.setAbbreviatedText(abbreviatedText);
		multiChoicePrompt.setExplanationText(explanationText);
		multiChoicePrompt.setDefaultValue(defaultValue);
		multiChoicePrompt.setCondition(condition);
		multiChoicePrompt.setSkippable(skippable);
		multiChoicePrompt.setSkipLabel(skipLabel);
		multiChoicePrompt.setChoices(properties);
	}

}
