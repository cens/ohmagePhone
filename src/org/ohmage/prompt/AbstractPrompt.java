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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.Utilities.KVLTriplet;
import org.ohmage.prompt.hoursbeforenow.HoursBeforeNowPrompt;
import org.ohmage.prompt.multichoice.MultiChoicePrompt;
import org.ohmage.prompt.multichoicecustom.MultiChoiceCustomPrompt;
import org.ohmage.prompt.number.NumberPrompt;
import org.ohmage.prompt.photo.PhotoPrompt;
import org.ohmage.prompt.remoteactivity.RemoteActivityPrompt;
import org.ohmage.prompt.singlechoice.SingleChoicePrompt;
import org.ohmage.prompt.singlechoicecustom.SingleChoiceCustomPrompt;
import org.ohmage.prompt.text.TextPrompt;
import org.ohmage.prompt.timestamp.TimestampPrompt;


public abstract class AbstractPrompt implements Prompt {

	public static final String SKIPPED_VALUE = "SKIPPED";
	public static final String NOT_DISPLAYED_VALUE = "NOT_DISPLAYED";

	// TODO change private to protected
	protected String mId;
	protected String mPromptType;
	protected String mDisplayType;
	protected String mDisplayLabel;
	protected String mPromptText;
	protected String mAbbreviatedText;
	protected String mExplanationText;
	protected String mDefaultValue;
	protected String mCondition;
	protected String mSkippable;
	protected String mSkipLabel;
	protected ArrayList<KVLTriplet> mProperties;
	
	// should these be here?
	protected boolean mDisplayed;
	protected boolean mSkipped;
	
	public boolean isDisplayed() {
		return mDisplayed;
	}
	
	public boolean isSkipped() {
		return mSkipped;
	}
	
	public void setDisplayed(boolean displayed) {
		this.mDisplayed = displayed;
		// should we clear or not clear?!
		if (!displayed) {
			clearTypeSpecificResponseData();
		}
	}
	
	public void setSkipped(boolean skipped) {
		this.mSkipped = skipped;
		if (skipped) {
			clearTypeSpecificResponseData();
		}
	}
	
	@Override
	public Object getResponseObject() {
		if (!isDisplayed()) {
			return NOT_DISPLAYED_VALUE;
		} else if (isSkipped()) {
			return SKIPPED_VALUE;
		} else {
			return getTypeSpecificResponseObject();
		}
	}
	
	public Object getExtrasObject() {
		return getTypeSpecificExtrasObject();
	}
	
