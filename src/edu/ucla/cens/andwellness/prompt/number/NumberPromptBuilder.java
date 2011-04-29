package edu.ucla.cens.andwellness.prompt.number;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.prompt.Prompt;
import edu.ucla.cens.andwellness.prompt.PromptBuilder;

public class NumberPromptBuilder implements PromptBuilder {

	@Override
	public void build(Prompt prompt, String id, String displayType,
			String displayLabel, String promptText, String abbreviatedText,
			String explanationText, String defaultValue, String condition,
			String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {
		
		NumberPrompt numberPrompt = (NumberPrompt) prompt;
		numberPrompt.setId(id);
		numberPrompt.setDisplayType(displayType);
		numberPrompt.setDisplayLabel(displayLabel);
		numberPrompt.setPromptText(promptText);
		numberPrompt.setAbbreviatedText(abbreviatedText);
		numberPrompt.setExplanationText(explanationText);
		numberPrompt.setDefaultValue(defaultValue);
		numberPrompt.setCondition(condition);
		numberPrompt.setSkippable(skippable);
		numberPrompt.setSkipLabel(skipLabel);
		
		for (KVLTriplet property : properties) {
			if (property.key.equals("min")) {
				numberPrompt.setMinimum(Integer.parseInt(property.label));
			} else if (property.key.equals("max")) {
				numberPrompt.setMaximum(Integer.parseInt(property.label));
			}
		}
		
		numberPrompt.clearTypeSpecificResponseData();
	}

}
