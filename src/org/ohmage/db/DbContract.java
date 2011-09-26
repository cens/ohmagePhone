package org.ohmage.db;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.ohmage.service.SurveyGeotagService;
import org.ohmage.service.OldUploadService;

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

    interface CampaignColumns {
        /** Unique string identifying this campaign. */
        String CAMPAIGN_URN = "campaign_urn";
        /** Name of this campaign. */
        String CAMPAIGN_NAME = "campaign_name";
        /** Description of this campaign. */
		String CAMPAIGN_DESCRIPTION = "campaign_description";
		/** Time when this campaign was created */
		String CAMPAIGN_CREATED = "campaign_created";
		/** Time when this campaign was downloaded */
		String CAMPAIGN_DOWNLOADED = "campaign_downloaded";
		/** Configuration xml for this campaign */
		String CAMPAIGN_CONFIGURATION_XML = "campaign_configuration_xml";
		/** Status of this campaign */
		String CAMPAIGN_STATUS = "campaign_status";
		/** Icon for this campaign */
		String CAMPAIGN_ICON = "campaign_icon";
		/** Privacy status of this campaign */
		String CAMPAIGN_PRIVACY = "campaign_privacy";
    }

    interface SurveyColumns {
    	/** Unique string identifying this survey. */
    	String SURVEY_ID = "survey_id";
    	/** Title of this survey. */
    	String SURVEY_TITLE = "survey_title";
    	/** Description of this survey. */
    	String SURVEY_DESCRIPTION = "survey_description";
    	String SURVEY_SUBMIT_TEXT = "survey_submit_text";
    	String SURVEY_SHOW_SUMMARY = "survey_show_summary";
    	String SURVEY_EDIT_SUMMARY = "survey_edit_summary";
    	String SURVEY_SUMMARY_TEXT = "survey_summary_text";
    	String SURVEY_INTRO_TEXT = "survey_intro_text";
    	String SURVEY_ANYTIME = "survey_anytime";
    	String SURVEY_STATUS = "survey_status";
    }
    
    interface SurveyPromptColumns {
		String SURVEY_PROMPT_TEXT = "survey_prompt_text";
		String SURVEY_PROMPT_TYPE = "survey_prompt_type";
		String SURVEY_PROMPT_PROPERTIES = "survey_prompt_properties";
    }

    private static final String PATH_CAMPAIGNS = "campaigns";
    private static final String PATH_SURVEYS = "surveys";
    private static final String PATH_PROMPTS = "prompts";
    
	/**
	 * Represents a campaign.
	 */
    public static final class Campaigns implements BaseColumns, CampaignColumns {

    	public static final Uri CONTENT_URI =
    			BASE_CONTENT_URI.buildUpon().appendPath(PATH_CAMPAIGNS).build();
    	public static final String CONTENT_TYPE =
    			"vnd.android.cursor.dir/vnd.ohmage.campaign";
    	public static final String CONTENT_ITEM_TYPE =
    			"vnd.android.cursor.item/vnd.ohmage.campaign";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = CampaignColumns.CAMPAIGN_NAME;

        /** Build {@link Uri} for requested {@link #CAMPAIGN_URN}  */
        public static Uri buildCampaignUri(String campaignUrn) {
            return CONTENT_URI.buildUpon().appendPath(campaignUrn).build();
        }

        /**
         * Build {@link Uri} that references any {@link Surveys} associated
         * with the requested {@link #CAMPAIGN_URN}.
         */
        public static Uri buildSurveysUri(String campaignUrn) {
            return CONTENT_URI.buildUpon().appendPath(campaignUrn).appendPath(PATH_SURVEYS).build();
        }
        
        /**
         * Build {@link Uri} that references any {@link Surveys} associated
         * with the requested {@link #CAMPAIGN_URN} and {@link Surveys#SURVEY_ID}
         */
        public static Uri buildSurveysUri(String campaignUrn, String surveyId) {
            return buildSurveysUri(campaignUrn).buildUpon().appendPath(surveyId).build();
        }
        
        /**
         * Build {@link Uri} that references any {@link SurveyPrompts} associated
         * with the requested {@link #CAMPAIGN_URN} and {@link Surveys#SURVEY_ID}
         */
        public static Uri buildSurveyPromptsUri(String campaignUrn, String surveyId) {
            return buildSurveysUri(campaignUrn, surveyId).buildUpon().appendPath(PATH_SURVEYS).build();
        }
        
        /** Read {@link #CAMPAIGN_URN} from {@link Campaigns} {@link Uri}. */
        public static String getCampaignUrn(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    /**
     * Represents a survey, as extracted from the campaign XML.
     */
    public static final class Surveys implements BaseColumns, SurveyColumns {

    	public static final Uri CONTENT_URI =
    			BASE_CONTENT_URI.buildUpon().appendPath(PATH_SURVEYS).build();
    	public static final String CONTENT_TYPE =
    			"vnd.android.cursor.dir/vnd.ohmage.survey";
    	public static final String CONTENT_ITEM_TYPE =
    			"vnd.android.cursor.item/vnd.ohmage.survey";

    	public static final String CAMPAIGN_URN = "campaign_urn";

    	/** Default "ORDER BY" clause. */
    	public static final String DEFAULT_SORT = SurveyColumns.SURVEY_TITLE;

    	/** Build {@link Uri} for all {@link SurveyPrompts}  */
    	public static Uri buildSurveyPromptsUri() {
    		return CONTENT_URI.buildUpon().appendPath(PATH_PROMPTS).build();
    	}

    	/** Read {@link #SURVEY_ID} from {@link Surveys} {@link Uri}. */
    	public static String getSurveyId(Uri uri) {
    		return uri.getPathSegments().get(3);
    	}
    }

    /**
	 * Represents a prompt within a survey, again as extracted from the campaign XML.
	 */
	public static final class SurveyPrompts implements BaseColumns, SurveyPromptColumns {
		
        public static final Uri CONTENT_URI =
        	BASE_CONTENT_URI.buildUpon().appendPath(PATH_SURVEYS).build();
        public static final String CONTENT_TYPE =
        	"vnd.android.cursor.dir/vnd.ohmage.surveyprompt";
        public static final String CONTENT_ITEM_TYPE =
        	"vnd.android.cursor.item/vnd.ohmage.surveyprompt";
        
        public static final String SURVEY_PID = "survey_pid";
        public static final String SURVEY_ID = "survey_id";
        public static final String PROMPT_ID = "prompt_id";
        public static final String COMPOSITE_ID = "composite_id";
	}
	
	/**
	 * Represents a survey response.
	 * 
	 * The 'source' field indicates how the survey response was added to the database. If it's "local", that
	 * means that the survey was filled out by the user and is awaiting upload. If it's "remote", it means
	 * that it was downloaded by the feedback sync service. Both "local" and "remote" surveys are used for
	 * generating feedback visualizations.
	 *
	 * The 'uploaded' field indicates whether or not the survey has been uploaded by the {@link OldUploadService}
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
		public static final String STATUS = "status";
		public static final String HASHCODE = "hashcode";
		
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

		public long _id;
		/** the campaign URN for which to record the survey response */
		public String campaignUrn;
		/** the username to whom the survey response belongs */
		public String username;
		/** the date on which the survey response was recorded, assumedly in UTC */
		public String date;
		/** milliseconds since the epoch when this survey response was completed */
		public long time;
		/** the timezone in which the survey response was completed */
		public String timezone;
		/**  LOCATION_-prefixed final string from {@link SurveyGeotagService}; if LOCATION_UNAVAILABLE is chosen, location data is ignored */
		public String locationStatus;
		/** latitude at which the survey response was recorded, if available */
		public double locationLatitude;
		/** longitude at which the survey response was recorded, if available */
		public double locationLongitude;
		/** the provider for the location data, if available */
		public String locationProvider;
		/** the accuracy of the location data, if available */
		public float locationAccuracy;
		/** time reported from location provider, if available */
		public long locationTime;
		/** the id of the survey to which the response corresponds, in URN format */
		public String surveyId;
		/** the context in which the survey was launched (e.g. triggered, user-initiated, etc.) */
		public String surveyLaunchContext;
		/** the response data as a JSON-encoded string */
		public String response;
		/** read-only, an int indicating the status of a response; use constants supplied in this class (e.g. STATUS_UPLOADED) */
		public int status;
		/** read-only, a hash that uniquely identifies this response */
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
    			r.status = cursor.getInt(cursor.getColumnIndex(Response.STATUS));
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
    			values.put(Response.STATUS, status);
    			
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
		public static final String EXTRA_VALUE = "extra_value";
		
		// data fields here to support use of the PromptResponse class as a data holder (and not just a schema definer)
		// this should be reconciled by some kind of real ORM someday
		public long _id;
		public long mResponseID;
		public String mCompositeID;
		public String mPromptID;
		public String mValue;
		public String mExtraValue;
		
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
    			temp.mExtraValue = cursor.getString(cursor.getColumnIndex(PromptResponse.EXTRA_VALUE));
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
        	values.put(PromptResponse.EXTRA_VALUE, mExtraValue);
        	
        	return values;
        }
	}
}
