/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.ucla.cens.andwellness.prompt;

import edu.ucla.cens.andwellness.prompt.hoursbeforenow.HoursBeforeNowPrompt;
import edu.ucla.cens.andwellness.prompt.multichoice.MultiChoicePrompt;
import edu.ucla.cens.andwellness.prompt.multichoicecustom.MultiChoiceCustomPrompt;
import edu.ucla.cens.andwellness.prompt.number.NumberPrompt;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.prompt.remoteactivity.RemoteActivityPrompt;
import edu.ucla.cens.andwellness.prompt.singlechoice.SingleChoicePrompt;
import edu.ucla.cens.andwellness.prompt.singlechoicecustom.SingleChoiceCustomPrompt;
import edu.ucla.cens.andwellness.prompt.text.TextPrompt;

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
