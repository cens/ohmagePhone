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
import edu.ucla.cens.andwellness.prompt.timestamp.TimestampPromptBuilder;

public class PromptBuilderFactory {
	
	private PromptBuilderFactory() {};
	
	public static PromptBuilder createPromptBuilder(String promptType ) {
		
		if (promptType.equals(PromptFactory.SINGLE_CHOICE)) {
			return new SingleChoicePromptBuilder();//id, displayType, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
		} else if (promptType.equals(PromptFactory.SINGLE_CHOICE_CUSTOM)) {
			return new SingleChoiceCustomPromptBuilder();
		} else if (promptType.equals(PromptFactory.MULTI_CHOICE)) {
			return new MultiChoicePromptBuilder();
		} else if (promptType.equals(PromptFactory.MULTI_CHOICE_CUSTOM)) {
			return new MultiChoiceCustomPromptBuilder();
		} else if (promptType.equals(PromptFactory.NUMBER)) {
			return new NumberPromptBuilder();
		} else if (promptType.equals(PromptFactory.HOURS_BEFORE_NOW)) {
			return new HoursBeforeNowPromptBuilder();
		} else if (promptType.equals(PromptFactory.TIMESTAMP)) {
			return new TimestampPromptBuilder();
		} else if (promptType.equals(PromptFactory.TEXT)) {
			return new TextPromptBuilder();
		} else if (promptType.equals(PromptFactory.PHOTO)) {
			return new PhotoPromptBuilder();
		} else if (promptType.equals(PromptFactory.REMOTE_ACTIVITY)) {
			return new RemoteActivityPromptBuilder();
		}
		
		throw new IllegalArgumentException("Unsupported prompt type: " + promptType);
	}
}
