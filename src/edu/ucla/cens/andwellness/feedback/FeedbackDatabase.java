package edu.ucla.cens.andwellness.feedback;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import edu.ucla.cens.andwellness.feedback.FeedbackContract.FeedbackPromptResponses;
import edu.ucla.cens.andwellness.feedback.FeedbackContract.FeedbackResponses;
import edu.ucla.cens.andwellness.service.SurveyGeotagService;

public class FeedbackDatabase extends SQLiteOpenHelper
{
	private static final String TAG = "FeedbackDatabase";
	
	private static final String DB_NAME = "andw.feedback.db";
	private static final int DB_VERSION = 1;

	interface Tables {
		String RESPONSES = "responses";
		String PROMPTS = "prompts";
		
		// joins declared here
		String PROMPTS_JOIN_RESPONSES = String.format("%1$s inner join %2$s on %1$s.%3$s=%2$s.%4$s",
				PROMPTS, RESPONSES, FeedbackPromptResponses.RESPONSE_ID, FeedbackResponses._ID);
	}
	
	public FeedbackDatabase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// create our caching table for responses
		db.execSQL("CREATE TABLE " + Tables.RESPONSES + " ("
				+ FeedbackResponses._ID + " INTEGER PRIMARY KEY, "
				+ FeedbackResponses.CAMPAIGN_URN + " TEXT, "
				+ FeedbackResponses.USERNAME + " TEXT, "
				+ FeedbackResponses.DATE + " TEXT, "
				+ FeedbackResponses.TIME + " INTEGER, "
				+ FeedbackResponses.TIMEZONE + " TEXT, "
				+ FeedbackResponses.LOCATION_STATUS + " TEXT, "
				+ FeedbackResponses.LOCATION_LATITUDE + " REAL, "
				+ FeedbackResponses.LOCATION_LONGITUDE + " REAL, "
				+ FeedbackResponses.LOCATION_PROVIDER + " TEXT, "
				+ FeedbackResponses.LOCATION_ACCURACY + " REAL, "
				+ FeedbackResponses.LOCATION_TIME + " INTEGER, "
				+ FeedbackResponses.SURVEY_ID + " TEXT, "
				+ FeedbackResponses.SURVEY_LAUNCH_CONTEXT + " TEXT, "
				+ FeedbackResponses.RESPONSE + " TEXT, "
				+ FeedbackResponses.HASHCODE + " TEXT, "
				+ FeedbackResponses.SOURCE + " TEXT"
				+ ");");
		
		// index the campaign and survey ID columns, as we'll be selecting on them
		db.execSQL("CREATE INDEX IF NOT EXISTS "
				+ FeedbackResponses.CAMPAIGN_URN + "_idx ON "
				+ Tables.RESPONSES + " (" + FeedbackResponses.CAMPAIGN_URN + ");");
		db.execSQL("CREATE INDEX IF NOT EXISTS "
				+ FeedbackResponses.SURVEY_ID + "_idx ON "
				+ Tables.RESPONSES + " (" + FeedbackResponses.SURVEY_ID + ");");
		// also index the time column, as we'll use that for time-related queries
		db.execSQL("CREATE INDEX IF NOT EXISTS "
				+ FeedbackResponses.TIME + "_idx ON "
				+ Tables.RESPONSES + " (" + FeedbackResponses.TIME + ");");
		
