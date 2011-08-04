/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.PromptResponse;
import org.ohmage.db.DbContract.Response;
import org.ohmage.service.SurveyGeotagService;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import edu.ucla.cens.systemlog.Log;

public class DbHelper extends SQLiteOpenHelper {
	
	private static final String TAG = "DbHelper";
	
	private static final String DB_NAME = "ohmage.db";
	private static final int DB_VERSION = 1;
	
	interface Tables {
		static final String RESPONSES = "responses";
		static final String CAMPAIGNS = "campaigns";
		static final String PROMPTS = "prompts";
		
		// joins declared here
		String PROMPTS_JOIN_RESPONSES = String.format("%1$s inner join %2$s on %1$s.%3$s=%2$s.%4$s",
				PROMPTS, RESPONSES, PromptResponse.RESPONSE_ID, Response._ID);
	}
	
	private static boolean isDbOpen = false;
	private static Object dbLock = new Object();

	public DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL("CREATE TABLE " + Tables.RESPONSES + " ("
				+ Response._ID + " INTEGER PRIMARY KEY, "
				+ Response.CAMPAIGN_URN + " TEXT, "
				+ Response.USERNAME + " TEXT, "
				+ Response.DATE + " TEXT, "
				+ Response.TIME + " INTEGER, "
				+ Response.TIMEZONE + " TEXT, "
				+ Response.LOCATION_STATUS + " TEXT, "
				+ Response.LOCATION_LATITUDE + " REAL, "
				+ Response.LOCATION_LONGITUDE + " REAL, "
				+ Response.LOCATION_PROVIDER + " TEXT, "
				+ Response.LOCATION_ACCURACY + " REAL, "
				+ Response.LOCATION_TIME + " INTEGER, "
				+ Response.SURVEY_ID + " TEXT, "
				+ Response.SURVEY_LAUNCH_CONTEXT + " TEXT, "
				+ Response.RESPONSE + " TEXT, "
				+ Response.UPLOADED + " INTEGER DEFAULT 0, "
				+ Response.SOURCE + " TEXT"
				+ ");");
		
		db.execSQL("CREATE TABLE " + Tables.CAMPAIGNS + " ("
				+ Campaign._ID + " INTEGER PRIMARY KEY, "
				+ Campaign.URN + " TEXT, "
				+ Campaign.NAME + " TEXT, "
				+ Campaign.CREATION_TIMESTAMP + " TEXT, "
				+ Campaign.DOWNLOAD_TIMESTAMP + " TEXT, "
				+ Campaign.CONFIGURATION_XML + " TEXT "
				+ ");");
		
		// index the campaign and survey ID columns, as we'll be selecting on them
		db.execSQL("CREATE INDEX IF NOT EXISTS "
				+ Response.CAMPAIGN_URN + "_idx ON "
				+ Tables.RESPONSES + " (" + Response.CAMPAIGN_URN + ");");
		db.execSQL("CREATE INDEX IF NOT EXISTS "
				+ Response.SURVEY_ID + "_idx ON "
				+ Tables.RESPONSES + " (" + Response.SURVEY_ID + ");");
		// also index the time column, as we'll use that for time-related queries
		db.execSQL("CREATE INDEX IF NOT EXISTS "
				+ Response.TIME + "_idx ON "
				+ Tables.RESPONSES + " (" + Response.TIME + ");");
		
		// create a "flat" table of prompt responses so we
		// can easily compute aggregates across multiple
		// survey responses (and potentially prompts)
		// NOTE: be sure to delete all entries from this table
		// whose "response_id" matches the Response._id column when
		// deleting from the TABLE_RESPONSES table
		db.execSQL("CREATE TABLE " + Tables.PROMPTS + " ("
				+ PromptResponse._ID + " INTEGER PRIMARY KEY, "
				+ PromptResponse.RESPONSE_ID + " INTEGER, " // foreign key to TABLE_RESPONSES
				+ PromptResponse.PROMPT_ID + " TEXT, "
				+ PromptResponse.PROMPT_VALUE + " TEXT"
				+ ");");
		
