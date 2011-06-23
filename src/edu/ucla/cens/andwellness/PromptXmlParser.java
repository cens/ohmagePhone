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
package edu.ucla.cens.andwellness;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;
import edu.ucla.cens.andwellness.Utilities.KVLTriplet;
import edu.ucla.cens.andwellness.prompt.Prompt;
import edu.ucla.cens.andwellness.prompt.PromptBuilder;
import edu.ucla.cens.andwellness.prompt.PromptBuilderFactory;
import edu.ucla.cens.andwellness.prompt.PromptFactory;

public class PromptXmlParser {
	
	//list of tags in our xml schema
	private static final String CAMPAIGN_NAME = "campaignName";
	private static final String CAMPAIGN_URN = "campaignUrn";
	private static final String SERVER_URL = "serverUrl";
	private static final String SURVEYS = "surveys";
	private static final String SURVEY = "survey";
	private static final String SURVEY_ID = "id";
	private static final String SURVEY_TITLE = "title";
	private static final String SURVEY_DESCRIPTION = "description";
	private static final String SURVEY_INTRO_TEXT = "introText";
	private static final String SURVEY_SUBMIT_TEXT = "submitText";
	private static final String SURVEY_SHOW_SUMMARY = "showSummary";
	private static final String SURVEY_EDIT_SUMMARY = "editSummary";
	private static final String SURVEY_SUMMARY_TEXT = "summaryText";
	private static final String SURVEY_ANYTIME = "anytime";
	private static final String SURVEY_CONTENT_LIST = "contentList";
	private static final String REPEATABLE_SET = "repeatableSet";
	private static final String REPEATABLE_SET_ID = "id";
	private static final String REPEATABLE_SET_TERMINATION_QUESTION = "terminationQuestion";
	private static final String REPEATABLE_SET_TERMINATION_TRUE_LABEL = "terminationTrueLabel";
	private static final String REPEATABLE_SET_TERMINATION_FALSE_LABEL = "terminationFalseLabel";
	private static final String REPEATABLE_SET_TERMINATION_SKIP_ENABLED = "terminationSkipEnabled";
	private static final String REPEATABLE_SET_TERMINATION_SKIP_LABEL = "terminationSkipLabel";
	private static final String REPEATABLE_SET_CONDITION = "condition";
	private static final String REPEATABLE_SET_PROMPTS = "prompts";
	private static final String PROMPT = "prompt";
	private static final String PROMPT_ID = "id";
	private static final String PROMPT_DISPLAY_TYPE = "displayType";
	private static final String PROMPT_DISPLAY_LABEL = "displayLabel";
	private static final String PROMPT_UNIT = "unit";
	private static final String PROMPT_TEXT = "promptText";
	private static final String PROMPT_ABBREVIATED_TEXT = "abbreviatedText";
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
	
	public static Map<String, String> parseCampaignInfo(InputStream promptXmlStream) throws XmlPullParserException, IOException {
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(new BufferedReader(new InputStreamReader(promptXmlStream, "UTF-8")));
		
		HashMap<String, String> map = null;
		
		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			
			if (map != null && map.containsKey("campaign_name") && map.containsKey("campaign_urn") && map.containsKey("server_url")) {
				return map;
			}
			
			String tagName = null;
			
			switch (eventType) {
			
			case XmlPullParser.START_DOCUMENT:
				map = new HashMap<String, String>(2);
				break;
				
			case XmlPullParser.START_TAG:
				
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase(CAMPAIGN_NAME)) {
					map.put("campaign_name", parser.nextText().trim());
				} else if (tagName.equalsIgnoreCase(CAMPAIGN_URN)) {
					map.put("campaign_urn", parser.nextText().trim());
				} else if (tagName.equalsIgnoreCase(SERVER_URL)) {
					map.put("server_url", parser.nextText().trim());
				} 
				break;
				
			case XmlPullParser.END_TAG:
								 
				break;
			}
			eventType = parser.next();
		}
		
		return map;
	}
	
	public static List<Survey> parseSurveys(InputStream promptXmlStream) throws XmlPullParserException, IOException {
		
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(new BufferedReader(new InputStreamReader(promptXmlStream, "UTF-8")));
		
		List<Survey> surveys = null;
		boolean surveyInProgress = false;
		boolean contentListInProgress = false;
		
		String id = null;
		String title = null;
		String description = null;
		String introText = null;
		String submitText = null;
		String showSummary = null;
		String editSummary = null;
		String summaryText = null;
		String anytime = null;
		
		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			
			String tagName = null;
			
			switch (eventType) {
			case XmlPullParser.START_DOCUMENT:
				surveys = new ArrayList<Survey>();
				break;
			case XmlPullParser.START_TAG:
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase(SURVEY)) {
					surveyInProgress = true;
					
					
					id = null;
					title = null;
					description = null;
					introText = null;
					submitText = null;
					showSummary = null;
					editSummary = null;
					summaryText = null;
					anytime = null;
					
				} else if (surveyInProgress && !contentListInProgress) {
					if (tagName.equalsIgnoreCase(SURVEY_ID)) {
						id = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(SURVEY_TITLE)) {
						title = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(SURVEY_DESCRIPTION)) {
						description = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(SURVEY_INTRO_TEXT)) {
						introText = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(SURVEY_SUBMIT_TEXT)) {
						submitText = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(SURVEY_SHOW_SUMMARY)) {
						showSummary = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(SURVEY_EDIT_SUMMARY)) {
						editSummary = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(SURVEY_SUMMARY_TEXT)) {
						summaryText = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(SURVEY_ANYTIME)) {
						anytime = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(SURVEY_CONTENT_LIST)) {
						contentListInProgress = true;
					} 
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase(SURVEY)) {
					if(!id.equals("foodButton") && !id.equals("stressButton")) {
						Survey survey = Survey.build(id, title, description, introText, submitText, showSummary, editSummary, summaryText, anytime);
						surveys.add(survey);
					}
					surveyInProgress = false;
				} else if (tagName.equalsIgnoreCase(SURVEY_CONTENT_LIST)) {
					contentListInProgress = false;
				} 
				break;
			}
			eventType = parser.next();
		}
		
		return surveys;
	}

