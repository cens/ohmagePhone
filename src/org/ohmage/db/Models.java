package org.ohmage.db;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.ohmage.CampaignPreferencesHelper;
import org.ohmage.OhmageApplication;
import org.ohmage.OhmageCache;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.SurveyPrompts;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.DbProvider.Qualified;
import org.ohmage.db.utils.SelectionBuilder;
import org.ohmage.prompt.multichoicecustom.MultiChoiceCustomDbAdapter;
import org.ohmage.prompt.singlechoicecustom.SingleChoiceCustomDbAdapter;
import org.ohmage.service.SurveyGeotagService;
import org.ohmage.triggers.glue.TriggerFramework;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
		public long updated;

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
				c.updated = cursor.getLong(cursor.getColumnIndexOrThrow(Campaigns.CAMPAIGN_UPDATED));
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

		/**
		 * Launch the Trigger list for this campaign
		 * @param context
		 * @param campaignUrn
		 */
		public static Intent launchTriggerIntent(Context context, String campaignUrn) {
			List<String> surveyTitles = new ArrayList<String>();
			
			// grab a list of surveys for this campaign
			Cursor surveys = context.getContentResolver().query(Campaigns.buildSurveysUri(campaignUrn), null, null, null, null);
			
			while (surveys.moveToNext()) {
				surveyTitles.add(surveys.getString(surveys.getColumnIndex(Surveys.SURVEY_TITLE)));
			}
			surveys.close();
			
			return TriggerFramework.launchTriggersIntent(context, campaignUrn, surveyTitles.toArray(new String[surveyTitles.size()]));
		}

		/**
		 * Returns the uri of the first ready campaign in the db which should be the campaign used in single campaign mode
		 * @param context
		 * @return the urn of the first ready campaign from the db, or null
		 */
		public static String getSingleCampaign(Context context) {
			Cursor campaign = context.getContentResolver().query(Campaigns.CONTENT_URI, new String[] { Campaigns.CAMPAIGN_URN },
					Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY, null, Campaigns.CAMPAIGN_CREATED + " DESC");
			String campaignUrn = null;
			if(campaign.moveToFirst())
				campaignUrn = campaign.getString(0);
			campaign.close();
			return campaignUrn;
		}

		/**
		 * Returns the first campaign in the db which should be the campaign used in single campaign mode.
		 * @param context
		 * @return the first campaign from the db, or null
		 */
		public static Campaign getFirstAvaliableCampaign(Context context) {
			Cursor campaign = context.getContentResolver().query(Campaigns.CONTENT_URI, new String[] { Campaigns.CAMPAIGN_URN, Campaigns.CAMPAIGN_STATUS },
					Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_REMOTE + " OR " +
					Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY + " OR " +
					Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_OUT_OF_DATE, null, Campaigns.CAMPAIGN_CREATED + " DESC");
			Campaign c = null;
			if(campaign.moveToFirst()) {
				c = new Campaign();
				c.mUrn = campaign.getString(0);
				c.mStatus = campaign.getInt(1);
			}
			campaign.close();
			return c;
		}

		/**
		 * Sets the campaign to {@link Campaign#STATUS_REMOTE}. Also removes surveys and responses.
		 * @param context
		 * @param campaignUrn
		 */
		public static void setRemote(Context context, String... campaignUrns) {
			SelectionBuilder builder = new SelectionBuilder();
			for(String c : campaignUrns)
				builder.where(Campaigns.CAMPAIGN_URN + "=?", SelectionBuilder.OR, c);

			setRemote(context, builder);
		}

		/**
		 * Makes sure that we only have one campaign if we can
		 * @param context
		 */
		public static void ensureSingleCampaign(Context context) {
			String campaignUrn = getSingleCampaign(context);

			if(campaignUrn != null) {
				SelectionBuilder builder = new SelectionBuilder();
				builder.where(Campaigns.CAMPAIGN_URN + "!=?", campaignUrn);
				setRemote(context, builder);
			}
		}

		private static void setRemote(Context context, SelectionBuilder builder) {
			// Query for all campaigns
			Cursor cursor = context.getContentResolver().query(Campaigns.CONTENT_URI, null, builder.getSelection(), builder.getSelectionArgs(), null);
			List<Campaign> campaigns = fromCursor(cursor);

			if(campaigns.size() > 0) {
				// Update campaigns to be remote
				ContentValues cv = new ContentValues();
				cv.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_REMOTE);
				cv.put(Campaigns.CAMPAIGN_CONFIGURATION_XML, "");
				cv.put(Campaigns.CAMPAIGN_UPDATED, System.currentTimeMillis());
				context.getContentResolver().update(Campaigns.CONTENT_URI, cv, builder.getSelection(), builder.getSelectionArgs());

				// Delete responses
				context.getContentResolver().delete(Responses.CONTENT_URI, builder.getSelection(), builder.getSelectionArgs());

				// Clean up after campaigns
				for(Campaign c : campaigns)
					c.cleanUp(context);
			}
		}

		@Override
		public void cleanUp(Context context) {
			if (mStatus != Campaign.STATUS_REMOTE)
				TriggerFramework.resetTriggerSettings(context, mUrn);

			try {
				if(!TextUtils.isEmpty(mIcon))
					OhmageCache.getCachedFile(context, new URI(mIcon)).delete();
			}catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Clear custom choices
			MultiChoiceCustomDbAdapter customMultiChoices = new MultiChoiceCustomDbAdapter(context);
			if(customMultiChoices.open()) {
				customMultiChoices.clearCampaign(mUrn);
				customMultiChoices.close();
			}
			SingleChoiceCustomDbAdapter customSingleChoices = new SingleChoiceCustomDbAdapter(context);
			if(customSingleChoices.open()) {
				customSingleChoices.clearCampaign(mUrn);
				customSingleChoices.close();
			}

			CampaignPreferencesHelper.clearAll(context, mUrn);
		}

		/**
		 * Retuns the campaign xml from the db
		 * @param context
		 * @param campaignUrn
		 * @return
		 * @throws IOException
		 */
		public static InputStream loadCampaignXml(Context context, String campaignUrn) throws IOException {
			ContentResolver cr = context.getContentResolver();
			Cursor cursor = cr.query(Campaigns.buildCampaignUri(campaignUrn), new String[] { Campaigns.CAMPAIGN_CONFIGURATION_XML }, null, null, null);

			// ensure that only one record is returned
			if (cursor.moveToFirst() && cursor.getCount() == 1) {
				String xml = cursor.getString(0);
				cursor.close();
				return new ByteArrayInputStream(xml.getBytes("UTF-8"));
			} else {
				cursor.close();
				return null;
			}
		}

		/**
		 * Counts the number of local responses for the given urn
		 * @param context
		 * @param campaignUrn
		 * @return
		 */
		public static int localResponseCount(Context context, String campaignUrn) {
			return localResponseCount(context, Campaigns.buildResponsesUri(campaignUrn));
		}

		/**
		 * Counts the number of local responses on the phone
		 * @param context
		 * @return
		 */
		public static int localResponseCount(Context context) {
			return localResponseCount(context, Responses.CONTENT_URI);
		}

		/**
		 * Helper method to consistently calculate the number of local responses
		 * @param context
		 * @param uri
		 * @return
		 */
		private static int localResponseCount(Context context, Uri uri) {
			Cursor localResponses = context.getContentResolver().query(uri, new String[] { Responses._ID },
					Responses.RESPONSE_STATUS + "!=" + Response.STATUS_DOWNLOADED + " AND " + Responses.RESPONSE_STATUS + "!=" + Response.STATUS_UPLOADED, null, null);
			int count = localResponses.getCount();
			localResponses.close();
			return count;
		}

		/**
		 * Returns the time of the last downloaded response for this campaign. We should be able to sync all responses newer than it
		 * @param responseSyncService
		 * @return the last downloaded response time or 0 if there are none;
		 */
		public long getLastDownloadedResponseTime(Context context) {
			Cursor c = context.getContentResolver().query(
					Responses.CONTENT_URI, new String[] { Responses.RESPONSE_TIME },
					Responses.RESPONSE_STATUS + "=" + Response.STATUS_DOWNLOADED + " AND "
							+ Qualified.RESPONSES_CAMPAIGN_URN + "=?", new String[] { mUrn },
							Responses.RESPONSE_TIME + " DESC");
			long time = 0;
			if(c.moveToFirst()) {
				time = c.getLong(0);
			}
			c.close();
			return time;
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
        	values.put(Surveys.SURVEY_INTRO_TEXT, mIntroText);
        	values.put(Surveys.SURVEY_ANYTIME, mAnytime);
        	values.put(Surveys.SURVEY_STATUS, mStatus);
        	
        	return values;
        }
        
		/**
		 * Launch the Trigger list for the campaign to which this survey belongs with a list of surveys selected by default.
		 * @param context
		 * @param campaignUrn the campaign URN from which to read the list of surveys that will be selectable from the list
		 * @param selectedSurveys an array of surveys which will be preselected when creating a new trigger
		 */
		public static Intent launchTriggerIntent(Context context, String campaignUrn, String[] selectedSurveys) {
			List<String> surveyTitles = new ArrayList<String>();
			
			// grab a list of surveys for this campaign
			Cursor surveys = context.getContentResolver().query(Campaigns.buildSurveysUri(campaignUrn), null, null, null, null);
			
			while (surveys.moveToNext()) {
				surveyTitles.add(surveys.getString(surveys.getColumnIndex(Surveys.SURVEY_TITLE)));
			}
			surveys.close();
			
			return TriggerFramework.launchTriggersIntent(context, campaignUrn, surveyTitles.toArray(new String[surveyTitles.size()]), selectedSurveys);
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

			return values;
		}

		/**
		 * Returns the file for given image uuid
		 * @param uuid
		 * @return
		 */
		public static File getTemporaryResponsesMedia(String uuid) {
			return new File(getResponseMediaUploadDir(), uuid);
		}

		/**
		 * Returns the directory to store images to upload in
		 * @return
		 */
		public static File getResponseMediaUploadDir() {
			File dir = new File(OhmageApplication.getContext().getExternalCacheDir(), "uploads");
			dir.mkdirs();
			return dir;
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
