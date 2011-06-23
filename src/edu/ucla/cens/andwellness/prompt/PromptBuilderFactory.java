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

import edu.ucla.cens.andwellness.prompt.hoursbeforenow.HoursBeforeNowPromptBuilder;
import edu.ucla.cens.andwellness.prompt.multichoice.MultiChoicePromptBuilder;
import edu.ucla.cens.andwellness.prompt.multichoicecustom.MultiChoiceCustomPromptBuilder;
import edu.ucla.cens.andwellness.prompt.number.NumberPromptBuilder;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPromptBuilder;
import edu.ucla.cens.andwellness.prompt.remoteactivity.RemoteActivityPromptBuilder;
import edu.ucla.cens.andwellness.prompt.singlechoice.SingleChoicePromptBuilder;
import edu.ucla.cens.andwellness.prompt.singlechoicecustom.SingleChoiceCustomPromptBuilder;
import edu.ucla.cens.andwellness.prompt.text.TextPromptBuilder;

public class PromptBuilderFactory {

	private static final String SINGLE_CHOICE = "single_choice";
	private static final String SINGLE_CHOICE_CUSTOM = "single_choice_custom";
	private static final String MULTI_CHOICE = "multi_choice";
	private static final String MULTI_CHOICE_CUSTOM = "multi_choice_custom";
	private static final String NUMBER = "number";
	private static final String HOURS_BEFORE_NOW = "hours_before_now";
	private static final String TEXT = "text";
	private static final String PHOTO = "photo";
	private static final String REMOTE_ACTIVITY = "remote_activity";
	
	private PromptBuilderFactory() {};
	
	public static PromptBuilder createPromptBuilder(String promptType ) {
		
		if (promptType.equals(SINGLE_CHOICE)) {
			return new SingleChoicePromptBuilder();//id, displayType, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
		} else if (promptType.equals(SINGLE_CHOICE_CUSTOM)) {
			return new SingleChoiceCustomPromptBuilder();
		} else if (promptType.equals(MULTI_CHOICE)) {
			return new MultiChoicePromptBuilder();
		} else if (promptType.equals(MULTI_CHOICE_CUSTOM)) {
			return new MultiChoiceCustomPromptBuilder();
		} else if (promptType.equals(NUMBER)) {
			return new NumberPromptBuilder();
		} else if (promptType.equals(HOURS_BEFORE_NOW)) {
			return new HoursBeforeNowPromptBuilder();
		} else if (promptType.equals(TEXT)) {
			return new TextPromptBuilder();
		} else if (promptType.equals(PHOTO)) {
			return new PhotoPromptBuilder();
		} else if (promptType.equals(REMOTE_ACTIVITY)) {
			return new RemoteActivityPromptBuilder();
		}
		
		return null;
	}
}
