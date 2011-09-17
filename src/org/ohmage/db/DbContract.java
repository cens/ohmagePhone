package org.ohmage.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.ohmage.db.DbHelper.Tables;
import org.ohmage.service.SurveyGeotagService;
import org.ohmage.service.UploadService;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for interacting with {@link DbProvider}. Defines the kinds of entities
 * managed by the provider, their schemas, and their relationships.
 * @author faisal
 *
 */
public class DbContract {
	public static final String CONTENT_AUTHORITY = "org.ohmage.db";
	private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
	
	public static Uri getBaseUri() {
		return BASE_CONTENT_URI;
	}
	
	/**
	 * Represents a campaign.
	 */
	public static final class Campaign implements BaseColumns {
		public static final String URN = "urn";
		public static final String NAME = "name";
		public static final String DESCRIPTION = "description";
		public static final String CREATION_TIMESTAMP = "creationTimestamp";
		public static final String DOWNLOAD_TIMESTAMP = "downloadTimestamp";
		public static final String CONFIGURATION_XML = "configuration_xml";
		public static final String STATUS = "status";
		public static final String ICON = "icon";
		
		public static final int STATUS_READY = 0;
		public static final int STATUS_REMOTE = 1;
		public static final int STATUS_STOPPED = 2;
		public static final int STATUS_OUT_OF_DATE = 3;
		public static final int STATUS_INVALID_USER_ROLE = 4;
		public static final int STATUS_DELETED = 5;
		public static final int STATUS_VAGUE = 6;
		public static final int STATUS_DOWNLOADING = 7;

		// data fields here to support use of the Campaign class as a data holder (and not just a schema definer)
		// this should be reconciled by some kind of real ORM someday
		public long _id;
		public String mUrn;
		public String mName;
		public String mDescription;
		public String mCreationTimestamp;
		public String mDownloadTimestamp;
		public String mXml;
		public int mStatus;
		public String mIcon;
		
        public static final Uri CONTENT_URI =
        	BASE_CONTENT_URI.buildUpon().appendPath("campaigns").build();
        public static final String CONTENT_TYPE =
        	"vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".campaign";
        public static final String CONTENT_ITEM_TYPE =
        	"vnd.android.cursor.item/vnd." + CONTENT_AUTHORITY + ".campaign";
       
        public static Uri getCampaigns() {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("campaigns")
				.build();
        }
        
