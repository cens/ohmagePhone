package edu.ucla.cens.andwellness;

public class PromptBuilderFactory {

	private static final String SINGLE_CHOICE = "single_choice";
	private static final String MULTI_CHOICE = "multi_choice";
	
	private PromptBuilderFactory() {};
	
	public static PromptBuilder createPromptBuilder(String promptType ) {
		
		if (promptType.equals(SINGLE_CHOICE)) {
			return new SingleChoicePromptBuilder();//id, displayType, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
		} else if (promptType.equals(MULTI_CHOICE)) {
			return null;
		}
		
		return null;
	}
}
