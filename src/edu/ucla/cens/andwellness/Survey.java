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
	private String mAnytime;
	private String mContentListXml;
	
	public Survey(String id, String title, String description, String introText, String submitText, String showSummary, String editSummary, String summaryText, String anytime, String contentListXml) {
		
		this.mId = id;
		this.mTitle = title;
		this.mDescription = description;
		this.mIntroText = introText;
		this.mSubmitText = submitText;
		this.mShowSummary = showSummary;
		this.mEditSummary = editSummary;
		this.mSummaryText = summaryText;
		this.mAnytime = anytime;
		this.mContentListXml = contentListXml;
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

	public String getAnytime() {
		return mAnytime;
	}

	public String getContentListXml() {
		return mContentListXml;
	}
	
}
