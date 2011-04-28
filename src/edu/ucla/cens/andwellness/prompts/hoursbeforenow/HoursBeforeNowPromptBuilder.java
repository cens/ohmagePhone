package edu.ucla.cens.andwellness.prompts.hoursbeforenow;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.prompts.Prompt;
import edu.ucla.cens.andwellness.prompts.PromptBuilder;

public class HoursBeforeNowPromptBuilder implements PromptBuilder {

	@Override
	public void build(Prompt prompt, String id, String displayType,
			String displayLabel, String promptText, String abbreviatedText,
			String explanationText, String defaultValue, String condition,
			String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {
		
		HoursBeforeNowPrompt hoursBeforeNowPrompt = (HoursBeforeNowPrompt) prompt;
		hoursBeforeNowPrompt.setId(id);
		hoursBeforeNowPrompt.setDisplayType(displayType);
		hoursBeforeNowPrompt.setDisplayLabel(displayLabel);
		hoursBeforeNowPrompt.setPromptText(promptText);
		hoursBeforeNowPrompt.setAbbreviatedText(abbreviatedText);
		hoursBeforeNowPrompt.setExplanationText(explanationText);
		hoursBeforeNowPrompt.setDefaultValue(defaultValue);
		hoursBeforeNowPrompt.setCondition(condition);
		hoursBeforeNowPrompt.setSkippable(skippable);
		hoursBeforeNowPrompt.setSkipLabel(skipLabel);
		
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
