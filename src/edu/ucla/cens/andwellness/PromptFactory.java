package edu.ucla.cens.andwellness;

public class PromptFactory {

	private static final String SINGLE_CHOICE = "single_choice";
	private static final String MULTI_CHOICE = "multi_choice";
	
	private PromptFactory() {};
	
	public static Prompt createPrompt (String promptType) {
		
		if (promptType.equals(SINGLE_CHOICE)) {
			return new SingleChoicePrompt();//id, displayType, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
		} else if (promptType.equals(MULTI_CHOICE)) {
			return null;
		}
		
		throw new IllegalArgumentException("Unsupported prompt type: " + promptType);
	}
}
