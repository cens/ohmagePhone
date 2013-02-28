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
package org.ohmage;

import android.util.Xml;

import org.ohmage.Utilities.KVLTriplet;
import org.ohmage.logprobe.Log;
import org.ohmage.prompt.Message;
import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.PromptBuilder;
import org.ohmage.prompt.PromptBuilderFactory;
import org.ohmage.prompt.PromptFactory;
import org.ohmage.prompt.RepeatableSetHeader;
import org.ohmage.prompt.RepeatableSetTerminator;
import org.ohmage.prompt.SurveyElement;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PromptXmlParser {
	
	private static final String TAG = "PromptXmlParser";
	
	//list of tags in our xml schema
	private static final String SURVEY = "survey";
	private static final String SURVEY_ID = "id";
	private static final String REPEATABLE_SET = "repeatableSet";
	private static final String REPEATABLE_SET_ID = "id";
	private static final String REPEATABLE_SET_TERMINATION_QUESTION = "terminationQuestion";
	private static final String REPEATABLE_SET_TERMINATION_TRUE_LABEL = "terminationTrueLabel";
	private static final String REPEATABLE_SET_TERMINATION_FALSE_LABEL = "terminationFalseLabel";
	private static final String REPEATABLE_SET_TERMINATION_SKIP_ENABLED = "terminationSkipEnabled";
	private static final String REPEATABLE_SET_TERMINATION_SKIP_LABEL = "terminationSkipLabel";
	private static final String REPEATABLE_SET_CONDITION = "condition";
	private static final String PROMPT = "prompt";
	private static final String PROMPT_ID = "id";
	private static final String PROMPT_DISPLAY_LABEL = "displayLabel";
	private static final String PROMPT_UNIT = "unit";
	private static final String PROMPT_TEXT = "promptText";
	private static final String PROMPT_EXPLANATION_TEXT = "explanationText";
	private static final String PROMPT_TYPE = "promptType";
	private static final String PROMPT_DEFAULT = "default";
	private static final String PROMPT_CONDITION = "condition";
	private static final String PROMPT_SKIPPABLE = "skippable";
	private static final String PROMPT_SKIP_LABEL = "skipLabel";
	private static final String PROMPT_PROPERTIES = "properties";
	private static final String PROMPT_PROPERTY = "property";
	private static final String PROPERTY_KEY = "key";
	private static final String PROPERTY_VALUE = "value";
	private static final String PROPERTY_LABEL = "label";
	private static final String MESSAGE = "message";
	private static final String MESSAGE_TEXT = "messageText";
	private static final String MESSAGE_CONDITION = "condition";

    private static final String INSTRUCTIONS = "instructions";

	public static List<SurveyElement> parseSurveyElements(InputStream promptXmlStream, String surveyId) throws XmlPullParserException, IOException {
		if(promptXmlStream == null)
			return null;

		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(new BufferedReader(new InputStreamReader(promptXmlStream, "UTF-8")));
		
		List<SurveyElement> surveyElements = null;
		boolean promptInProgress = false;
		boolean repeatableSetInProgress = false;
		boolean messageInProgress = false;
		boolean surveyInProgress = false;
		boolean surveyFound = false;
		
		String id = null;
		String displayLabel = null;
		String unit = null;
		String promptText = null;
		String explanationText = null;
		String promptType = null;
		String defaultValue = null;
		String condition = null;
		String skippable = null;
		String skipLabel = null;
		//ArrayList<String[]> properties = null;
		ArrayList<KVLTriplet> properties = null;
		String key = null;
		String value = null;
		String label = null;
		
		String repeatableSetId = null;
		String terminationQuestion = null;
		String terminationTrueLabel = null;
		String terminationFalseLabel = null;
		String terminationSkipEnabled = null;
		String terminationSkipLabel = null;
		String repeatableSetCondition = null;
		List<Prompt> repeatableSetPrompts = null;
		
		String messageText = null;
		String messageCondition = null;
		
		// TODO deal with optional tags
		
		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			
			String tagName = null;
			
			switch (eventType) {
			case XmlPullParser.START_DOCUMENT:
				
				break;
			case XmlPullParser.START_TAG:
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase(SURVEY)) {
					surveyInProgress = true;
				} else if (surveyInProgress) {
					if (tagName.equalsIgnoreCase(SURVEY_ID) && !promptInProgress && !repeatableSetInProgress && !messageInProgress) {
						if (parser.nextText().trim().equals(surveyId)) {
							surveyFound = true;
							surveyElements = new ArrayList<SurveyElement>();
						}
					} else if (surveyFound) {
						if (tagName.equalsIgnoreCase(REPEATABLE_SET)) {
							repeatableSetInProgress = true;
							
							repeatableSetId = null;
							terminationQuestion = null;
							terminationFalseLabel = null;
							terminationTrueLabel = null;
							terminationSkipLabel = null;
							terminationSkipEnabled = null;
							repeatableSetCondition = null;
							
							repeatableSetPrompts = new ArrayList<Prompt>();
							
						} else if (tagName.equalsIgnoreCase(PROMPT)) {
							promptInProgress = true;
							
							id  = null;
							displayLabel = null;
							unit = null;
							promptText = null;
							explanationText = null;
							promptType = null;
							defaultValue = null;
							condition = null;
							skippable = null;
							skipLabel = null;
							properties = null;
						} else if (tagName.equalsIgnoreCase(MESSAGE)) {
							messageInProgress = true;
							
							messageText = null;
							messageCondition = null;
							
						} else if (promptInProgress) {
							if (tagName.equalsIgnoreCase(PROMPT_ID)) {
								id = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_DISPLAY_LABEL)) {
								displayLabel = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_UNIT)) {
								unit = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_TEXT)) {
								promptText = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_EXPLANATION_TEXT)) {
								explanationText = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_TYPE)) {
								promptType = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_DEFAULT)) {
								defaultValue = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_CONDITION)) {
								condition = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_SKIPPABLE)) {
								skippable = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_SKIP_LABEL)) {
								skipLabel = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_PROPERTIES)) {
								properties = new ArrayList<KVLTriplet>();
							} else if (tagName.equalsIgnoreCase(PROMPT_PROPERTY)) {
								key = null;
								value = null;
								label = null;
							} else if (tagName.equalsIgnoreCase(PROPERTY_KEY)) {
								key = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROPERTY_VALUE)) {
								value = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROPERTY_LABEL)) {
								label = parser.nextText().trim();
							}
						} else if (repeatableSetInProgress) {
							if (tagName.equalsIgnoreCase(REPEATABLE_SET_ID)) {
								repeatableSetId = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(REPEATABLE_SET_CONDITION)) {
								repeatableSetCondition = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(REPEATABLE_SET_TERMINATION_FALSE_LABEL)) {
								terminationFalseLabel = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(REPEATABLE_SET_TERMINATION_TRUE_LABEL)) {
								terminationTrueLabel = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(REPEATABLE_SET_TERMINATION_SKIP_LABEL)) {
								terminationSkipLabel = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(REPEATABLE_SET_TERMINATION_SKIP_ENABLED)) {
								terminationSkipEnabled = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(REPEATABLE_SET_TERMINATION_QUESTION)) {
								terminationQuestion = parser.nextText().trim();
							}
						} else if (messageInProgress) {
							if (tagName.equalsIgnoreCase(MESSAGE_TEXT)) {
								messageText = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(MESSAGE_CONDITION)) {
								messageCondition = parser.nextText().trim();
							}
						}
					}
				}
				
				break;
			case XmlPullParser.END_TAG:
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase(SURVEY)) {
					surveyInProgress = false;
					surveyFound = false;
				} else if (surveyFound) {
					if (tagName.equalsIgnoreCase(PROMPT)) {
						try {
							Prompt prompt = PromptFactory.createPrompt(promptType);
							PromptBuilder builder = PromptBuilderFactory.createPromptBuilder(promptType);
							builder.build(prompt, id, displayLabel, promptText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
							if (repeatableSetInProgress) {
								repeatableSetPrompts.add(prompt);
							} else {
								surveyElements.add(prompt);
							}
						} catch (Exception e) {
							Log.e(TAG, "Error building prompt", e);
						}
						
						promptInProgress = false;
					} else if (tagName.equalsIgnoreCase(PROMPT_PROPERTY)) {
						if (promptInProgress) {
							properties.add(new KVLTriplet(key, value, label));
						}
					} else if (tagName.equalsIgnoreCase(REPEATABLE_SET)) {
						surveyElements.add(new RepeatableSetHeader(repeatableSetId, repeatableSetCondition, repeatableSetPrompts.size()));
						surveyElements.addAll(repeatableSetPrompts);
						surveyElements.add(new RepeatableSetTerminator(repeatableSetId, repeatableSetCondition, terminationQuestion, terminationTrueLabel, terminationFalseLabel, terminationSkipLabel, terminationSkipEnabled, repeatableSetPrompts.size()));
						repeatableSetInProgress = false;
					} else if (tagName.equalsIgnoreCase(MESSAGE)) {
						surveyElements.add(new Message(messageText, messageCondition));
						messageInProgress = false;
					}
				}
				break;
			}
			eventType = parser.next();
		}
		
		return surveyElements;
	}

	/**
	 * parse the instructions from a campaign
	 * @param campaignXml
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
    public static String parseCampaignInstructions(InputStream campaignXml)
            throws XmlPullParserException, IOException {
        if (campaignXml == null)
            return null;

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new BufferedReader(new InputStreamReader(campaignXml, "UTF-8")));

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG
                    && parser.getName().equalsIgnoreCase(INSTRUCTIONS)) {
                parser.next();
                return parser.getText();
            }
            eventType = parser.next();
        }

        return null;
    }
}
