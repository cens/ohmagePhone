package edu.ucla.cens.andwellness.prompt.timestamp;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.prompt.Prompt;
import edu.ucla.cens.andwellness.prompt.PromptBuilder;
import edu.ucla.cens.andwellness.prompt.number.NumberPrompt;

public class TimestampPromptBuilder implements PromptBuilder {

	@Override
	public void build(Prompt prompt, String id, String displayType,
			String displayLabel, String promptText, String abbreviatedText,
			String explanationText, String defaultValue, String condition,
			String skippable, String skipLabel, ArrayList<KVLTriplet> properties) {
		
		TimestampPrompt timestampPrompt = (TimestampPrompt) prompt;
		timestampPrompt.setId(id);
		timestampPrompt.setDisplayType(displayType);
		timestampPrompt.setDisplayLabel(displayLabel);
		timestampPrompt.setPromptText(promptText);
		timestampPrompt.setAbbreviatedText(abbreviatedText);
		timestampPrompt.setExplanationText(explanationText);
		timestampPrompt.setDefaultValue(defaultValue);
		timestampPrompt.setCondition(condition);
		timestampPrompt.setSkippable(skippable);
		timestampPrompt.setSkipLabel(skipLabel);
		timestampPrompt.setProperties(properties);
		
		
	}

}
