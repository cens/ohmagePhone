package org.ohmage.db;

import org.ohmage.OhmageApplication;
import org.ohmage.OhmageCache;
import org.ohmage.Utilities;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.SurveyPrompts;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.service.SurveyGeotagService;
import org.ohmage.triggers.glue.TriggerFramework;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Models {

	public static class DbModel {
		public long _id;

		/**
		 * Db models should delete any external files associated with it here.
		 * This is called by {@link DbProvider#delete(android.net.Uri, String, String[])}
		 */
		public void cleanUp(Context context) { }

	}

	public final static class Campaign extends DbModel {

		public static final int STATUS_READY = 0;
		public static final int STATUS_REMOTE = 1;
		public static final int STATUS_STOPPED = 2;
		public static final int STATUS_OUT_OF_DATE = 3;
		public static final int STATUS_INVALID_USER_ROLE = 4;
		public static final int STATUS_NO_EXIST = 5;
		public static final int STATUS_VAGUE = 6;
		public static final int STATUS_DOWNLOADING = 7;
		
		public static final String PRIVACY_UNKNOWN = "unknown";
		public static final String PRIVACY_SHARED = "shared";
		public static final String PRIVACY_PRIVATE = "private";

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

		public static File getCampaignImageDir(Context context, String campaignUrn) {
			File f = new File(OhmageApplication.getImageDirectory(context), campaignUrn.replace(':', '_'));
			f.mkdirs();
			return f;
		}
		
		private File getCampaignImageDir(Context context) {
			return getCampaignImageDir(context, mUrn);
		}
		
		public static File getCampaignImage(Context context, String campaignUrn, String uuid) {
			return new File(Campaign.getCampaignImageDir(context, campaignUrn), "temp" + uuid);
		}

		/**
		 * Launch the Trigger list for this campaign
		 * @param context
		 * @param campaignUrn
		 */
		public static Intent launchTriggerIntent(Context context, String campaignUrn) {
			List<String> surveyTitles = new ArrayList<String>();
			
			// grab a list of surveys for this campaign
			Cursor surveys = context.getContentResolver().query(Campaigns.buildSurveysUri(campaignUrn), new String[] { Surveys.SURVEY_ID, Surveys.SURVEY_TITLE }, null, null, null);
			
			while (surveys.moveToNext()) {
				String id = surveys.getString(surveys.getColumnIndex(Surveys.SURVEY_ID));
				if(!id.equals("foodButton") && !id.equals("stressButton"))
					surveyTitles.add(surveys.getString(surveys.getColumnIndex(Surveys.SURVEY_TITLE)));
			}
			
			return TriggerFramework.launchTriggersIntent(context, campaignUrn, surveyTitles.toArray(new String[surveyTitles.size()]));
		}

		/**
		 * Returns the uri of the first ready campaign in the db which should be the campaign used in single campaign mode
		 * @param context
		 * @return the urn of the first ready campaign from the db, or null
		 */
		public static String getSingleCampaign(Context context) {
			return getFirstCampaign(context, Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY, null);
		}

		/**
		 * Returns the uri of the first campaign in the db which should be the campaign used in single campaign mode.
		 * @param context
		 * @return the urn of the first campaign from the db, or null
		 */
		public static String getFirstAvaliableCampaign(Context context) {
			return getFirstCampaign(context, Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_REMOTE + " OR " + Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY, null);
		}

		/**
		 * Helper method for return the first campaign urn given where and args
		 * @param context
		 * @param where
		 * @param args
		 * @return the urn of the first campaign
		 */
		public static String getFirstCampaign(Context context, String where, String[] args) {
			Cursor campaign = context.getContentResolver().query(Campaigns.CONTENT_URI, new String[] { Campaigns.CAMPAIGN_URN }, where, args, Campaigns.CAMPAIGN_CREATED + " DESC");
			if(campaign.moveToFirst())
				return campaign.getString(0);
			return null;
		}

		/**
		 * Sets the campaign to {@link Campaign#STATUS_REMOTE}. Also removes surveys and responses.
		 * @param context
		 * @param campaignUrn
		 */
		public static void setRemote(Context context, String campaignUrn) {
			setRemote(context, campaignUrn, Campaign.STATUS_REMOTE);
		}

		/**
		 * Sets the campaign to a remote status. Also removes surveys and responses.
		 * @param context
		 * @param campaignUrn
		 * @param status
		 */
		public static void setRemote(Context context, String campaignUrn, int status) {
			ContentValues cv = new ContentValues();
			cv.put(Campaigns.CAMPAIGN_STATUS, status);
			cv.put(Campaigns.CAMPAIGN_CONFIGURATION_XML, "");
			context.getContentResolver().update(Campaigns.CONTENT_URI, cv, Campaigns.CAMPAIGN_URN + "=?", new String[]{campaignUrn});
			context.getContentResolver().delete(Responses.CONTENT_URI, Responses.CAMPAIGN_URN + "=?", new String[]{campaignUrn});
		}

		@Override
		public void cleanUp(Context context) {
			if (mStatus != Campaign.STATUS_REMOTE)
				TriggerFramework.resetTriggerSettings(context, mUrn);

			try {
				if(mIcon != null)
					OhmageCache.getCachedFile(context, new URI(mIcon)).delete();
			}catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				if(getCampaignImageDir(context).exists())
					Utilities.delete(getCampaignImageDir(context));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public final static class Survey extends DbModel {

		public static final int STATUS_NORMAL = 0;
		public static final int STATUS_TRIGGERED = 1;

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

	public final static class SurveyPrompt extends DbModel {

		public long mSurveyPID;
		public String mSurveyID;
		public String mCompositeID;
		public String mPromptID;
		public String mPromptText;
		public String mPromptType;
		public String mProperties;

		/**
		 * Returns a list of Survey objects from the given cursor.
		 * 
		 * @param cursor a cursor containing the fields specified in the Survey schema, which is closed when this method returns.
		 * @return a List of Survey objects
		 */
		public static List<SurveyPrompt> fromCursor(Cursor cursor) {
			List<SurveyPrompt> surveyprompts = new ArrayList<SurveyPrompt>();

			cursor.moveToFirst();

			for (int i = 0; i < cursor.getCount(); i++) {

				SurveyPrompt temp = new SurveyPrompt();
				temp._id = cursor.getLong(cursor.getColumnIndex(SurveyPrompts._ID));
				temp.mSurveyPID = cursor.getLong(cursor.getColumnIndex(SurveyPrompts.SURVEY_PID));
				temp.mSurveyID = cursor.getString(cursor.getColumnIndex(SurveyPrompts.SURVEY_ID));
				temp.mCompositeID = cursor.getString(cursor.getColumnIndex(SurveyPrompts.COMPOSITE_ID));
				temp.mPromptID = cursor.getString(cursor.getColumnIndex(SurveyPrompts.PROMPT_ID));
				temp.mPromptText = cursor.getString(cursor.getColumnIndex(SurveyPrompts.SURVEY_PROMPT_TEXT));
				temp.mPromptType = cursor.getString(cursor.getColumnIndex(SurveyPrompts.SURVEY_PROMPT_TYPE));
				temp.mProperties = cursor.getString(cursor.getColumnIndex(SurveyPrompts.SURVEY_PROMPT_PROPERTIES));
				surveyprompts.add(temp);

				cursor.moveToNext();
			}

			cursor.close();

			return surveyprompts;
		}

		public ContentValues toCV() {
			ContentValues values = new ContentValues();

			values.put(SurveyPrompts.SURVEY_PID, mSurveyPID);
			values.put(SurveyPrompts.SURVEY_ID, mSurveyID);
			values.put(SurveyPrompts.COMPOSITE_ID, mCompositeID);
			values.put(SurveyPrompts.PROMPT_ID, mPromptID);
			values.put(SurveyPrompts.SURVEY_PROMPT_TEXT, mPromptText);
			values.put(SurveyPrompts.SURVEY_PROMPT_TYPE, mPromptType);
			values.put(SurveyPrompts.SURVEY_PROMPT_PROPERTIES, mProperties);

			return values;
		}
	}

	public final static class Response extends DbModel {

		public static final int STATUS_UPLOADED = 0;
		public static final int STATUS_UPLOADING = 1;
		public static final int STATUS_QUEUED = 2;
		public static final int STATUS_STANDBY = 3;
		public static final int STATUS_ERROR_INVALID_USER_ROLE = 4;
		public static final int STATUS_ERROR_CAMPAIGN_NO_EXIST = 5;
		public static final int STATUS_ERROR_AUTHENTICATION = 6;
		public static final int STATUS_WAITING_FOR_LOCATION = 7;
		public static final int STATUS_DOWNLOADED = 8;
		public static final int STATUS_ERROR_OTHER = 9;
		public static final int STATUS_ERROR_CAMPAIGN_STOPPED = 10;
		public static final int STATUS_ERROR_CAMPAIGN_OUT_OF_DATE = 11;
		public static final int STATUS_ERROR_HTTP = 12;

		/** the campaign URN for which to record the survey response */
		public String uuid;
		public String campaignUrn;
		public String username;
		public String date;
		public long time;
		public String timezone;
		public String locationStatus;
		public double locationLatitude;
		public double locationLongitude;
		public String locationProvider;
		public float locationAccuracy;
		public long locationTime;
		/** the id of the survey to which the response corresponds, in URN format */
		public String surveyId;
		public String surveyLaunchContext;
		public String response;
		public int status;
		public String hashcode;

		/**
		 * Returns a list of Response objects from the given cursor.
		 * 
		 * @param cursor a cursor containing the fields specified in the Response schema, which is closed when this method returns.
		 * @return a List of Response objects
		 */
		public static List<Response> fromCursor(Cursor cursor) {

			ArrayList<Response> responses = new ArrayList<Response>();

			cursor.moveToFirst();

			for (int i = 0; i < cursor.getCount(); i++) {

				Response r = new Response();
				r._id = cursor.getLong(cursor.getColumnIndex(Responses._ID));
				r.uuid = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_UUID));
				r.campaignUrn = cursor.getString(cursor.getColumnIndex(Responses.CAMPAIGN_URN));
				r.username = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_USERNAME));
				r.date = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_DATE));
				r.time = cursor.getLong(cursor.getColumnIndex(Responses.RESPONSE_TIME));
				r.timezone = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_TIMEZONE));
				r.locationStatus = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_STATUS));
				if (! r.locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE)) {

					r.locationLatitude = cursor.getDouble(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_LATITUDE));
					r.locationLongitude = cursor.getDouble(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_LONGITUDE));
					r.locationProvider = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_PROVIDER));
					r.locationAccuracy = cursor.getFloat(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_ACCURACY));
					r.locationTime = cursor.getLong(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_TIME));
				}
				r.surveyId = cursor.getString(cursor.getColumnIndex(Responses.SURVEY_ID));
				r.surveyLaunchContext = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_SURVEY_LAUNCH_CONTEXT));
				r.response = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_JSON));
				r.status = cursor.getInt(cursor.getColumnIndex(Responses.RESPONSE_STATUS));
				responses.add(r);

				cursor.moveToNext();
			}

			cursor.close();

			return responses; 
		}

		public ContentValues toCV() {
			try {
				ContentValues values = new ContentValues();

				values.put(Responses.RESPONSE_UUID, uuid);
				values.put(Responses.CAMPAIGN_URN, campaignUrn);
				values.put(Responses.RESPONSE_USERNAME, username);
				values.put(Responses.RESPONSE_DATE, date);
				values.put(Responses.RESPONSE_TIME, time);
				values.put(Responses.RESPONSE_TIMEZONE, timezone);
				values.put(Responses.RESPONSE_LOCATION_STATUS, locationStatus);

				if (locationStatus != SurveyGeotagService.LOCATION_UNAVAILABLE)
				{
					values.put(Responses.RESPONSE_LOCATION_LATITUDE, locationLatitude);
					values.put(Responses.RESPONSE_LOCATION_LONGITUDE, locationLongitude);
					values.put(Responses.RESPONSE_LOCATION_PROVIDER, locationProvider);
					values.put(Responses.RESPONSE_LOCATION_ACCURACY, locationAccuracy);
				}

				values.put(Responses.RESPONSE_LOCATION_TIME, locationTime);
				values.put(Responses.SURVEY_ID, surveyId);
				values.put(Responses.RESPONSE_SURVEY_LAUNCH_CONTEXT, surveyLaunchContext);
				values.put(Responses.RESPONSE_JSON, response);
				values.put(Responses.RESPONSE_STATUS, status);

				String hashableData = campaignUrn + surveyId + username + date;
				String hashcode = DbHelper.getSHA1Hash(hashableData);
				values.put(Responses.RESPONSE_HASHCODE, hashcode);

				return values;
			}
			catch (NoSuchAlgorithmException e) {
				throw new UnsupportedOperationException("The SHA1 algorithm is not available, can't make a response CV", e);
			}
		}

		public static File getResponsesImageDir(Context context, String campaignUrn, String id) {
			File f = new File(Campaign.getCampaignImageDir(context, campaignUrn), id);
			f.mkdirs();
			return f;
		}

		/**
		 * Returns the image for a given uuid for this response id and campaign. Usually it is in the response dir
		 * but it also checks the old locations and moves them if appropriate
		 * @param context
		 * @param campaignUrn
		 * @param id
		 * @param uuid
		 * @return the image file
		 */
		public static File getResponsesImage(Context context, String campaignUrn, String id, final String uuid) {
			File image = new File(Response.getResponsesImageDir(context, campaignUrn, id), uuid);

			// If the image isnt there for some reason, maybe its in the old location under the campaign urn
			if(image == null || !image.exists()) {
				File jpgImage = new File(Campaign.getCampaignImageDir(context, campaignUrn), uuid + ".jpg");
				if(jpgImage != null && jpgImage.exists())
					jpgImage.renameTo(image);
				else {
					File pngImage = new File(Campaign.getCampaignImageDir(context, campaignUrn), uuid + ".png");
					if(pngImage != null && pngImage.exists())
						pngImage.renameTo(image);
				}
			}
			return image;
		}

		private File getResponseImageDir(Context context) {
			return getResponsesImageDir(context, campaignUrn, String.valueOf(_id));
		}

		@Override
		public void cleanUp(Context context) {
			try {
				if(getResponseImageDir(context).exists())
					Utilities.delete(getResponseImageDir(context));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public final static class PromptResponse extends DbModel {

		public long mResponseID;
		public String mCompositeID;
		public String mPromptID;
		public String mValue;
		public String mExtraValue;
		
        /**
         * Returns a list of PromptResponse objects from the given cursor.
         * 
         * @param cursor a cursor containing the fields specified in the PromptResponse schema, which is closed when this method returns.
         * @return a List of PromptResponse objects
         */
        public static List<PromptResponse> fromCursor(Cursor cursor) {
    		
    		ArrayList<PromptResponse> prompts = new ArrayList<PromptResponse>();
    		
    		cursor.moveToFirst();
    		
    		for (int i = 0; i < cursor.getCount(); i++) {
    			
    			PromptResponse temp = new PromptResponse();
    			temp._id = cursor.getLong(cursor.getColumnIndex(PromptResponses._ID));
    			temp.mResponseID = cursor.getLong(cursor.getColumnIndex(PromptResponses.RESPONSE_ID));
    			temp.mCompositeID = cursor.getString(cursor.getColumnIndex(PromptResponses.COMPOSITE_ID));
    			temp.mPromptID = cursor.getString(cursor.getColumnIndex(PromptResponses.PROMPT_ID));
    			temp.mValue = cursor.getString(cursor.getColumnIndex(PromptResponses.PROMPT_RESPONSE_VALUE));
    			temp.mExtraValue = cursor.getString(cursor.getColumnIndex(PromptResponses.PROMPT_RESPONSE_EXTRA_VALUE));
    			prompts.add(temp);
    			
    			cursor.moveToNext();
    		}
    		
    		cursor.close();
    		
    		return prompts; 
    	}
        
        public ContentValues toCV() {
        	ContentValues values = new ContentValues();
        	
        	values.put(PromptResponses.RESPONSE_ID, mResponseID);
        	values.put(PromptResponses.COMPOSITE_ID, mCompositeID);
        	values.put(PromptResponses.PROMPT_ID, mPromptID);
        	values.put(PromptResponses.PROMPT_RESPONSE_VALUE, mValue);
        	values.put(PromptResponses.PROMPT_RESPONSE_EXTRA_VALUE, mExtraValue);
        	
        	return values;
        }
	}
}
