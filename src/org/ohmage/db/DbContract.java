package org.ohmage.db;

import java.util.ArrayList;
import java.util.List;

import org.ohmage.service.SurveyGeotagService;
import org.ohmage.service.OldUploadService;

import android.content.ContentUris;
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

    interface ResponseColumns {
		/** the username to whom the survey response belongs */
		String RESPONSE_USERNAME = "response_username";
		/** the date on which the survey response was recorded, assumedly in UTC */
		String RESPONSE_DATE = "response_date";
		/** milliseconds since the epoch when this survey response was completed */
		String RESPONSE_TIME = "response_time";
		/** the timezone in which the survey response was completed */
		String RESPONSE_TIMEZONE = "response_timezone";
		/**  LOCATION_-prefixed final string from {@link SurveyGeotagService}; if LOCATION_UNAVAILABLE is chosen, location data is ignored */
		String RESPONSE_LOCATION_STATUS ="response_location_status";
		/** latitude at which the survey response was recorded, if available */
		String RESPONSE_LOCATION_LATITUDE = "response_location_latitude";
		/** longitude at which the survey response was recorded, if available */
		String RESPONSE_LOCATION_LONGITUDE = "response_location_longitude";
		/** the provider for the location data, if available */
		String RESPONSE_LOCATION_PROVIDER = "response_location_provider";
		/** the accuracy of the location data, if available */
		String RESPONSE_LOCATION_ACCURACY = "response_location_accuracy";
		/** time reported from location provider, if available */
		String RESPONSE_LOCATION_TIME = "response_location_time";
		/** the context in which the survey was launched (e.g. triggered, user-initiated, etc.) */
		String RESPONSE_SURVEY_LAUNCH_CONTEXT = "response_survey_launch_context";
		/** the response data as a JSON-encoded string */
		String RESPONSE_JSON = "response_json";
		/** read-only, an int indicating the status of a response; use constants supplied in this class (e.g. STATUS_UPLOADED) */
		String RESPONSE_STATUS = "response_status";
		/** read-only, a hash that uniquely identifies this response */
		String RESPONSE_HASHCODE = "response_hashcode";
    }
    
    private static final String PATH_CAMPAIGNS = "campaigns";
    private static final String PATH_SURVEYS = "surveys";
    private static final String PATH_PROMPTS = "prompts";
    private static final String PATH_RESPONSES = "responses";
    
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

        /**
         * Build {@link Uri} that references any {@link Responses} associated
         * with the requested {@link #CAMPAIGN_URN}
         */
		public static Uri buildResponsesUri(String campaignUrn) {
            return CONTENT_URI.buildUpon().appendPath(campaignUrn).appendPath(PATH_RESPONSES).build();
		}
		
        /**
         * Build {@link Uri} that references any {@link Responses} associated
         * with the requested {@link #CAMPAIGN_URN} and {@link Surveys#SURVEY_ID}
         */
        public static Uri buildResponsesUri(String campaignUrn, String surveyId) {
            return buildSurveysUri(campaignUrn, surveyId).buildUpon().appendPath(PATH_RESPONSES).build();
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
	public static final class Responses implements BaseColumns, ResponseColumns {

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_RESPONSES).build();
		public static final String CONTENT_TYPE =
				"vnd.android.cursor.dir/vnd.ohmage.response";
		public static final String CONTENT_ITEM_TYPE =
				"vnd.android.cursor.item/vnd.ohmage.response";

		public static final String CAMPAIGN_URN = "campaign_urn";
		public static final String SURVEY_ID = "survey_id";
		
        /** Build {@link Uri} for requested {@link #_ID}  */
        public static Uri buildResponseUri(long responseId) {
        	return ContentUris.withAppendedId(CONTENT_URI, responseId);
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
