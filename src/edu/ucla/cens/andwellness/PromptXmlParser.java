package edu.ucla.cens.andwellness;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;
import edu.ucla.cens.andwellness.Utilities.KVPair;

public class PromptXmlParser {
	
	//list of tags in our xml schema
	private static final String PROMPT = "prompt";
	private static final String ID = "id";
	private static final String DISPLAY_TYPE = "displayType";
	private static final String DISPLAY_LABEL = "displayLabel";
	private static final String PROMPT_TEXT = "promptText";
	private static final String ABBREVIATED_TEXT = "abbreviatedText";
	private static final String EXPLANATION_TEXT = "explanationText";
	private static final String PROMPT_TYPE = "promptType";
	private static final String DEFAULT = "default";
	private static final String CONDITION = "condition";
	private static final String SKIPPABLE = "skippable";
	private static final String SKIP_LABEL = "skipLabel";
	private static final String PROPERTIES = "properties";
	private static final String P = "p";
	private static final String K = "k";
	private static final String V = "v";

	
	public static List<Prompt> parse(InputStream promptXmlStream) throws XmlPullParserException, IOException {
		
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(new BufferedReader(new InputStreamReader(promptXmlStream, "UTF-8")));
		
		List<Prompt> prompts = null;
		boolean promptInProgress = false;
		
		String id = null;
		String displayType = null;
		String displayLabel = null;
		String promptText = null;
		String abbreviatedText = null;
		String explanationText = null;
		String promptType = null;
		String defaultValue = null;
		String condition = null;
		String skippable = null;
		String skipLabel = null;
		//ArrayList<String[]> properties = null;
		ArrayList<KVPair> properties = null;
		String key = null;
		String value = null;
		
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
					if (tagName.equalsIgnoreCase(ID)) {
						id = parser.nextText();
					} else if (tagName.equalsIgnoreCase(DISPLAY_TYPE)) {
						displayType = parser.nextText();
					} else if (tagName.equalsIgnoreCase(DISPLAY_LABEL)) {
						displayLabel = parser.nextText();
					} else if (tagName.equalsIgnoreCase(PROMPT_TEXT)) {
						promptText = parser.nextText();
					} else if (tagName.equalsIgnoreCase(ABBREVIATED_TEXT)) {
						abbreviatedText = parser.nextText();
					} else if (tagName.equalsIgnoreCase(EXPLANATION_TEXT)) {
						explanationText = parser.nextText();
					} else if (tagName.equalsIgnoreCase(PROMPT_TYPE)) {
						promptType = parser.nextText();
					} else if (tagName.equalsIgnoreCase(DEFAULT)) {
						defaultValue = parser.nextText();
					} else if (tagName.equalsIgnoreCase(CONDITION)) {
						condition = parser.nextText();
					} else if (tagName.equalsIgnoreCase(SKIPPABLE)) {
						skippable = parser.nextText();
					} else if (tagName.equalsIgnoreCase(SKIP_LABEL)) {
						skipLabel = parser.nextText();
					} else if (tagName.equalsIgnoreCase(PROPERTIES)) {
						properties = new ArrayList<KVPair>();
					} else if (tagName.equalsIgnoreCase(P)) {
						key = null;
						value = null;
					} else if (tagName.equalsIgnoreCase(K)) {
						key = parser.nextText();
					} else if (tagName.equalsIgnoreCase(V)) {
						value = parser.nextText();
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
					if (tagName.equalsIgnoreCase(P)) {
						properties.add(new KVPair(key, value));
					}
				}
				break;
			}
			eventType = parser.next();
		}
		
		return prompts;
	}
	
}
