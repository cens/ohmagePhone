package edu.ucla.cens.andwellness.feedback;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for interacting with {@link FeedbackProvider}. Defines the kinds of entities
 * managed by the provider, their schemas, and their relationships.
 * @author faisal
 *
 */
public class FeedbackContract {
	public static final String CONTENT_AUTHORITY = "edu.ucla.cens.andwellness.feedback";
	private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
	
	// ===================================
	// === feedback responses schema
	// ===================================
	
	interface FeedbackResponsesColumns {
		String CAMPAIGN_URN = "campaign_urn";
		String USERNAME = "username";
		String DATE = "date";
		String TIME = "time";
		String TIMEZONE = "timezone";
		String LOCATION_STATUS ="location_status";
		String LOCATION_LATITUDE = "location_latitude";
		String LOCATION_LONGITUDE = "location_longitude";
		String LOCATION_PROVIDER = "location_provider";
		String LOCATION_ACCURACY = "location_accuracy";
		String LOCATION_TIME = "location_time";
		String SURVEY_ID = "survey_id";
		String SURVEY_LAUNCH_CONTEXT = "survey_launch_context";
		/** json-encoded response data */
		String RESPONSE = "response";
		/** SHA1 hash of the concatenation of campaign urn + survey ID + username + time. be sure to compute this and include it at time of insertion. */
		String HASHCODE = "hashcode";
		/** source of this data; either "local" for locally cached data, or "remote" if from the server */
		String SOURCE = "source";
	}
	
	public static final class FeedbackResponses implements FeedbackResponsesColumns, BaseColumns
	{
        public static final Uri CONTENT_URI =
        	BASE_CONTENT_URI.buildUpon().appendPath("responses").build();
        public static final String CONTENT_TYPE =
        	"vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".response";
        public static final String CONTENT_ITEM_TYPE =
        	"vnd.android.cursor.item/vnd." + CONTENT_AUTHORITY + ".response";
        
        public static Uri getResponseUri(long insertID) {
        	return CONTENT_URI.buildUpon().appendPath(Long.toString(insertID)).build();
        }
	}
	
	// ===================================
	// === feedback prompt responses schema
	// ===================================
	
	interface FeedbackPromptResponsesColumns {
		String RESPONSE_ID = "response_id";
		String PROMPT_ID = "prompt_id";
		String PROMPT_VALUE = "prompt_value";
	}
	
	public static final class FeedbackPromptResponses implements FeedbackPromptResponsesColumns, BaseColumns
	{
        public static final Uri CONTENT_URI =
        	BASE_CONTENT_URI.buildUpon().appendPath("prompts").build();
        public static final String CONTENT_TYPE =
        	"vnd.android.cursor.dir/vnd." + CONTENT_AUTHORITY + ".prompt";
        public static final String CONTENT_ITEM_TYPE =
        	"vnd.android.cursor.item/vnd." + CONTENT_AUTHORITY + ".prompt";
        
        public static Uri getPromptUri(long insertID) {
        	return CONTENT_URI.buildUpon().appendPath(Long.toString(insertID)).build();
        }
	}
	
	// makes this class non-instantiable, since it's just a collection of other classes for organizational purposes
	private FeedbackContract()
	{
		
	}
}
