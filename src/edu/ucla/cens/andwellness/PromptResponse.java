package edu.ucla.cens.andwellness;

public class PromptResponse {

	private String mId;
	private String mValue;
	private boolean mSkipped;
	private boolean mNotDisplayed;
	
	public PromptResponse(String id, String value) {
		mId = id;
		mValue = value;
	}

	public String getId() {
		return mId;
	}

	public String getValue() {
		return mValue;
	}
	
}
