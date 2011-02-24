package edu.ucla.cens.andwellness.prompts.text;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities;
import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.Utilities.KVPair;
import edu.ucla.cens.andwellness.prompts.Prompt;
import edu.ucla.cens.andwellness.prompts.PromptBuilder;

public class TextPromptBuilder implements PromptBuilder {

	@Override
	public void build(Prompt prompt, String id, String displayType,
			String displayLabel, String promptText, String abbreviatedText,
			String explanationText, String defaultValue, String condition,
			String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {
		
		TextPrompt textPrompt = (TextPrompt) prompt;
		textPrompt.setId(id);
		textPrompt.setDisplayType(displayType);
		textPrompt.setDisplayLabel(displayLabel);
		textPrompt.setPromptText(promptText);
		textPrompt.setAbbreviatedText(abbreviatedText);
		textPrompt.setExplanationText(explanationText);
		textPrompt.setDefaultValue(defaultValue);
		textPrompt.setCondition(condition);
		textPrompt.setSkippable(skippable);
		textPrompt.setSkipLabel(skipLabel);
		
		textPrompt.clearTypeSpecificResponseData();
		
	}

}
