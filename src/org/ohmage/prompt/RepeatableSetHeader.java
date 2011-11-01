package org.ohmage.prompt;

public class RepeatableSetHeader implements SurveyElement{

	String mId;
	String mCondition;
	int mPromptCount;
	boolean mDisplayed;
	
	public RepeatableSetHeader(String id, String condition, int promptCount) {
		this.mId = id;
		this.mCondition = condition;
		this.mPromptCount = promptCount;
		this.mDisplayed = false;
	}
	
	public String getId() {
		return mId;
	}
	
	public String getCondition() {
		return mCondition;
	}
	
	public int getPromptCount() {
		return mPromptCount;
	}
	
	public boolean isDisplayed() {
		return mDisplayed;
	}
	
	public void setDisplayed(boolean displayed) {
		mDisplayed = displayed;
	}
	
	public RepeatableSetHeader getCopy() {
		RepeatableSetHeader copy = new RepeatableSetHeader(mId, mCondition, mPromptCount);
		copy.setDisplayed(mDisplayed);
		return copy;
	}
}
