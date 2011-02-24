package edu.ucla.cens.andwellness.prompts;

import edu.ucla.cens.andwellness.prompts.hoursbeforenow.HoursBeforeNowPrompt;
import edu.ucla.cens.andwellness.prompts.multichoice.MultiChoicePrompt;
import edu.ucla.cens.andwellness.prompts.number.NumberPrompt;
import edu.ucla.cens.andwellness.prompts.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.prompts.singlechoice.SingleChoicePrompt;
import edu.ucla.cens.andwellness.prompts.text.TextPrompt;

public class PromptFactory {

	private static final String SINGLE_CHOICE = "single_choice";
	private static final String MULTI_CHOICE = "multi_choice";
	private static final String NUMBER = "number";
	private static final String HOURS_BEFORE_NOW = "hours_before_now";
	private static final String TEXT = "text";
	private static final String PHOTO = "photo";
	
	private PromptFactory() {};
	
	public static Prompt createPrompt (String promptType) {
		
		if (promptType.equals(SINGLE_CHOICE)) {
			return new SingleChoicePrompt();//id, displayType, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
		} else if (promptType.equals(MULTI_CHOICE)) {
			return new MultiChoicePrompt();
		} else if (promptType.equals(NUMBER)) {
			return new NumberPrompt();
		} else if (promptType.equals(HOURS_BEFORE_NOW)) {
			return new HoursBeforeNowPrompt();
		} else if (promptType.equals(TEXT)) {
			return new TextPrompt();
		} else if (promptType.equals(PHOTO)) {
			return new PhotoPrompt();
		}
		
		throw new IllegalArgumentException("Unsupported prompt type: " + promptType);
	}
}
