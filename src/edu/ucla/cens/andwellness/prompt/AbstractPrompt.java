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

import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractPrompt implements Prompt {

	// TODO change private to protected
	protected String mId;
	protected String mDisplayType;
	protected String mDisplayLabel;
	protected String mPromptText;
	protected String mAbbreviatedText;
	protected String mExplanationText;
	protected String mDefaultValue;
	protected String mCondition;
	protected String mSkippable;
	protected String mSkipLabel;
	
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
	
	public Object getResponseObject() {
		if (!isDisplayed()) {
			return "NOT_DISPLAYED";
		} else if (isSkipped()) {
			return "SKIPPED";
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
	
}
