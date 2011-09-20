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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.CampaignXmlHelper;
import org.ohmage.PromptXmlParser;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.PromptResponse;
import org.ohmage.db.DbContract.Response;
import org.ohmage.db.DbContract.Survey;
import org.ohmage.db.DbContract.SurveyPrompt;
import org.ohmage.service.SurveyGeotagService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Xml;
import edu.ucla.cens.systemlog.Log;

public class DbHelper extends SQLiteOpenHelper {
	
	private static final String TAG = "DbHelper";
	
	private static final String DB_NAME = "ohmage.db";
	private static final int DB_VERSION = 12;
	
	private Context mContext;
	
	public interface Tables {
		static final String RESPONSES = "responses";
		static final String CAMPAIGNS = "campaigns";
		static final String PROMPT_RESPONSES = "prompt_responses";
		static final String SURVEYS = "surveys";
		static final String SURVEY_PROMPTS = "survey_prompts";
		
		// joins declared here
		String RESPONSES_JOIN_CAMPAIGNS = String.format("%1$s inner join %2$s on %1$s.%3$s=%2$s.%4$s",
				RESPONSES, CAMPAIGNS, Response.CAMPAIGN_URN, Campaign.URN);
		
		String PROMPTS_JOIN_RESPONSES = String.format("%1$s inner join %2$s on %1$s.%3$s=%2$s.%4$s",
				PROMPT_RESPONSES, RESPONSES, PromptResponse.RESPONSE_ID, Response._ID);
		
		String SURVEY_PROMPTS_JOIN_SURVEYS = String.format("%1$s inner join %2$s on %1$s.%3$s=%2$s.%4$s",
				SURVEY_PROMPTS, SURVEYS, SurveyPrompt.SURVEY_ID, Survey.SURVEY_ID);
	}
	
	interface Subqueries {
		// nested queries declared here
		// this may only be used on a PromptResponse query, since it references PromptResponse.COMPOSITE_ID
		String PROMPTS_GET_TYPES = String.format(
				"(select * from %1$s where %1$s.%2$s=%3$s)",
				Tables.SURVEY_PROMPTS, SurveyPrompt.COMPOSITE_ID, PromptResponse.COMPOSITE_ID);
	}

