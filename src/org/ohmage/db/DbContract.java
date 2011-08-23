package org.ohmage.db;

import org.ohmage.service.UploadService;

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
        
        public static Uri getResponseUri(long insertID) {
        	return CONTENT_URI.buildUpon().appendPath(Long.toString(insertID)).build();
        }
        
        public static Uri getResponses() {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("responses")
				.build();
        }
        
        public static Uri getResponsesByID(int responseID) {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("responses")
				.appendPath(Integer.toString(responseID))
				.build();
        }
        
        public static Uri getResponsesByCampaign(String campaignUrn) {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath(campaignUrn)
				.appendPath("responses")
				.build();
        }
        
        public static Uri getResponsesByCampaignAndSurvey(String campaignUrn, String surveyID) {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath(campaignUrn)
				.appendPath(surveyID)
				.appendPath("responses")
				.build();
        }
	}
	
	public static final class Campaign implements BaseColumns {
		public static final String URN = "urn";
		public static final String NAME = "name";
		public static final String DESCRIPTION = "description";
		public static final String CREATION_TIMESTAMP = "creationTimestamp";
		public static final String DOWNLOAD_TIMESTAMP = "downloadTimestamp";
		public static final String CONFIGURATION_XML = "configuration_xml";

		// data fields here to support use of the Campaign class as a data holder (and not just a schema definer)
		// this should be reconciled by some kind of real ORM someday
		public long _id;
		public String mUrn;
		public String mName;
		public String mDescription;
		public String mCreationTimestamp;
		public String mDownloadTimestamp;
		public String mXml;
		
        public static final Uri CONTENT_URI =
        	BASE_CONTENT_URI.buildUpon().appendPath("campaigns").build();
        public static final String CONTENT_TYPE =
        	"vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".campaign";
        public static final String CONTENT_ITEM_TYPE =
        	"vnd.android.cursor.item/vnd." + CONTENT_AUTHORITY + ".campaign";
	}
	
	// ===================================
	// === feedback prompt responses schema
	// ===================================
	
	public static final class PromptResponse implements BaseColumns
	{
		public static final String RESPONSE_ID = "response_id";
		public static final String PROMPT_ID = "prompt_id";
		public static final String PROMPT_VALUE = "prompt_value";
		public static final String CUSTOM_CHOICES = "custom_choices";
		
        public static final Uri CONTENT_URI =
        	BASE_CONTENT_URI.buildUpon().appendPath("prompts").build();
        public static final String CONTENT_TYPE =
        	"vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".prompt";
        public static final String CONTENT_ITEM_TYPE =
        	"vnd.android.cursor.item/vnd." + CONTENT_AUTHORITY + ".prompt";
        
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
        
        public static Uri getPromptsByResponseID(int responseID) {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath("responses")
				.appendPath(Integer.toString(responseID))
				.appendPath("prompts")
				.build();
        }
        
        public static Uri getPromptsByCampaignAndSurvey(String campaignUrn, String surveyID, String promptID) {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath(campaignUrn)
				.appendPath(surveyID)
				.appendPath("responses")
				.appendPath("prompts")
				.appendPath(promptID)
				.build();
        }
        
        public static Uri getPromptsByCampaignAndSurvey(String campaignUrn, String surveyID, String promptID, AggregateTypes aggregate) {
    		return BASE_CONTENT_URI.buildUpon()
				.appendPath(campaignUrn)
				.appendPath(surveyID)
				.appendPath("responses")
				.appendPath("prompts")
				.appendPath(promptID)
				.appendPath(aggregate.toString())
				.build();
        }
	}
}
