package edu.ucla.cens.andwellness.prompts;

import edu.ucla.cens.andwellness.prompts.hoursbeforenow.HoursBeforeNowPrompt;
import edu.ucla.cens.andwellness.prompts.multichoice.MultiChoicePrompt;
import edu.ucla.cens.andwellness.prompts.multichoicecustom.MultiChoiceCustomPrompt;
import edu.ucla.cens.andwellness.prompts.number.NumberPrompt;
import edu.ucla.cens.andwellness.prompts.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.prompts.remoteactivity.RemoteActivityPrompt;
import edu.ucla.cens.andwellness.prompts.singlechoice.SingleChoicePrompt;
import edu.ucla.cens.andwellness.prompts.singlechoicecustom.SingleChoiceCustomPrompt;
import edu.ucla.cens.andwellness.prompts.text.TextPrompt;

public class PromptFactory {

	private static final String SINGLE_CHOICE = "single_choice";
	private static final String SINGLE_CHOICE_CUSTOM = "single_choice_custom";
	private static final String MULTI_CHOICE = "multi_choice";
	private static final String MULTI_CHOICE_CUSTOM = "multi_choice_custom";
	private static final String NUMBER = "number";
	private static final String HOURS_BEFORE_NOW = "hours_before_now";
	private static final String TEXT = "text";
	private static final String PHOTO = "photo";
	private static final String REMOTE_ACTIVITY = "remote_activity";
	
	private PromptFactory() {};
	
	public static Prompt createPrompt (String promptType) {
		
		if (promptType.equals(SINGLE_CHOICE)) {
			return new SingleChoicePrompt();//id, displayType, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
		} else if (promptType.equals(SINGLE_CHOICE_CUSTOM)) {
			return new SingleChoiceCustomPrompt();
		} else if (promptType.equals(MULTI_CHOICE)) {
			return new MultiChoicePrompt();
		} else if (promptType.equals(MULTI_CHOICE_CUSTOM)) {
			return new MultiChoiceCustomPrompt();
		} else if (promptType.equals(NUMBER)) {
			return new NumberPrompt();
		} else if (promptType.equals(HOURS_BEFORE_NOW)) {
			return new HoursBeforeNowPrompt();
		} else if (promptType.equals(TEXT)) {
			return new TextPrompt();
		} else if (promptType.equals(PHOTO)) {
			return new PhotoPrompt();
		} else if (promptType.equals(REMOTE_ACTIVITY)) {
			return new RemoteActivityPrompt();
		}
		
		throw new IllegalArgumentException("Unsupported prompt type: " + promptType);
	}
}