	public DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		mContext = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.CAMPAIGNS + " ("
				+ Campaign._ID + " INTEGER PRIMARY KEY, "
				+ Campaign.URN + " TEXT, "
				+ Campaign.NAME + " TEXT, "
				+ Campaign.DESCRIPTION + " TEXT, "
				+ Campaign.CREATION_TIMESTAMP + " TEXT, "
				+ Campaign.DOWNLOAD_TIMESTAMP + " TEXT, "
				+ Campaign.CONFIGURATION_XML + " TEXT, "
				+ Campaign.STATUS + " INTEGER, "
				+ Campaign.ICON + " TEXT "
				+ ");");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.SURVEYS + " ("
				+ Survey._ID + " INTEGER PRIMARY KEY, "
				+ Survey.CAMPAIGN_URN + " TEXT, " // cascade delete from campaigns
				+ Survey.SURVEY_ID + " TEXT, "
				+ Survey.TITLE + " TEXT, "
				+ Survey.DESCRIPTION + " TEXT, "
				+ Survey.SUMMARY + " TEXT, "
				+ Survey.SUBMIT_TEXT + " TEXT"
				+ ");");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.SURVEY_PROMPTS + " ("
				+ SurveyPrompt._ID + " INTEGER PRIMARY KEY, "
				+ SurveyPrompt.SURVEY_PID + " INTEGER, " // cascade delete from surveys
				+ SurveyPrompt.SURVEY_ID + " TEXT, "
				+ SurveyPrompt.COMPOSITE_ID + " TEXT, "
				+ SurveyPrompt.PROMPT_ID + " TEXT, "
				+ SurveyPrompt.PROMPT_TEXT + " TEXT, "
				+ SurveyPrompt.PROMPT_TYPE + " TEXT"
				+ ");");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.RESPONSES + " ("
				+ Response._ID + " INTEGER PRIMARY KEY, "
				+ Response.CAMPAIGN_URN + " TEXT, " // cascade delete from campaigns
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
				+ Response.SOURCE + " TEXT, "
				+ Response.HASHCODE + " TEXT"
				+ ");");
		
		// make campaign URN unique in the campaigns table
		db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
				+ Campaign.URN + "_idx ON "
				+ Tables.CAMPAIGNS + " (" + Campaign.URN + ");");
		
		// create a "flat" table of prompt responses so we can easily compute aggregates
		// across multiple survey responses (and potentially prompts)
		db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.PROMPT_RESPONSES + " ("
				+ PromptResponse._ID + " INTEGER PRIMARY KEY, "
				+ PromptResponse.RESPONSE_ID + " INTEGER, " // cascade delete from responses
				+ PromptResponse.COMPOSITE_ID + " TEXT, "
				+ PromptResponse.PROMPT_ID + " TEXT, "
				+ PromptResponse.PROMPT_VALUE + " TEXT, "
				+ PromptResponse.EXTRA_VALUE + " TEXT"
				+ ");");
		
		// for responses, index the campaign and survey ID columns, as we'll be selecting on them
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
		
		//  for responses, to prevent duplicates, add a unique key on the
		// 'hashcode' column, which is just a hash of the concatentation
		// of the campaign urn + survey ID + username + time of the response,
		// computed and maintained by us, unfortunately :\
		db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
				+ Response.HASHCODE + "_idx ON "
				+ Tables.RESPONSES + " (" + Response.HASHCODE + ");");
		
		// for prompt values, index on the response id for fast lookups
		db.execSQL("CREATE INDEX IF NOT EXISTS "
				+ PromptResponse.RESPONSE_ID + "_idx ON "
				+ Tables.PROMPT_RESPONSES + " (" + PromptResponse.RESPONSE_ID + ");");
		
		// --------
		// --- set up the triggers to implement cascading deletes, too
		// --------
		
		// annoyingly, sqlite 3.5.9 doesn't support recursive triggers.
		// we must first disable them before running these statements,
		// and each trigger has to delete everything associated w/the entity in question
		db.execSQL("PRAGMA recursive_triggers = off");
		
		// delete everything associated with a campaign when it's removed
		db.execSQL("CREATE TRIGGER IF NOT EXISTS "
				+ Tables.CAMPAIGNS + "_cascade_del AFTER DELETE ON "
				+ Tables.CAMPAIGNS
				+ " BEGIN "
				
				+ "DELETE from " + Tables.SURVEY_PROMPTS
				+ " WHERE " + SurveyPrompt._ID + " IN ("
					+ " SELECT " + Tables.SURVEY_PROMPTS + "." + SurveyPrompt._ID + " FROM " + Tables.SURVEY_PROMPTS + " SP"
					+ " INNER JOIN " + Tables.SURVEYS + " S ON S." + Survey._ID + "=SP." + SurveyPrompt.SURVEY_PID
					+ " WHERE S." + Survey.CAMPAIGN_URN + "=old." + Campaign.URN
				+ "); "
				
				+ "DELETE from " + Tables.PROMPT_RESPONSES
				+ " WHERE " + PromptResponse._ID + " IN ("
					+ " SELECT " + Tables.PROMPT_RESPONSES + "." + PromptResponse._ID + " FROM " + Tables.PROMPT_RESPONSES + " PR"
					+ " INNER JOIN " + Tables.RESPONSES + " R ON R." + Response._ID + "=PR." + PromptResponse.RESPONSE_ID
					+ " WHERE R." + Response.CAMPAIGN_URN + "=old." + Campaign.URN
				+ "); "
			
				+ "DELETE from " + Tables.SURVEYS + " WHERE " + Survey.CAMPAIGN_URN + "=old." + Campaign.URN + "; "
				+ "DELETE from " + Tables.RESPONSES + " WHERE " + Response.CAMPAIGN_URN + "=old." + Campaign.URN + "; "
				+ "END;");
		
		db.execSQL("CREATE TRIGGER IF NOT EXISTS "
				+ Tables.SURVEYS + "_cascade_del AFTER DELETE ON "
				+ Tables.SURVEYS
				+ " BEGIN "
				+ "DELETE from " + Tables.SURVEY_PROMPTS + " WHERE " + SurveyPrompt.SURVEY_PID + "=old." + Survey._ID + "; "
				+ "END;");
		
		db.execSQL("CREATE TRIGGER IF NOT EXISTS "
				+ Tables.RESPONSES + "_cascade_del AFTER DELETE ON "
				+ Tables.RESPONSES
				+ " BEGIN "
				+ "DELETE from " + Tables.PROMPT_RESPONSES + " WHERE " + PromptResponse.RESPONSE_ID + "=old." + Response._ID + "; "
				+ "END;");
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO: create an actual upgrade plan rather than just dumping and recreating everything
		clearAll(db);
	}
	
	public void clearAll(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS " + Tables.CAMPAIGNS);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEYS);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.SURVEY_PROMPTS);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.RESPONSES);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.PROMPT_RESPONSES);
		onCreate(db);
	}
	
	public void clearAll() {
		// this is for allowing non onUpgrade calls to clear the db.
		// we acquire the handle manually here and then invoke clearAll(db) as we normally do.
		SQLiteDatabase db = getWritableDatabase();
		clearAll(db);
		// we also have to close it, since it's not a managed reference as with onUpgrade's db handle.
		db.close();
	}
	
	// helper method that returns a hex-formatted string for some given input
	public static String getSHA1Hash(String input) throws NoSuchAlgorithmException {
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
	 * @return the ID of the inserted record, or -1 if unsuccessful
	 */
	public long addResponseRow(SQLiteDatabase db, ContentValues values)
	{		
		long rowId = -1;
		
		// extract data that we'll need to parse the json + insert prompt responses
		String response = values.getAsString(Response.RESPONSE);
		String campaignUrn = values.getAsString(Response.CAMPAIGN_URN);
		String surveyId = values.getAsString(Response.SURVEY_ID);
		
		try {
			// start a transaction involving the following operations:
			// 1) insert feedback response row
			// 2) parse json-encoded responses and insert one row into prompts per entry
			db.beginTransaction();

			// do the actual insert into feedback responses
			rowId = db.insert(Tables.RESPONSES, null, values);
			
			// check if it succeeded; if not, we can't do anything
			if (rowId == -1)
				return -1;
			
			if (populatePromptsFromResponseJSON(db, rowId, response, campaignUrn, surveyId)) {
				// and we're done; finalize the transaction
				db.setTransactionSuccessful();
			}
			// else we fail and the transaction gets rolled back
		}
		catch (SQLiteConstraintException e) {
			Log.e(TAG, "Attempted to insert record that violated a SQL constraint (likely the hashcode)");
			return -1;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Generic exception thrown from db insert", e);
			return -1;
		}
		finally {
			db.endTransaction();
			db.close();
		}
			
		return rowId;
	}
	
	/**
	 * Flags a response as having been uploaded. This is used exclusively by the upload service.
	 * @param _id the ID of the response row to set as uploaded
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean setResponseRowUploaded(long _id) {
		ContentValues values = new ContentValues();
		ContentResolver cr = mContext.getContentResolver();
		values.put("uploaded", 1);
		return cr.update(Response.CONTENT_URI, values, Response._ID + "=" + _id, null) > 0;
	}
	
	/**
	 * Removes survey responses (and their associated prompts) for the given campaign.
	 * 
	 * @param campaignUrn the campaign URN for which to remove the survey responses
	 * @return
	 */
	public boolean removeResponseRows(String campaignUrn) {		
		ContentResolver cr = mContext.getContentResolver();
		return cr.delete(Response.CONTENT_URI, Response.CAMPAIGN_URN + "='" + campaignUrn + "'", null) > 0;
	}
	
	/**
	 * Removes survey responses that are "stale" for the given campaignUrn.
	 * 
	 * Staleness is defined as a survey response whose source field is "remote", or a response
	 * whose source field is "local" and uploaded field is 1.
	 * 
	 * @return
	 */
	public int removeStaleResponseRows(String campaignUrn) {
		// build and execute the delete on the response table
		String whereClause = "(" + Response.SOURCE + "='remote'" + " or (" + Response.SOURCE + "='local' and " + Response.UPLOADED + "=1))";
		
		if (campaignUrn != null)
			whereClause += " and " + Response.CAMPAIGN_URN + "='" + campaignUrn + "'";
		
		// get a contentresolver and pass the delete onto it (so it can notify, etc.)
		ContentResolver cr = mContext.getContentResolver();
		return cr.delete(Response.CONTENT_URI, whereClause, null);
	}
	
	/**
	 * Removes survey responses that are "stale" for all campaigns.
	 * 
	 * Staleness is defined as a survey response whose source field is "remote", or a response
	 * whose source field is "local" and uploaded field is 1.
	 * 
	 * @return
	 */
	public int removeStaleResponseRows() {
		return removeStaleResponseRows(null);
	}
	
	public List<Response> getSurveyResponses(String campaignUrn) {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(Response.getResponsesByCampaign(campaignUrn), null,
				Response.SOURCE + "='local' AND " +
				Response.UPLOADED + "=0", null, null);
		
		List<Response> responses = Response.fromCursor(cursor); 

		return responses;
	}
	
	/**
	 * Returns survey responses for the given campaign that were stored before the given cutoff value.
	 * Note: this only returns *local* survey responses that have not already been uploaded.
	 * 
	 * @param campaignUrn the campaign for which to retrieve survey responses
	 * @param cutoffTime the time before which survey responses should be returned
	 * @return a List<{@link Response}> of survey responses
	 */
	public List<Response> getSurveyResponsesBefore(String campaignUrn, long cutoffTime) {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(Response.getResponsesByCampaign(campaignUrn), null,
				Response.TIME + " < " + Long.toString(cutoffTime) + " AND " +
				Response.SOURCE + "='local' AND " +
				Response.UPLOADED + "=0", null, null);

		return Response.fromCursor(cursor);
	}

	public int updateRecentRowLocations(String locationStatus, double locationLatitude, double locationLongitude, String locationProvider, float locationAccuracy, long locationTime) {
		if (locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE))
			return -1;
		
		ContentValues vals = new ContentValues();
		vals.put(Response.LOCATION_STATUS, locationStatus);
		vals.put(Response.LOCATION_LATITUDE, locationLatitude);
		vals.put(Response.LOCATION_LONGITUDE, locationLongitude);
		vals.put(Response.LOCATION_PROVIDER, locationProvider);
		vals.put(Response.LOCATION_ACCURACY, locationAccuracy);
		vals.put(Response.LOCATION_TIME, locationTime);
		
		long earliestTimestampToUpdate = locationTime - SurveyGeotagService.LOCATION_STALENESS_LIMIT;
		
		ContentResolver cr = mContext.getContentResolver();
		int count = cr.update(Response.CONTENT_URI, vals,
				Response.LOCATION_STATUS + " = '" + SurveyGeotagService.LOCATION_UNAVAILABLE + "' AND "
				+ Response.TIME + " > " + earliestTimestampToUpdate + " AND "
				+ Response.SOURCE + " = 'local' AND "
				+ Response.UPLOADED + " = 0", null);
		
		return count;
	}
	
	/**
	 * Used by the ContentProvider to insert a campaign and also insert into interested tables. Don't use this directly; if
	 * you do, none of the contentobservers, etc. that are listening to Campaigns, Surveys, or SurveyPrompts will be notified.
	 * @param values a ContentValues collection, preferably generated by calling {@link Campaign}'s toCV() method
	 * @return the ID of the inserted record
	 */
	public long addCampaign(SQLiteDatabase db, ContentValues values) {

		long rowId = -1; // the row ID for the campaign that we'll eventually be returning
				
		try {
			// start the transaction that will include inserting the campaign + surveys + survey prompts
			db.beginTransaction();
			
			// hold onto some variables for processing
			String configurationXml = values.getAsString(Campaign.CONFIGURATION_XML);
			String campaignUrn = values.getAsString(Campaign.URN);
			
			// actually insert the campaign
			rowId = db.insert(Tables.CAMPAIGNS, null, values);
			
			if (configurationXml != null) {
				// xml parsing below, inserts into Surveys and SurveyPrompts
				if (populateSurveysFromCampaignXML(db, campaignUrn, configurationXml)) {
					// i think we're done now; finish up the transaction
					db.setTransactionSuccessful();
				}
				// else we fail and the transaction gets rolled back
			} else {
				db.setTransactionSuccessful();
			}
		}
		finally {
			db.endTransaction();
		}

		return rowId;
	}
	
	public boolean removeCampaign(String urn) {
		ContentResolver cr = mContext.getContentResolver();
		return cr.delete(Campaign.CONTENT_URI, Campaign.URN + "='" + urn +"'", null) > 0;
	}
	
	public Campaign getCampaign(String urn) {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(Campaign.getCampaignByURN(urn), null, null, null, null);
		
		// ensure that only one record is returned
		if (cursor.getCount() != 1) {
			cursor.close();
			return null;
		}
		
		// since we know we have one record, we know index 0 will exist
		return Campaign.fromCursor(cursor).get(0);
	}
	
	public List<Campaign> getCampaigns() {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(Campaign.getCampaigns(), null, null, null, null);		
		return Campaign.fromCursor(cursor);
	}
	
	/**
	 * Utility method that populates the Survey and SurveyPrompt tables for the campaign identified
	 * by campaignUrn and containing the given xml as campaignXML.
	 * 
	 * Note that this method takes a db handle so that it can be used in a transaction.
	 * 
 	 * @param db a handle to an existing writable db
	 * @param campaignUrn the urn of the campaign for which we're populating subtables
	 * @param campaignXML the XML for the campaign (not validated by this method)
	 * @return 
	 * 
	 */
	public boolean populateSurveysFromCampaignXML(SQLiteDatabase db, String campaignUrn, String campaignXML) {	
		try {
			// dump all the surveys (and consequently survey prompts) before we do anything
			// this is (perhaps surprisingly) desired behavior, as the surveys + survey prompts
			// should always reflect the state of the campaign XML, valid or not
			db.delete(Tables.SURVEYS, Survey.CAMPAIGN_URN + "=?", new String[]{campaignUrn});
			
			// do a pass over the XML to gather surveys and survey prompts
			XmlPullParser xpp = Xml.newPullParser();
			xpp.setInput(new ByteArrayInputStream(campaignXML.getBytes("UTF-8")), "UTF-8");
			int eventType = xpp.getEventType();
			String tagName;
			
			// various stacks to maintain state while walking through the xml tree
			Stack<String> tagStack = new Stack<String>();
			Survey curSurvey = null; // valid only within a survey, null otherwise
			Vector<SurveyPrompt> prompts = new Vector<SurveyPrompt>(); // valid only within a survey, empty otherwise

			// iterate through the xml, paying attention only to surveys and prompts
			// note that this does no validation outside of preventing itself from crashing catastrophically
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					tagName = xpp.getName();
					tagStack.push(tagName);
					
					if (tagName.equalsIgnoreCase("survey")) {
						if (curSurvey != null)
							throw new XmlPullParserException("encountered a survey tag inside another survey tag");
						
						curSurvey = new Survey();
						curSurvey.mCampaignUrn = campaignUrn;
					}
					else if (tagName.equalsIgnoreCase("prompt")) {
						SurveyPrompt sp = new SurveyPrompt();
						// FIXME: add the campaign + survey ID to make lookups easier?
						prompts.add(sp);
					}
				}
				else if (eventType == XmlPullParser.TEXT) {
					if (tagStack.size() >= 2) {
						// we may be in an entity>property situation, so check and assign accordingly
						if (tagStack.get(tagStack.size()-2).equalsIgnoreCase("survey")) {				
							// populating the current survey object with its properties here
							if (tagStack.peek().equalsIgnoreCase("id"))
								curSurvey.mSurveyID = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase("title"))
								curSurvey.mTitle = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase("description"))
								curSurvey.mDescription = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase("summaryText"))
								curSurvey.mSummary = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase("submitText"))
								curSurvey.mSubmitText = xpp.getText();
						}
						else if (tagStack.get(tagStack.size()-2).equalsIgnoreCase("prompt")) {
							SurveyPrompt sp = prompts.lastElement();
							
							// populating the last encountered survey prompt with its properties here
							if (tagStack.peek().equalsIgnoreCase("id"))
								sp.mPromptID = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase("promptText"))
								sp.mPromptText = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase("promptType"))
								sp.mPromptType = xpp.getText();
						}
					}
				}
				else if (eventType == XmlPullParser.END_TAG) {
					tagName = xpp.getName();
					tagStack.pop();
					
					if (tagName.equalsIgnoreCase("survey")) {
						// store the current survey to the database
						long surveyPID = db.insert(Tables.SURVEYS, null, curSurvey.toCV());
						
						// also store all the prompts we accumulated for it
						for (SurveyPrompt sp : prompts) {
							sp.mSurveyID = curSurvey.mSurveyID;
							sp.mSurveyPID = surveyPID;
							sp.mCompositeID = curSurvey.mCampaignUrn + ":" + curSurvey.mSurveyID;
							db.insert(Tables.SURVEY_PROMPTS, null, sp.toCV());
						}
						
						// flush the prompts we've stored up so far
						prompts.clear();
						
						// and clear us from being in any survey
						curSurvey = null;
					}
				}
				
				eventType = xpp.next();
			}
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean populatePromptsFromResponseJSON(SQLiteDatabase db, long responseRowID, String response, String campaignUrn, String surveyId) {
		try {
			JSONArray responseData = new JSONArray(response);
			
			// iterate through the responses and add them to the prompt table one by one
			for (int i = 0; i < responseData.length(); ++i) {
				// nab the jsonobject, which contains "prompt_id" and "value"
				JSONObject item = responseData.getJSONObject(i);
				
				// if the entry we're looking at doesn't include prompt_id or value, continue
				if (!item.has("prompt_id") || !item.has("value"))
					continue;
				
				// construct a new PromptResponse object to populate
				PromptResponse p = new PromptResponse();
				
				p.mCompositeID = campaignUrn + ":" + surveyId;
				p.mResponseID = responseRowID;
				p.mPromptID = item.getString("prompt_id");
				
				// determine too if we have to remap the value from a number to text
				// if custom_choices is included, then we do
				if (item.has("custom_choices")) {
					// build a hashmap of ID->label so we can do the remapping
					JSONArray choicesArray = item.getJSONArray("custom_choices");
					HashMap<String,String> glossary = new HashMap<String, String>();
					
					for (int iv = 0; iv < choicesArray.length(); ++iv) {
						JSONObject choiceObject = choicesArray.getJSONObject(iv);
						glossary.put(choiceObject.getString("choice_id"), choiceObject.getString("choice_value"));
					}
					
					// determine if the value is singular or an array
					// if it's an array, we need to remap each element
					try {
						JSONArray remapper = item.getJSONArray("value");
						
						for (int ir = 0; ir < remapper.length(); ++ir)
							remapper.put(ir, glossary.get(remapper.getString(ir)));
						
						p.mValue = remapper.toString();
					}
					catch (JSONException e) {
						// it wasn't a json array, so just remap the single value
						p.mValue = glossary.get(item.getString("value"));
					}
				}
				else {
					p.mValue = item.getString("value");
				}
				
				// and insert this into prompts				
				db.insert(Tables.PROMPT_RESPONSES, null, p.toCV());
			}
		}
		catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
}