public static List<Prompt> parsePrompts(InputStream promptXmlStream, String surveyId) throws XmlPullParserException, IOException {
		
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(new BufferedReader(new InputStreamReader(promptXmlStream, "UTF-8")));
		
		List<Prompt> prompts = null;
		boolean promptInProgress = false;
		boolean surveyInProgress = false;
		boolean surveyFound = false;
		
		String id = null;
		String displayType = null;
		String displayLabel = null;
		String unit = null;
		String promptText = null;
		String abbreviatedText = null;
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
					if (tagName.equalsIgnoreCase(SURVEY_ID) && !promptInProgress) {
						if (parser.nextText().trim().equals(surveyId)) {
							surveyFound = true;
							prompts = new ArrayList<Prompt>();
						}
					} else if (surveyFound) {
						if (tagName.equalsIgnoreCase(PROMPT)) {
							promptInProgress = true;
							
							id  = null;
							displayType = null;
							displayLabel = null;
							unit = null;
							promptText = null;
							abbreviatedText = null;
							explanationText = null;
							promptType = null;
							defaultValue = null;
							condition = null;
							skippable = null;
							skipLabel = null;
							properties = null;
							
						} else if (promptInProgress) {
							if (tagName.equalsIgnoreCase(PROMPT_ID)) {
								id = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_DISPLAY_TYPE)) {
								displayType = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_DISPLAY_LABEL)) {
								displayLabel = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_UNIT)) {
								unit = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_TEXT)) {
								promptText = parser.nextText().trim();
							} else if (tagName.equalsIgnoreCase(PROMPT_ABBREVIATED_TEXT)) {
								abbreviatedText = parser.nextText().trim();
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
						Prompt prompt = PromptFactory.createPrompt(promptType);
						PromptBuilder builder = PromptBuilderFactory.createPromptBuilder(promptType);
						builder.build(prompt, id, displayType, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
						prompts.add(prompt);
						promptInProgress = false;
					} else if (promptInProgress) {
						if (tagName.equalsIgnoreCase(PROMPT_PROPERTY)) {
							properties.add(new KVLTriplet(key, value, label));
						}
					}
				}
				break;
			}
			eventType = parser.next();
		}
		
		return prompts;
	}

	/*public static List<Prompt> parsePrompts(InputStream promptXmlStream) throws XmlPullParserException, IOException {
		
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(new BufferedReader(new InputStreamReader(promptXmlStream, "UTF-8")));
		
		List<Prompt> prompts = null;
		boolean promptInProgress = false;
		
		String id = null;
		String displayType = null;
		String displayLabel = null;
		String unit = null;
		String promptText = null;
		String abbreviatedText = null;
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
		
		// TODO deal with optional tags
		
		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			
			String tagName = null;
			
			switch (eventType) {
			case XmlPullParser.START_DOCUMENT:
				prompts = new ArrayList<Prompt>();
				break;
			case XmlPullParser.START_TAG:
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase(PROMPT)) {
					promptInProgress = true;
					
					id  = null;
					displayType = null;
					displayLabel = null;
					unit = null;
					promptText = null;
					abbreviatedText = null;
					explanationText = null;
					promptType = null;
					defaultValue = null;
					condition = null;
					skippable = null;
					skipLabel = null;
					properties = null;
					
				} else if (promptInProgress) {
					if (tagName.equalsIgnoreCase(PROMPT_ID)) {
						id = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(PROMPT_DISPLAY_TYPE)) {
						displayType = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(PROMPT_DISPLAY_LABEL)) {
						displayLabel = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(PROMPT_UNIT)) {
						unit = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(PROMPT_TEXT)) {
						promptText = parser.nextText().trim();
					} else if (tagName.equalsIgnoreCase(PROMPT_ABBREVIATED_TEXT)) {
						abbreviatedText = parser.nextText().trim();
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
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = parser.getName();
				if (tagName.equalsIgnoreCase(PROMPT)) {
					Prompt prompt = PromptFactory.createPrompt(promptType);
					PromptBuilder builder = PromptBuilderFactory.createPromptBuilder(promptType);
					builder.build(prompt, id, displayType, displayLabel, promptText, abbreviatedText, explanationText, defaultValue, condition, skippable, skipLabel, properties);
					prompts.add(prompt);
					promptInProgress = false;
				} else if (promptInProgress) {
					if (tagName.equalsIgnoreCase(PROMPT_PROPERTY)) {
						properties.add(new KVLTriplet(key, value, label));
					}
				}
				break;
			}
			eventType = parser.next();
		}
		
		return prompts;
	}*/
	
}