	@Override
	public String getResponseJson() {
		
		JSONObject responseJson = new JSONObject();
		try {
			responseJson.put("prompt_id", this.getId());
			//responseJson.put("value", mSelectedKey);
			responseJson.put("value", getResponseObject());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return responseJson.toString();
	}
	
	protected abstract Object getTypeSpecificResponseObject();
	
	protected abstract Object getTypeSpecificExtrasObject();
	
	protected abstract void clearTypeSpecificResponseData();

	public AbstractPrompt() {
		setDisplayed(false);
		setSkipped(false);
	}
	
	/*public AbstractPrompt(	String id, String displayType, String displayLabel,
					String promptText, String abbreviatedText, String explanationText, 
					String defaultValue, String condition, 
					String skippable, String skipLabel) {
		
		this.mId = id;
		this.mDisplayType = displayType;
		this.mDisplayLabel = displayLabel;
		this.mPromptText = promptText;
		this.mAbbreviatedText = abbreviatedText;
		this.mExplanationText = explanationText;
		this.mDefaultValue = defaultValue;
		this.mCondition = condition;
		this.mSkippable = skippable;
		this.mSkipLabel = skipLabel;
	}*/
	
	public String getId() {
		return mId;
	}
	
	public String getDisplayType() {
		return mDisplayType;
	}
	
	public String getDisplayLabel() {
		return mDisplayLabel;
	}
	
	public String getPromptText() {
		return mPromptText;
	}
		
	public String getAbbreviatedText() {
		return mAbbreviatedText;
	}
	
	public String getExplanationText() {
		return mExplanationText;
	}
	
	public String getDefaultValue() {
		return mDefaultValue;
	}
	
	public String getCondition() {
		return mCondition;
	}
	
	public String getSkippable() {
		return mSkippable;
	}
	
	public String getSkipLabel() {
		return mSkipLabel;
	}
	
	public ArrayList<KVLTriplet> getProperties() {
		return mProperties;
	}
	
	public void setId(String id) {
		this.mId = id;
	}

	public void setDisplayType(String displayType) {
		this.mDisplayType = displayType;
	}

	public void setDisplayLabel(String displayLabel) {
		this.mDisplayLabel = displayLabel;
	}

	public void setPromptText(String promptText) {
		this.mPromptText = promptText;
	}

	public void setAbbreviatedText(String abbreviatedText) {
		this.mAbbreviatedText = abbreviatedText;
	}

	public void setExplanationText(String explanationText) {
		this.mExplanationText = explanationText;
	}
	
	public void setDefaultValue(String defaultValue) {
		this.mDefaultValue = defaultValue;
	}

	public void setCondition(String condition) {
		this.mCondition = condition;
	}

	public void setSkippable(String skippable) {
		this.mSkippable = skippable;
	}

	public void setSkipLabel(String skipLabel) {
		this.mSkipLabel = skipLabel;
	}
	
	public void setProperties(ArrayList<KVLTriplet> properties) {
		this.mProperties = properties;
	}
	
	public Prompt getCopy() {

		Prompt prompt = null;
		PromptBuilder builder = null;
		
		if (this instanceof SingleChoicePrompt) {
			prompt = PromptFactory.createPrompt(PromptFactory.SINGLE_CHOICE);
			builder = PromptBuilderFactory.createPromptBuilder(PromptFactory.SINGLE_CHOICE);
		} else if (this instanceof SingleChoiceCustomPrompt) {
			prompt = PromptFactory.createPrompt(PromptFactory.SINGLE_CHOICE_CUSTOM);
			builder = PromptBuilderFactory.createPromptBuilder(PromptFactory.SINGLE_CHOICE_CUSTOM);
		} else if (this instanceof MultiChoicePrompt) {
			prompt = PromptFactory.createPrompt(PromptFactory.MULTI_CHOICE);
			builder = PromptBuilderFactory.createPromptBuilder(PromptFactory.MULTI_CHOICE);
		} else if (this instanceof MultiChoiceCustomPrompt) {
			prompt = PromptFactory.createPrompt(PromptFactory.MULTI_CHOICE_CUSTOM);
			builder = PromptBuilderFactory.createPromptBuilder(PromptFactory.MULTI_CHOICE_CUSTOM);
		} else if (this instanceof NumberPrompt) {
			prompt = PromptFactory.createPrompt(PromptFactory.NUMBER);
			builder = PromptBuilderFactory.createPromptBuilder(PromptFactory.NUMBER);
		} else if (this instanceof HoursBeforeNowPrompt) {
			prompt = PromptFactory.createPrompt(PromptFactory.HOURS_BEFORE_NOW);
			builder = PromptBuilderFactory.createPromptBuilder(PromptFactory.HOURS_BEFORE_NOW);
		} else if (this instanceof TimestampPrompt) {
			prompt = PromptFactory.createPrompt(PromptFactory.TIMESTAMP);
			builder = PromptBuilderFactory.createPromptBuilder(PromptFactory.TIMESTAMP);
		} else if (this instanceof TextPrompt) {
			prompt = PromptFactory.createPrompt(PromptFactory.TEXT);
			builder = PromptBuilderFactory.createPromptBuilder(PromptFactory.TEXT);
		} else if (this instanceof PhotoPrompt) {
			prompt = PromptFactory.createPrompt(PromptFactory.PHOTO);
			builder = PromptBuilderFactory.createPromptBuilder(PromptFactory.PHOTO);
		} else if (this instanceof RemoteActivityPrompt) {
			prompt = PromptFactory.createPrompt(PromptFactory.REMOTE_ACTIVITY);
			builder = PromptBuilderFactory.createPromptBuilder(PromptFactory.REMOTE_ACTIVITY);
		}

		builder.build(prompt, mId, mDisplayType, mDisplayLabel, mPromptText, mAbbreviatedText, mExplanationText, mDefaultValue, mCondition, mSkippable, mSkipLabel, mProperties);
		return prompt;
	}
	
	public static String getDisplayValue(AbstractPrompt allPromptList, String value) {
		if(allPromptList instanceof SingleChoicePrompt){
			SingleChoicePrompt prompt = (SingleChoicePrompt)allPromptList;
			List<KVLTriplet> choiceKVLTriplet = prompt.getChoices();
			for(KVLTriplet i : choiceKVLTriplet){
				if(i.key.equals(value)){
					return i.label;
				}
			}
		} 
		else if(allPromptList instanceof SingleChoiceCustomPrompt){
			SingleChoiceCustomPrompt prompt = (SingleChoiceCustomPrompt)allPromptList;
			List<KVLTriplet> choiceKVLTriplet = prompt.getChoices();
			for(KVLTriplet i : choiceKVLTriplet){
				if(i.key.equals(value)){
					return i.label;
				}
			}
		} 
		else if(allPromptList instanceof MultiChoicePrompt){
			MultiChoicePrompt prompt = (MultiChoicePrompt)allPromptList;
			List<KVLTriplet> choiceKVLTriplet = prompt.getChoices();
			String result = "";
			try{
				JSONArray jsonValue = new JSONArray(value);
				for(int k=0; k<jsonValue.length(); k++){
					String answer = jsonValue.get(k).toString(); 
					for(KVLTriplet i : choiceKVLTriplet){
						if(i.key.equals(answer)){
							result += i.label + "  ";
						}
					}
				}
			}
			catch(Exception e){
				result = value;
			}
			return result;
		} 
		else if(allPromptList instanceof MultiChoiceCustomPrompt){
			MultiChoiceCustomPrompt prompt = (MultiChoiceCustomPrompt)allPromptList;
			List<KVLTriplet> choiceKVLTriplet = prompt.getChoices();
			String result = "";
			try{
				JSONArray jsonValue = new JSONArray(value);
				for(int k=0; k<jsonValue.length(); k++){
					String answer = jsonValue.get(k).toString(); 
					for(KVLTriplet i : choiceKVLTriplet){
						if(i.key.equals(answer)){
							result += i.label + "  ";
						}
					}
				}
			}
			catch(Exception e){
				result = value;
			}
			return result;
		} 
		else if(allPromptList instanceof NumberPrompt){
			NumberPrompt prompt = (NumberPrompt)allPromptList;
			return String.valueOf(prompt.getValue());
		} 
		else if(allPromptList instanceof HoursBeforeNowPrompt){
			HoursBeforeNowPrompt prompt = (HoursBeforeNowPrompt)allPromptList;
			return String.valueOf(prompt.getValue());
		} 
		else if(allPromptList instanceof TextPrompt){
			//TextPrompt prompt = (TextPrompt)allPromptList;
			return value;
		} 
		else if(allPromptList instanceof PhotoPrompt){
			PhotoPrompt prompt = (PhotoPrompt)allPromptList;
			//TODO Add a feature to display Photo
			return "";
		} 
		else if(allPromptList instanceof RemoteActivityPrompt){
			RemoteActivityPrompt prompt = (RemoteActivityPrompt)allPromptList;
			//TODO Add a feature to handle remote activity prompt
			return value;
		}
		return value;
	}

	@Override
	public void onHidden() {
		// By default there is nothing we need to do
	}
}
