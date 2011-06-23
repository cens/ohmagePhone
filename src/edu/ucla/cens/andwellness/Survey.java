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

public class Survey {

	private String mId;
	private String mTitle;
	private String mDescription;
	private String mIntroText;
	private String mSubmitText;
	private String mShowSummary;
	private String mEditSummary;
	private String mSummaryText;
	private boolean mAnytime;
	private boolean mTriggered;
	
	private Survey(String id, String title, String description, String introText, String submitText, String showSummary, String editSummary, String summaryText, boolean anytime) {
		
		this.mId = id;
		this.mTitle = title;
		this.mDescription = description;
		this.mIntroText = introText;
		this.mSubmitText = submitText;
		this.mShowSummary = showSummary;
		this.mEditSummary = editSummary;
		this.mSummaryText = summaryText;
		this.mAnytime = anytime;
		this.mTriggered = false;
	}
	
	public static Survey build(String id, String title, String description, String introText, String submitText, String showSummary, String editSummary, String summaryText, String anytime) {
		if (anytime.equals("false")) {
			return new Survey(id, title, description, introText, submitText, showSummary, editSummary, summaryText, false);
		} else {
			return new Survey(id, title, description, introText, submitText, showSummary, editSummary, summaryText, true);
		}
	}

	public String getId() {
		return mId;
	}

	public String getTitle() {
		return mTitle;
	}

	public String getDescription() {
		return mDescription;
	}
	
	public String getIntroText() {
		return mIntroText;
	}

	public String getSubmitText() {
		return mSubmitText;
	}

	public String getShowSummary() {
		return mShowSummary;
	}

	public String getEditSummary() {
		return mEditSummary;
	}

	public String getSummaryText() {
		return mSummaryText;
	}

	public boolean isAnytime() {
		return mAnytime;
	}
	
//	public void setTriggered(boolean value) {
//		mTriggered = value;
//	}
//	
//	public boolean isTriggered() {
//		return mTriggered;
//	}
}
