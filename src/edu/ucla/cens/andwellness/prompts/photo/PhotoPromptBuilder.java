package edu.ucla.cens.andwellness.prompts.photo;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities;
import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.Utilities.KVPair;
import edu.ucla.cens.andwellness.prompts.Prompt;
import edu.ucla.cens.andwellness.prompts.PromptBuilder;

public class PhotoPromptBuilder implements PromptBuilder {

	@Override
	public void build(Prompt prompt, String id, String displayType,
			String displayLabel, String promptText, String abbreviatedText,
			String explanationText, String defaultValue, String condition,
			String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {
		
		PhotoPrompt photoPrompt = (PhotoPrompt) prompt;
		photoPrompt.setId(id);
		photoPrompt.setDisplayType(displayType);
		photoPrompt.setDisplayLabel(displayLabel);
		photoPrompt.setPromptText(promptText);
		photoPrompt.setAbbreviatedText(abbreviatedText);
		photoPrompt.setExplanationText(explanationText);
		photoPrompt.setDefaultValue(defaultValue);
		photoPrompt.setCondition(condition);
		photoPrompt.setSkippable(skippable);
		photoPrompt.setSkipLabel(skipLabel);
		
		for (KVLTriplet property : properties) {
			if (property.key.equals("res")) {
				photoPrompt.setResolution(property.label);
			} 
		}
		
		photoPrompt.clearTypeSpecificResponseData();

	}

}