		// and index on the response id for fast lookups
		db.execSQL("CREATE INDEX IF NOT EXISTS "
				+ PromptResponse.RESPONSE_ID + "_idx ON "
				+ Tables.PROMPTS + " (" + PromptResponse.RESPONSE_ID + ");");
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + Tables.RESPONSES);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.CAMPAIGNS);
		onCreate(db);
	}
	
	private SQLiteDatabase openDb() {
		synchronized(dbLock)
		{
			while (isDbOpen)
			{
				try
				{
					dbLock.wait();
				}
				catch (InterruptedException e){}

			}
			isDbOpen = true;
			try {
				return getWritableDatabase();
			} catch (SQLiteException e) {
				Log.e(TAG, "Error opening database: " + DB_NAME);
				isDbOpen = false;
				return null;
			}
		}
	}
	
	private void closeDb(SQLiteDatabase db) {
		synchronized(dbLock)
		{
			db.close();
			isDbOpen = false;
			dbLock.notify();
		}		
	}
	
	public void clearAll() {
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return;
		}
		
		db.execSQL("DROP TABLE IF EXISTS " + Tables.RESPONSES);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.CAMPAIGNS);
		onCreate(db);
		
		closeDb(db);
	}
	
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
			
			// bookkeeping: compute the hashcode and add that, too
			String hashableData = campaignUrn + surveyId + username + date;
			String hashcode = getSHA1Hash(hashableData);
			values.put(Response.HASHCODE, hashcode);
			
			// do the actual insert into feedback responses
			long rowId = -1;
			
			try {
				rowId = db.insert(Tables.RESPONSES, null, values);
			}
			catch (Exception e) {
				// just ignore it; we don't want the exceptions cluttering up the logs
				rowId = -1;
			}
			
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
				promptValues.put(PromptResponse.RESPONSE_ID, rowId);
				promptValues.put(PromptResponse.PROMPT_ID, item.getString("prompt_id"));
				promptValues.put(PromptResponse.PROMPT_VALUE, item.getString("value"));
				
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
	public long addResponseRowWithoutLocation(String campaignUrn, String username, String date, long time, String timezone, String surveyId, String surveyLaunchContext, String response, String source) {
		// just call the normal addresponserow with locationstatus set to unavailable and garbage location data
		// the original method is smart enough to not insert the garbage location data
		return addResponseRow(campaignUrn, username, date, time, timezone, SurveyGeotagService.LOCATION_UNAVAILABLE, -1, -1, null, -1, -1, surveyId, surveyLaunchContext, response, source);
	}
	
	/**
	 * Flags a response as having been uploaded. This is used exclusively by the upload service.
	 * @param _id the ID of the response row to set as uploaded
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean setResponseRowUploaded(long _id) {
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return false;
		}
		
		ContentValues values = new ContentValues();
		values.put("uploaded", 1);
		int count = db.update(Tables.RESPONSES, values, Response._ID + "=" + _id, null);
		
		closeDb(db);
		
		return count > 0;
	}
	
	public boolean removeResponseRows(String campaignUrn) {
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return false;
		}
		
		// clear the prompt responses for this campaign first
		
		int count = db.delete(Tables.RESPONSES, Response.CAMPAIGN_URN + "='" + campaignUrn + "'", null);
		
		closeDb(db);
		
		return count > 0;
	}
	
	public void getResponseRow() {
		
	}
	
	public List<Response> getSurveyResponses(String campaignUrn) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		Cursor cursor = db.query(Tables.RESPONSES, null, Response.CAMPAIGN_URN + "='" + campaignUrn + "'", null, null, null, null);
		
		List<Response> responses = readResponseRows(cursor); 
			
		closeDb(db);
		
		return responses;
	}
	
//	public List<Response> getSurveyResponsesBefore(long cutoffTime) {
//		
//		SQLiteDatabase db = openDb();
//		
//		if (db == null) {
//			return null;
//		}
//		
//		Cursor cursor = db.query(Tables.RESPONSES, null, Response.TIME + " < " + Long.toString(cutoffTime), null, null, null, null);
//		
//		List<Response> responses = readResponseRows(cursor); 
//		
//		closeDb(db);
//		
//		return responses;
//	}
	
	public List<Response> getSurveyResponsesBefore(String campaignUrn, long cutoffTime) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		Cursor cursor = db.query(Tables.RESPONSES, null, Response.CAMPAIGN_URN + "='" + campaignUrn + "' AND " + Response.TIME + " < " + Long.toString(cutoffTime), null, null, null, null);
		
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
			r.source = cursor.getString(cursor.getColumnIndex(Response.SOURCE));
			r.uploaded = cursor.getInt(cursor.getColumnIndex(Response.UPLOADED));
			responses.add(r);
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		return responses; 
	}
	
	/*public List<Response> getResponseRows() {
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		ArrayList<Response> responses = new ArrayList<Response>();
		
		Cursor cursor = db.query(Tables.RESPONSES, null, null, null, null, null, null);
		
		cursor.moveToFirst();
		
		for (int i = 0; i < cursor.getCount(); i++) {
			
			Response r = new Response();
			r._id = cursor.getLong(cursor.getColumnIndex(Response._ID));
			r.campaign = cursor.getString(cursor.getColumnIndex(Response.CAMPAIGN));
			r.campaignVersion = cursor.getString(cursor.getColumnIndex(Response.CAMPAIGN_VERSION));
			r.username = cursor.getString(cursor.getColumnIndex(Response.USERNAME));
			r.date = cursor.getString(cursor.getColumnIndex(Response.DATE));
			r.time = cursor.getLong(cursor.getColumnIndex(Response.TIME));
			r.timezone = cursor.getString(cursor.getColumnIndex(Response.TIMEZONE));
			r.locationStatus = cursor.getString(cursor.getColumnIndex(Response.LOCATION_STATUS));
			if (! r.locationStatus.equals(SurveyLocationService.LOCATION_UNAVAILABLE)) {
				
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
		
		closeDb(db);
		
		return responses;
	}*/
	
	public void updateResponseRow() {
		
	}

	public int updateRecentRowLocations(String locationStatus, double locationLatitude, double locationLongitude, String locationProvider, float locationAccuracy, long locationTime) {
		if (locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE)) return -1;
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return -1;
		}
		
		ContentValues vals = new ContentValues();
		vals.put(Response.LOCATION_STATUS, locationStatus);
		vals.put(Response.LOCATION_LATITUDE, locationLatitude);
		vals.put(Response.LOCATION_LONGITUDE, locationLongitude);
		vals.put(Response.LOCATION_PROVIDER, locationProvider);
		vals.put(Response.LOCATION_ACCURACY, locationAccuracy);
		vals.put(Response.LOCATION_TIME, locationTime);
		
		
		long earliestTimestampToUpdate = locationTime - SurveyGeotagService.LOCATION_STALENESS_LIMIT;
		
		int count = db.update(Tables.RESPONSES, vals, Response.LOCATION_STATUS + " = '" + SurveyGeotagService.LOCATION_UNAVAILABLE + "' AND " + Response.TIME + " > " + earliestTimestampToUpdate + " AND " + Response.SOURCE + " = 'local' AND " + Response.UPLOADED + " = 0", null);
		
		closeDb(db);
		
		return count;
	}
	
	public long addCampaign(String campaignUrn, String campaignName, String creationTimestamp, String downloadTimestamp, String configurationXml) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return -1;
		}
		
		ContentValues values = new ContentValues();
		values.put(Campaign.URN, campaignUrn);
		values.put(Campaign.NAME, campaignName);
		values.put(Campaign.CREATION_TIMESTAMP, creationTimestamp);
		values.put(Campaign.DOWNLOAD_TIMESTAMP, downloadTimestamp);
		values.put(Campaign.CONFIGURATION_XML, configurationXml);
		
		long rowId = db.insert(Tables.CAMPAIGNS, null, values);
		
		closeDb(db);
		
		return rowId;
	}
	
	public boolean removeCampaign(long _id) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return false;
		}
		
		int count = db.delete(Tables.CAMPAIGNS, Campaign._ID + "=" + _id, null);
		
		closeDb(db);
		
		return count > 0;
	}
	
	public boolean removeCampaign(String urn) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return false;
		}
		
		int count = db.delete(Tables.CAMPAIGNS, Campaign.URN + "='" + urn +"'", null);
		
		closeDb(db);
		
		return count > 0;
	}
	
	public Campaign getCampaign(long _id) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		Cursor cursor = db.query(Tables.CAMPAIGNS, null, Campaign._ID + "=" + _id, null, null, null, null);
		
		cursor.moveToFirst();
		
		Campaign c = new Campaign();
		c._id = cursor.getLong(cursor.getColumnIndex(Campaign._ID));
		c.mUrn = cursor.getString(cursor.getColumnIndex(Campaign.URN));
		c.mName = cursor.getString(cursor.getColumnIndex(Campaign.NAME));
		c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.CREATION_TIMESTAMP));
		c.mDownloadTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.DOWNLOAD_TIMESTAMP));
		c.mXml = cursor.getString(cursor.getColumnIndex(Campaign.CONFIGURATION_XML));
		
		cursor.close();
		
		closeDb(db);
		
		return c;
	}
	
	public Campaign getCampaign(String urn) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		Cursor cursor = db.query(Tables.CAMPAIGNS, null, Campaign.URN + "= ?", new String[] {urn}, null, null, null);
		
		cursor.moveToFirst();
		
		Campaign c = new Campaign();
		c._id = cursor.getLong(cursor.getColumnIndex(Campaign._ID));
		c.mUrn = cursor.getString(cursor.getColumnIndex(Campaign.URN));
		c.mName = cursor.getString(cursor.getColumnIndex(Campaign.NAME));
		c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.CREATION_TIMESTAMP));
		c.mDownloadTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.DOWNLOAD_TIMESTAMP));
		c.mXml = cursor.getString(cursor.getColumnIndex(Campaign.CONFIGURATION_XML));
		
		cursor.close();
		
		closeDb(db);
		
		return c;
	}
	
	public List<Campaign> getCampaigns() {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		List<Campaign> campaigns = new ArrayList<Campaign>();
		
		Cursor cursor = db.query(Tables.CAMPAIGNS, null, null, null, null, null, null);
		
		cursor.moveToFirst();
		
		for (int i = 0; i < cursor.getCount(); i++) {
			
			Campaign c = new Campaign();
			c._id = cursor.getLong(cursor.getColumnIndex(Campaign._ID));
			c.mUrn = cursor.getString(cursor.getColumnIndex(Campaign.URN));
			c.mName = cursor.getString(cursor.getColumnIndex(Campaign.NAME));
			c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.CREATION_TIMESTAMP));
			c.mDownloadTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.DOWNLOAD_TIMESTAMP));
			c.mXml = cursor.getString(cursor.getColumnIndex(Campaign.CONFIGURATION_XML));
			campaigns.add(c);
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		closeDb(db);
		
		return campaigns; 
	}
}
