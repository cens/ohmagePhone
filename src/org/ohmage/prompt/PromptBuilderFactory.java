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
package org.ohmage.prompt;

import org.ohmage.prompt.hoursbeforenow.HoursBeforeNowPromptBuilder;
import org.ohmage.prompt.media.PhotoPromptBuilder;
import org.ohmage.prompt.media.VideoPromptBuilder;
import org.ohmage.prompt.multichoice.MultiChoicePromptBuilder;
import org.ohmage.prompt.multichoicecustom.MultiChoiceCustomPromptBuilder;
import org.ohmage.prompt.number.NumberPromptBuilder;
import org.ohmage.prompt.remoteactivity.RemoteActivityPromptBuilder;
import org.ohmage.prompt.singlechoice.SingleChoicePromptBuilder;
import org.ohmage.prompt.singlechoicecustom.SingleChoiceCustomPromptBuilder;
import org.ohmage.prompt.text.TextPromptBuilder;
import org.ohmage.prompt.timestamp.TimestampPromptBuilder;

public class PromptBuilderFactory {
	
	private PromptBuilderFactory() {};
	
	public static PromptBuilder createPromptBuilder(String promptType ) {
		
		if (promptType.equals(PromptFactory.SINGLE_CHOICE)) {
			return new SingleChoicePromptBuilder();//id, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
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
		} else if (promptType.equals(PromptFactory.VIDEO)) {
			return new VideoPromptBuilder();
		} else if (promptType.equals(PromptFactory.REMOTE_ACTIVITY)) {
			return new RemoteActivityPromptBuilder();
		}
		
		throw new IllegalArgumentException("Unsupported prompt type: " + promptType);
	}
}