		// finally, to prevent duplicates, add a unique key on the
		// 'hashcode' column, which is just a hash of the concatentation
		// of the campaign urn + survey ID + username + time of the response,
		// computed and maintained by us, unfortunately :\
		db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
				+ FeedbackResponses.HASHCODE + "_idx ON "
				+ Tables.RESPONSES + " (" + FeedbackResponses.HASHCODE + ");");
		
		// create a "flat" table of prompt responses so we
		// can easily compute aggregates across multiple
		// survey responses (and potentially prompts)
		// NOTE: be sure to delete all entries from this table
		// whose "response_id" matches the Response._id column when
		// deleting from the TABLE_RESPONSES table
		db.execSQL("CREATE TABLE " + Tables.PROMPTS + " ("
				+ FeedbackPromptResponses._ID + " INTEGER PRIMARY KEY, "
				+ FeedbackPromptResponses.RESPONSE_ID + " INTEGER, " // foreign key to TABLE_RESPONSES
				+ FeedbackPromptResponses.PROMPT_ID + " TEXT, "
				+ FeedbackPromptResponses.PROMPT_VALUE + " TEXT"
				+ ");");
		
		// and index on the response id for fast lookups
		db.execSQL("CREATE INDEX IF NOT EXISTS "
				+ FeedbackPromptResponses.RESPONSE_ID + "_idx ON "
				+ Tables.PROMPTS + " (" + FeedbackPromptResponses.RESPONSE_ID + ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// destroy and recreate our tables
		db.execSQL("DROP TABLE IF EXISTS " + Tables.RESPONSES);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.PROMPTS);
		onCreate(db);
	}
	
	public void clearAll() {
		SQLiteDatabase db = getWritableDatabase();
		
		if (db == null) {
			return;
		}
		
		db.execSQL("DROP TABLE IF EXISTS " + Tables.RESPONSES);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.PROMPTS);
		onCreate(db);
		
		db.close();
	}
	
	// =========================================
	// === legacy data access and insertion
	// =========================================
	
	// helper method that returns a hex-formatted string for some given input
	private static String getSHA1Hash(String input) throws NoSuchAlgorithmException {
		Formatter formatter = new Formatter();
		MessageDigest md = MessageDigest.getInstance("SHA1");
		byte[] hash = md.digest(input.getBytes());

        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        
        return formatter.toString();
    }
	
	/**
	 * Adds a response to the feedback database.
	 * 
	 * @param campaignUrn the campaign URN for which to record the survey response
	 * @param username the username to whom the survey response belongs
	 * @param date the date on which the survey response was recorded, assumedly in UTC
	 * @param time milliseconds since the epoch when this survey response was completed
	 * @param timezone the timezone in which the survey response was completed
	 * @param locationStatus LOCATION_-prefixed final string from {@link SurveyGeotagService}; if LOCATION_UNAVAILABLE is chosen, location data is ignored
	 * @param locationLatitude latitude at which the survey response was recorded, if available
	 * @param locationLongitude longitude at which the survey response was recorded, if available
	 * @param locationProvider the provider for the location data, if available
	 * @param locationAccuracy the accuracy of the location data, if available
	 * @param locationTime time reported from location provider, if available
	 * @param surveyId the id of the survey to which the response corresponds, in URN format
	 * @param surveyLaunchContext the context in which the survey was launched (e.g. triggered, user-initiated, etc.)
	 * @param response the response data as a JSON-encoded string
	 * @param source the source of this data, either "local" or "remote"
	 * @return the ID of the inserted record, or -1 if unsuccessful
	 */
	public long addResponseRow(String campaignUrn, String username, String date, long time, String timezone, String locationStatus, double locationLatitude, double locationLongitude, String locationProvider, float locationAccuracy, long locationTime, String surveyId, String surveyLaunchContext, String response, String source)
	{
		SQLiteDatabase db = getWritableDatabase();
		
		if (db == null) {
			return -1;
		}
		
		// start a transaction involving the following operations:
		// 1) insert feedback response row
		// 2) parse json-encoded responses and insert one row into prompts per entry
		db.beginTransaction();
		
		try {
			ContentValues values = new ContentValues();
			values.put(FeedbackResponses.CAMPAIGN_URN, campaignUrn);
			values.put(FeedbackResponses.USERNAME, username);
			values.put(FeedbackResponses.DATE, date);
			values.put(FeedbackResponses.TIME, time);
			values.put(FeedbackResponses.TIMEZONE, timezone);
			values.put(FeedbackResponses.LOCATION_STATUS, locationStatus);
			
			if (locationStatus != SurveyGeotagService.LOCATION_UNAVAILABLE)
			{
				values.put(FeedbackResponses.LOCATION_LATITUDE, locationLatitude);
				values.put(FeedbackResponses.LOCATION_LONGITUDE, locationLongitude);
				values.put(FeedbackResponses.LOCATION_PROVIDER, locationProvider);
				values.put(FeedbackResponses.LOCATION_ACCURACY, locationAccuracy);
			}
			
			values.put(FeedbackResponses.LOCATION_TIME, locationTime);
			values.put(FeedbackResponses.SURVEY_ID, surveyId);
			values.put(FeedbackResponses.SURVEY_LAUNCH_CONTEXT, surveyLaunchContext);
			values.put(FeedbackResponses.RESPONSE, response);
			
			// bookkeeping: compute the hashcode and add that, too
			String hashableData = campaignUrn + surveyId + username + date;
			String hashcode = getSHA1Hash(hashableData);
			values.put(FeedbackResponses.HASHCODE, hashcode);
			values.put(FeedbackResponses.SOURCE, source);
			
			// do the actual insert into feedback responses
			long rowId = db.insert(Tables.RESPONSES, null, values);
			
			// check if it succeeded; if not, we can't do anything
			if (rowId == -1)
				return -1;
			
			// more bookkeeping: parse the responses and add those to the prompt responses table one by one
			JSONArray responseData = new JSONArray(response);
			
			// iterate through the responses and add them to the prompt table one by one
			for (int i = 0; i < responseData.length(); ++i) {
				// nab the jsonobject, which contains "prompt_id" and "value"
				// and possibly "custom_choices", but we're not storing that for now
				JSONObject item = responseData.getJSONObject(i);
				
				// and insert this into prompts
				ContentValues promptValues = new ContentValues();
				promptValues.put(FeedbackPromptResponses.RESPONSE_ID, rowId);
				promptValues.put(FeedbackPromptResponses.PROMPT_ID, item.getString("prompt_id"));
				promptValues.put(FeedbackPromptResponses.PROMPT_VALUE, item.getString("value"));
				
				db.insert(Tables.PROMPTS, null, promptValues);
			}
			
			// and we're done; finalize the transaction
			db.setTransactionSuccessful();
			
			// return the inserted feedback response row
			return rowId;
		}
		catch (JSONException e) {
			Log.e(TAG, "Unable to parse response data in insert", e);
			return -1;
		}
		catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Unable to produce hashcode -- is SHA-1 supported?", e);
			return -1;
		}
		finally {
			db.endTransaction();
			db.close();
		}
	}
	
	/**
	 * Adds a response to the feedback database, but without location data.
	 * 
	 * @param campaignUrn the campaign URN for which to record the survey response
	 * @param username the username to whom the survey response belongs
	 * @param date the date on which the survey response was recorded, assumedly in UTC
	 * @param time milliseconds since the epoch when this survey response was completed
	 * @param timezone the timezone in which the survey response was completed
	 * @param surveyId the id of the survey to which the response corresponds, in URN format
	 * @param surveyLaunchContext the context in which the survey was launched (e.g. triggered, user-initiated, etc.)
	 * @param response the response data as a JSON-encoded string
	 * @param source the source of this data, either "local" or "remote"
	 * @return the ID of the inserted record, or -1 if unsuccessful
	 */
	public long addResponseRow(String campaignUrn, String username, String date, long time, String timezone, String surveyId, String surveyLaunchContext, String response, String source) {
		// just call the normal addresponserow with locationstatus set to unavailable and garbage location data
		// the original method is smart enough to not insert the garbage location data
		return addResponseRow(campaignUrn, username, date, time, timezone, SurveyGeotagService.LOCATION_UNAVAILABLE, -1, -1, null, -1, -1, surveyId, surveyLaunchContext, response, source);
	}
	
	/*
	public List<Response> getSurveyResponses(String campaignUrn) {
		
		SQLiteDatabase db = openDb(false);
		
		if (db == null) {
			return null;
		}
		
		String selectionCriteria = null;
		
		if (campaignUrn != null)
			selectionCriteria = FeedbackResponses.CAMPAIGN_URN + "='" + campaignUrn + "'";
		
		Cursor cursor = db.query(Tables.RESPONSES, null, selectionCriteria, null, null, null, null);
		
		List<Response> responses = readResponseRows(cursor); 
			
		closeDb(db);
		
		return responses;
	}
	
	private List<Response> readResponseRows(Cursor cursor) {
		
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
			responses.add(r);
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		return responses; 
	}
	*/
}