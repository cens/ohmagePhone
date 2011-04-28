package edu.ucla.cens.andwellness.prompts;

import java.util.ArrayList;

import edu.ucla.cens.andwellness.Utilities.KVLTriplet;

public interface PromptBuilder {

	void build(	Prompt prompt, String id, String displayType, String displayLabel, 
				String promptText, String abbreviatedText, String explanationText,
				String defaultValue, String condition, 
				String skippable, String skipLabel,
				ArrayList<KVLTriplet> properties);
}
