package edu.ucla.cens.andwellness.prompts;

import edu.ucla.cens.andwellness.prompts.hoursbeforenow.HoursBeforeNowPromptBuilder;
import edu.ucla.cens.andwellness.prompts.multichoice.MultiChoicePromptBuilder;
import edu.ucla.cens.andwellness.prompts.number.NumberPromptBuilder;
import edu.ucla.cens.andwellness.prompts.photo.PhotoPromptBuilder;
import edu.ucla.cens.andwellness.prompts.singlechoice.SingleChoicePromptBuilder;
import edu.ucla.cens.andwellness.prompts.text.TextPromptBuilder;

public class PromptBuilderFactory {

	private static final String SINGLE_CHOICE = "single_choice";
	private static final String SINGLE_CHOICE_CUSTOM = "single_choice_custom";
	private static final String MULTI_CHOICE = "multi_choice";
	private static final String MULTI_CHOICE_CUSTOM = "multi_choice_custom";
	private static final String NUMBER = "number";
	private static final String HOURS_BEFORE_NOW = "hours_before_now";
	private static final String TEXT = "text";
	private static final String PHOTO = "photo";
	
	private PromptBuilderFactory() {};
	
	public static PromptBuilder createPromptBuilder(String promptType ) {
		
		if (promptType.equals(SINGLE_CHOICE)) {
			return new SingleChoicePromptBuilder();//id, displayType, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
		} else if (promptType.equals(MULTI_CHOICE)) {
			return new MultiChoicePromptBuilder();
		} else if (promptType.equals(NUMBER)) {
			return new NumberPromptBuilder();
		} else if (promptType.equals(HOURS_BEFORE_NOW)) {
			return new HoursBeforeNowPromptBuilder();
		} else if (promptType.equals(TEXT)) {
			return new TextPromptBuilder();
		} else if (promptType.equals(PHOTO)) {
			return new PhotoPromptBuilder();
		}
		
		return null;
	}
}