        public static Uri getCampaignByURN(String campaignUrn) {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("campaigns")
				.appendPath(campaignUrn)
				.build();
        }
        
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
    			c._id = cursor.getLong(cursor.getColumnIndex(Campaign._ID));
    			c.mUrn = cursor.getString(cursor.getColumnIndex(Campaign.URN));
    			c.mName = cursor.getString(cursor.getColumnIndex(Campaign.NAME));
    			c.mDescription = cursor.getString(cursor.getColumnIndex(Campaign.DESCRIPTION));
    			c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.CREATION_TIMESTAMP));
    			c.mDownloadTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.DOWNLOAD_TIMESTAMP));
    			c.mXml = cursor.getString(cursor.getColumnIndex(Campaign.CONFIGURATION_XML));
    			c.mStatus = cursor.getInt(cursor.getColumnIndex(Campaign.STATUS));
    			c.mIcon = cursor.getString(cursor.getColumnIndex(Campaign.ICON));
    			campaigns.add(c);
    			
    			cursor.moveToNext();
    		}
    		
    		cursor.close();
    		
    		return campaigns;
        }
        
        public ContentValues toCV() {
        	ContentValues values = new ContentValues();
        	
    		values.put(Campaign.URN, mUrn);
    		values.put(Campaign.NAME, mName);
    		values.put(Campaign.DESCRIPTION, mDescription);
    		values.put(Campaign.CREATION_TIMESTAMP, mCreationTimestamp);
    		values.put(Campaign.DOWNLOAD_TIMESTAMP, mDownloadTimestamp);
    		values.put(Campaign.CONFIGURATION_XML, mXml);
    		values.put(Campaign.STATUS, mStatus);
    		values.put(Campaign.ICON, mIcon);
        	return values;
        }
	}
	
	/**
	 * Represents a survey, as extracted from the campaign XML.
	 */
	public static final class Survey implements BaseColumns {
		public static final String SURVEY_ID = "survey_id";
		public static final String CAMPAIGN_URN = "campaign_urn";
		public static final String TITLE = "title";
		public static final String DESCRIPTION = "description";
		public static final String SUMMARY = "summary";

		// data fields here to support use of the Survey class as a data holder (and not just a schema definer)
		// this should be reconciled by some kind of real ORM someday
		public long _id;
		public String mSurveyID;
		public String mCampaignUrn;
		public String mTitle;
		public String mDescription;
		public String mSummary;
		
        public static final Uri CONTENT_URI =
        	BASE_CONTENT_URI.buildUpon().appendPath("surveys").build();
        public static final String CONTENT_TYPE =
        	"vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".survey";
        public static final String CONTENT_ITEM_TYPE =
        	"vnd.android.cursor.item/vnd." + CONTENT_AUTHORITY + ".survey";
       
        public static Uri getSurveys() {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("surveys")
				.build();
        }

        public static Uri getSurveyByID(String campaignUrn, String surveyId) {
    		return BASE_CONTENT_URI.buildUpon()
    			.appendPath("campaigns")
    			.appendPath(campaignUrn)
				.appendPath("surveys")
				.appendPath(surveyId)
				.build();
        }
        
        public static Uri getSurveysByCampaignURN(String campaignUrn) {
    		return BASE_CONTENT_URI.buildUpon()
    			.appendPath("campaigns")
    			.appendPath(campaignUrn)
				.appendPath("surveys")
				.build();
        }
        
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
    			s._id = cursor.getLong(cursor.getColumnIndex(Survey._ID));
    			s.mSurveyID = cursor.getString(cursor.getColumnIndex(Survey.SURVEY_ID));
    			s.mCampaignUrn = cursor.getString(cursor.getColumnIndex(Survey.CAMPAIGN_URN));
    			s.mTitle = cursor.getString(cursor.getColumnIndex(Survey.TITLE));
    			s.mDescription = cursor.getString(cursor.getColumnIndex(Survey.DESCRIPTION));
    			s.mSummary = cursor.getString(cursor.getColumnIndex(Survey.SUMMARY));
    			surveys.add(s);
    			
    			cursor.moveToNext();
    		}
    		
    		cursor.close();
    		
    		return surveys;
        }
        
        public ContentValues toCV() {
        	ContentValues values = new ContentValues();
        	
        	values.put(Survey.SURVEY_ID, mSurveyID);
        	values.put(Survey.CAMPAIGN_URN, mCampaignUrn);
        	values.put(Survey.TITLE, mTitle);
        	values.put(Survey.DESCRIPTION, mDescription);
        	values.put(Survey.SUMMARY, mSummary);
    		
        	return values;
        }
	}
	
	/**
	 * Represents a prompt within a survey, again as extracted from the campaign XML.
	 */
	public static final class SurveyPrompt implements BaseColumns {
		public static final String SURVEY_PID = "survey_pid";
		public static final String SURVEY_ID = "survey_id";
		public static final String COMPOSITE_ID = "composite_id";
		public static final String PROMPT_ID = "prompt_id";
		public static final String PROMPT_TEXT = "prompt_text";
		public static final String PROMPT_TYPE = "prompt_type";

		// data fields here to support use of the Survey class as a data holder (and not just a schema definer)
		// this should be reconciled by some kind of real ORM someday
		public long _id;
		public long mSurveyPID;
		public String mSurveyID;
		public String mCompositeID;
		public String mPromptID;
		public String mPromptText;
		public String mPromptType;
		
        public static final Uri CONTENT_URI =
        	BASE_CONTENT_URI.buildUpon().appendPath("surveys").build();
        public static final String CONTENT_TYPE =
        	"vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".surveyprompt";
        public static final String CONTENT_ITEM_TYPE =
        	"vnd.android.cursor.item/vnd." + CONTENT_AUTHORITY + ".surveyprompt";
        
        public static Uri getSurveyPrompts() {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("surveys")
				.appendPath("prompts")
				.build();
        }
        
        public static Uri getSurveyPromptsByCampaignAndSurveyID(String campaignUrn, String surveyID) {
    		return BASE_CONTENT_URI.buildUpon()
    			.appendPath("campaigns")
    			.appendPath(campaignUrn)
				.appendPath("surveys")
				.appendPath(surveyID)
				.appendPath("prompts")
				.build();
        }

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
    			temp._id = cursor.getLong(cursor.getColumnIndex(SurveyPrompt._ID));
    			temp.mSurveyPID = cursor.getLong(cursor.getColumnIndex(SurveyPrompt.SURVEY_PID));
    			temp.mSurveyID = cursor.getString(cursor.getColumnIndex(SurveyPrompt.SURVEY_ID));
    			temp.mCompositeID = cursor.getString(cursor.getColumnIndex(SurveyPrompt.COMPOSITE_ID));
    			temp.mPromptID = cursor.getString(cursor.getColumnIndex(SurveyPrompt.PROMPT_ID));
    			temp.mPromptText = cursor.getString(cursor.getColumnIndex(SurveyPrompt.PROMPT_TEXT));
    			temp.mPromptType = cursor.getString(cursor.getColumnIndex(SurveyPrompt.PROMPT_TYPE));
    			surveyprompts.add(temp);
    			
    			cursor.moveToNext();
    		}
    		
    		cursor.close();
    		
    		return surveyprompts;
        }
        
        public ContentValues toCV() {
        	ContentValues values = new ContentValues();
        	
        	values.put(SurveyPrompt.SURVEY_PID, mSurveyPID);
        	values.put(SurveyPrompt.SURVEY_ID, mSurveyID);
        	values.put(SurveyPrompt.COMPOSITE_ID, mCompositeID);
        	values.put(SurveyPrompt.PROMPT_ID, mPromptID);
        	values.put(SurveyPrompt.PROMPT_TEXT, mPromptText);
        	values.put(SurveyPrompt.PROMPT_TYPE, mPromptType);
    		
        	return values;
        }
	}
	
	/**
	 * Represents a survey response.
	 * 
	 * The 'source' field indicates how the survey response was added to the database. If it's "local", that
	 * means that the survey was filled out by the user and is awaiting upload. If it's "remote", it means
	 * that it was downloaded by the feedback sync service. Both "local" and "remote" surveys are used for
	 * generating feedback visualizations.
	 *
	 * The 'uploaded' field indicates whether or not the survey has been uploaded by the {@link UploadService}
	 * yet. This field is valid only for records where 'source' is "local".
	 */
	public static final class Response implements BaseColumns {
		public static final String CAMPAIGN_URN = "campaign_urn";
		public static final String USERNAME = "username";
		public static final String DATE = "date";
		public static final String TIME = "time";
		public static final String TIMEZONE = "timezone";
		public static final String LOCATION_STATUS ="location_status";
		public static final String LOCATION_LATITUDE = "location_latitude";
		public static final String LOCATION_LONGITUDE = "location_longitude";
		public static final String LOCATION_PROVIDER = "location_provider";
		public static final String LOCATION_ACCURACY = "location_accuracy";
		public static final String LOCATION_TIME = "location_time";
		public static final String SURVEY_ID = "survey_id";
		public static final String SURVEY_LAUNCH_CONTEXT = "survey_launch_context";
		public static final String RESPONSE = "response";
		public static final String UPLOADED = "uploaded";
		public static final String SOURCE = "source";
		public static final String HASHCODE = "hashcode";

		// data fields here to support use of the Response class as a data holder (and not just a schema definer)
		// this should be reconciled by some kind of real ORM someday
		public long _id;
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
		public String surveyId;
		public String surveyLaunchContext;
		public String response;
		public int uploaded;
		public String source;
		public String hashcode;
		
        public static final Uri CONTENT_URI =
        	BASE_CONTENT_URI.buildUpon().appendPath("responses").build();
        public static final String CONTENT_TYPE =
        	"vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".response";
        public static final String CONTENT_ITEM_TYPE =
        	"vnd.android.cursor.item/vnd." + CONTENT_AUTHORITY + ".response";
        
        public static Uri getResponses() {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("responses")
				.build();
        }
        
        public static Uri getResponseByID(long responseID) {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("responses")
				.appendPath(Long.toString(responseID))
				.build();
        }
        
        public static Uri getResponsesByCampaign(String campaignUrn) {
    		return BASE_CONTENT_URI.buildUpon()
    			.appendPath("campaigns")
				.appendPath(campaignUrn)
				.appendPath("responses")
				.build();
        }
        
        public static Uri getResponsesByCampaignAndSurvey(String campaignUrn, String surveyID) {
    		return BASE_CONTENT_URI.buildUpon()
    			.appendPath("campaigns")
				.appendPath(campaignUrn)
				.appendPath("surveys")
				.appendPath(surveyID)
				.appendPath("responses")
				.build();
        }

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
    			r._id = cursor.getLong(cursor.getColumnIndex(Response._ID));
    			r.campaignUrn = cursor.getString(cursor.getColumnIndex(Response.CAMPAIGN_URN));
    			r.username = cursor.getString(cursor.getColumnIndex(Response.USERNAME));
    			r.date = cursor.getString(cursor.getColumnIndex(Response.DATE));
    			r.time = cursor.getLong(cursor.getColumnIndex(Response.TIME));
    			r.timezone = cursor.getString(cursor.getColumnIndex(Response.TIMEZONE));
    			r.locationStatus = cursor.getString(cursor.getColumnIndex(Response.LOCATION_STATUS));
    			if (! r.locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE)) {
    				
    				r.locationLatitude = cursor.getDouble(cursor.getColumnIndex(Response.LOCATION_LATITUDE));
    				r.locationLongitude = cursor.getDouble(cursor.getColumnIndex(Response.LOCATION_LONGITUDE));
    				r.locationProvider = cursor.getString(cursor.getColumnIndex(Response.LOCATION_PROVIDER));
    				r.locationAccuracy = cursor.getFloat(cursor.getColumnIndex(Response.LOCATION_ACCURACY));
    				r.locationTime = cursor.getLong(cursor.getColumnIndex(Response.LOCATION_TIME));
    			}
    			r.surveyId = cursor.getString(cursor.getColumnIndex(Response.SURVEY_ID));
    			r.surveyLaunchContext = cursor.getString(cursor.getColumnIndex(Response.SURVEY_LAUNCH_CONTEXT));
    			r.response = cursor.getString(cursor.getColumnIndex(Response.RESPONSE));
    			r.source = cursor.getString(cursor.getColumnIndex(Response.SOURCE));
    			r.uploaded = cursor.getInt(cursor.getColumnIndex(Response.UPLOADED));
    			responses.add(r);
    			
    			cursor.moveToNext();
    		}
    		
    		cursor.close();
    		
    		return responses; 
    	}
    	
        public ContentValues toCV() {
        	try {
            	ContentValues values = new ContentValues();

    			values.put(Response.CAMPAIGN_URN, campaignUrn);
    			values.put(Response.USERNAME, username);
    			values.put(Response.DATE, date);
    			values.put(Response.TIME, time);
    			values.put(Response.TIMEZONE, timezone);
    			values.put(Response.LOCATION_STATUS, locationStatus);
    			
    			if (locationStatus != SurveyGeotagService.LOCATION_UNAVAILABLE)
    			{
    				values.put(Response.LOCATION_LATITUDE, locationLatitude);
    				values.put(Response.LOCATION_LONGITUDE, locationLongitude);
    				values.put(Response.LOCATION_PROVIDER, locationProvider);
    				values.put(Response.LOCATION_ACCURACY, locationAccuracy);
    			}
    			
    			values.put(Response.LOCATION_TIME, locationTime);
    			values.put(Response.SURVEY_ID, surveyId);
    			values.put(Response.SURVEY_LAUNCH_CONTEXT, surveyLaunchContext);
    			values.put(Response.RESPONSE, response);
    			values.put(Response.SOURCE, source);
    			
    			String hashableData = campaignUrn + surveyId + username + date;
    			String hashcode = DbHelper.getSHA1Hash(hashableData);
    			values.put(Response.HASHCODE, hashcode);
    			
            	return values;
        	}
        	catch (NoSuchAlgorithmException e) {
        		throw new UnsupportedOperationException("The SHA1 algorithm is not available, can't make a response CV", e);
        	}
        }
	}
	
	// ===================================
	// === feedback prompt responses schema
	// ===================================
	
	/**
	 * Represents a single response to a prompt.
	 * 
	 * These are extracted from the survey response json at the time of survey completion,
	 * or in FeedbackService from the downloaded response data.
	 */
	public static final class PromptResponse implements BaseColumns
	{
		public static final String RESPONSE_ID = "response_id";
		public static final String COMPOSITE_ID = "composite_id";
		public static final String PROMPT_ID = "prompt_id";
		public static final String PROMPT_VALUE = "prompt_value";
		
		// data fields here to support use of the PromptResponse class as a data holder (and not just a schema definer)
		// this should be reconciled by some kind of real ORM someday
		public long _id;
		public long mResponseID;
		public String mCompositeID;
		public String mPromptID;
		public String mValue;
		
        public static final Uri CONTENT_URI =
        	BASE_CONTENT_URI.buildUpon().appendPath("prompts").build();
        public static final String CONTENT_TYPE =
        	"vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".promptresponse";
        public static final String CONTENT_ITEM_TYPE =
        	"vnd.android.cursor.item/vnd." + CONTENT_AUTHORITY + ".promptresponse";
        
        public enum AggregateTypes {
        	AVG,
        	COUNT,
        	MAX,
        	MIN,
        	TOTAL
        }
        
        public static Uri getPromptUri(long insertID) {
        	return CONTENT_URI.buildUpon().appendPath(Long.toString(insertID)).build();
        }
        
        public static Uri getPrompts() {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("prompts")
				.build();
        }
        
        public static Uri getPromptsByResponseID(long responseID) {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("responses")
				.appendPath(Long.toString(responseID))
				.appendPath("prompts")
				.build();
        }
        
        public static Uri getPromptsByCampaignAndSurvey(String campaignUrn, String surveyID, String promptID) {
    		return BASE_CONTENT_URI.buildUpon()
    			.appendPath("campaigns")
				.appendPath(campaignUrn)
				.appendPath("surveys")
				.appendPath(surveyID)
				.appendPath("responses")
				.appendPath("prompts")
				.appendPath(promptID)
				.build();
        }
        
        public static Uri getPromptsByCampaignAndSurvey(String campaignUrn, String surveyID, String promptID, AggregateTypes aggregate) {
    		return BASE_CONTENT_URI.buildUpon()
    			.appendPath("campaigns")
				.appendPath(campaignUrn)
				.appendPath("surveys")
				.appendPath(surveyID)
				.appendPath("responses")
				.appendPath("prompts")
				.appendPath(promptID)
				.appendPath(aggregate.toString())
				.build();
        }
        
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
    			temp._id = cursor.getLong(cursor.getColumnIndex(PromptResponse._ID));
    			temp.mResponseID = cursor.getLong(cursor.getColumnIndex(PromptResponse.RESPONSE_ID));
    			temp.mCompositeID = cursor.getString(cursor.getColumnIndex(PromptResponse.COMPOSITE_ID));
    			temp.mPromptID = cursor.getString(cursor.getColumnIndex(PromptResponse.PROMPT_ID));
    			temp.mValue = cursor.getString(cursor.getColumnIndex(PromptResponse.PROMPT_VALUE));
    			prompts.add(temp);
    			
    			cursor.moveToNext();
    		}
    		
    		cursor.close();
    		
    		return prompts; 
    	}
        
        public ContentValues toCV() {
        	ContentValues values = new ContentValues();
        	
        	values.put(PromptResponse.RESPONSE_ID, mResponseID);
        	values.put(PromptResponse.COMPOSITE_ID, mCompositeID);
        	values.put(PromptResponse.PROMPT_ID, mPromptID);
        	values.put(PromptResponse.PROMPT_VALUE, mValue);
        	
        	return values;
        }
	}
}
