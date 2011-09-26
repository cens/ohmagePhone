package org.ohmage.db;

import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Surveys;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class Models {

	public final static class Campaign {

		public static final int STATUS_READY = 0;
		public static final int STATUS_REMOTE = 1;
		public static final int STATUS_STOPPED = 2;
		public static final int STATUS_OUT_OF_DATE = 3;
		public static final int STATUS_INVALID_USER_ROLE = 4;
		public static final int STATUS_DELETED = 5;
		public static final int STATUS_VAGUE = 6;
		public static final int STATUS_DOWNLOADING = 7;

		public long _id;
		public String mUrn;
		public String mName;
		public String mDescription;
		public String mCreationTimestamp;
		public String mDownloadTimestamp;
		public String mXml;
		public int mStatus;
		public String mIcon;
		public String mPrivacy;

		/**
		 * Returns a list of Campaign objects from the given cursor.
		 * 
		 * @param cursor a cursor containing the fields specified in the Campaign schema, which is closed when this method returns.
		 * @return a List of Campaign objects
		 */
		public static List<Campaign> fromCursor(Cursor cursor) {
			List<Campaign> campaigns = new ArrayList<Campaign>();

			cursor.moveToFirst();

			for (int i = 0; i < cursor.getCount(); i++) {

				Campaign c = new Campaign();
				c._id = cursor.getLong(cursor.getColumnIndex(Campaigns._ID));
				c.mUrn = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_URN));
				c.mName = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_NAME));
				c.mDescription = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_DESCRIPTION));
				c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_CREATED));
				c.mDownloadTimestamp = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_DOWNLOADED));
				c.mXml = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_CONFIGURATION_XML));
				c.mStatus = cursor.getInt(cursor.getColumnIndex(Campaigns.CAMPAIGN_STATUS));
				c.mIcon = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_ICON));
				c.mPrivacy = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_PRIVACY));
				campaigns.add(c);

				cursor.moveToNext();
			}

			cursor.close();

			return campaigns;
		}

		public ContentValues toCV() {
			ContentValues values = new ContentValues();

			values.put(Campaigns.CAMPAIGN_URN, mUrn);
			values.put(Campaigns.CAMPAIGN_NAME, mName);
			values.put(Campaigns.CAMPAIGN_DESCRIPTION, mDescription);
			values.put(Campaigns.CAMPAIGN_CREATED, mCreationTimestamp);
			values.put(Campaigns.CAMPAIGN_DOWNLOADED, mDownloadTimestamp);
			values.put(Campaigns.CAMPAIGN_CONFIGURATION_XML, mXml);
			values.put(Campaigns.CAMPAIGN_STATUS, mStatus);
			values.put(Campaigns.CAMPAIGN_ICON, mIcon);
			values.put(Campaigns.CAMPAIGN_PRIVACY, mPrivacy);
			return values;
		}
	}

	public final static class Survey {

		public static final int STATUS_NORMAL = 0;
		public static final int STATUS_TRIGGERED = 1;

		public long _id;
		public String mSurveyID;
		public String mCampaignUrn;
		public String mTitle;
		public String mDescription;
		public String mSubmitText;
		public boolean mShowSummary;
		public boolean mEditSummary;
		public String mSummaryText;
		public String mIntroText;
		public boolean mAnytime;
		public int mStatus;
		
        /**
         * Returns a list of Survey objects from the given cursor.
         * 
         * @param cursor a cursor containing the fields specified in the Survey schema, which is closed when this method returns.
         * @return a List of Survey objects
         */
        public static List<Survey> fromCursor(Cursor cursor) {
        	List<Survey> surveys = new ArrayList<Survey>();
    		
    		cursor.moveToFirst();
    		
    		for (int i = 0; i < cursor.getCount(); i++) {
    			
    			Survey s = new Survey();
    			s._id = cursor.getLong(cursor.getColumnIndex(Surveys._ID));
    			s.mSurveyID = cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_ID));
    			s.mCampaignUrn = cursor.getString(cursor.getColumnIndex(Surveys.CAMPAIGN_URN));
    			s.mTitle = cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_TITLE));
    			s.mDescription = cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_DESCRIPTION));
    			s.mSubmitText = cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_SUBMIT_TEXT));
    			s.mShowSummary = cursor.getInt(cursor.getColumnIndex(Surveys.SURVEY_SHOW_SUMMARY)) == 0 ? false : true;
    			s.mEditSummary = cursor.getInt(cursor.getColumnIndex(Surveys.SURVEY_EDIT_SUMMARY)) == 0 ? false : true;
    			s.mSummaryText = cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_SUMMARY_TEXT));
    			s.mIntroText = cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_INTRO_TEXT));
    			s.mAnytime = cursor.getInt(cursor.getColumnIndex(Surveys.SURVEY_ANYTIME)) == 0 ? false : true;
    			s.mStatus = cursor.getInt(cursor.getColumnIndex(Surveys.SURVEY_STATUS));
    			surveys.add(s);
    			
    			cursor.moveToNext();
    		}
    		
    		cursor.close();
    		
    		return surveys;
        }
        
        public ContentValues toCV() {
        	ContentValues values = new ContentValues();
        	
        	values.put(Surveys.SURVEY_ID, mSurveyID);
        	values.put(Surveys.CAMPAIGN_URN, mCampaignUrn);
        	values.put(Surveys.SURVEY_TITLE, mTitle);
        	values.put(Surveys.SURVEY_DESCRIPTION, mDescription);
        	values.put(Surveys.SURVEY_SUBMIT_TEXT, mSubmitText);
        	values.put(Surveys.SURVEY_SHOW_SUMMARY, mShowSummary);
        	values.put(Surveys.SURVEY_EDIT_SUMMARY, mEditSummary);
        	values.put(Surveys.SURVEY_SUMMARY_TEXT, mSummaryText);
        	values.put(Surveys.SURVEY_INTRO_TEXT, mIntroText);
        	values.put(Surveys.SURVEY_ANYTIME, mAnytime);
        	values.put(Surveys.SURVEY_STATUS, mStatus);
        	
        	return values;
        }
	}
}
