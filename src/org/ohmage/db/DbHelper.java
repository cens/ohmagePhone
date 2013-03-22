/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.ohmage.db;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.SurveyPrompts;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.PromptResponse;
import org.ohmage.db.Models.Response;
import org.ohmage.db.Models.Survey;
import org.ohmage.db.Models.SurveyPrompt;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

public class DbHelper extends SQLiteOpenHelper {

	private static final String TAG = "DbHelper";

	private static final String DB_NAME = "ohmage.db";
	private static final int DB_VERSION = 34;
	
	private final Context mContext;

	public interface Tables {
		static final String RESPONSES = "responses";
		static final String CAMPAIGNS = "campaigns";
		static final String PROMPT_RESPONSES = "prompt_responses";
		static final String SURVEYS = "surveys";
		static final String SURVEY_PROMPTS = "survey_prompts";

		// joins declared here
		String RESPONSES_JOIN_CAMPAIGNS_SURVEYS =
			Tables.RESPONSES
			+ " inner join " + Tables.CAMPAIGNS
				+ " on " + Tables.CAMPAIGNS + "." + Campaigns.CAMPAIGN_URN + "=" + Tables.RESPONSES + "." + Responses.CAMPAIGN_URN
			+ " inner join " + Tables.SURVEYS +
				" on " + Tables.SURVEYS + "." + Surveys.SURVEY_ID + "=" + Tables.RESPONSES + "." + Responses.SURVEY_ID +
				" and " + Tables.SURVEYS + "." + Surveys.CAMPAIGN_URN + "=" + Tables.RESPONSES + "." + Responses.CAMPAIGN_URN;
				
		String PROMPTS_JOIN_RESPONSES_SURVEYS_CAMPAIGNS = String
				.format(
						"%1$s inner join %2$s on %1$s.%3$s=%2$s.%4$s",
						PROMPT_RESPONSES, // 1
						RESPONSES, // 2
						PromptResponses.RESPONSE_ID, // 3
						Responses._ID, // 4
						Responses.CAMPAIGN_URN, // 5
						Responses.SURVEY_ID); // 6

		String SURVEY_PROMPTS_JOIN_SURVEYS = String.format(
				"distinct %1$s inner join %2$s on %1$s.%3$s=%2$s.%4$s", SURVEY_PROMPTS,
				SURVEYS, SurveyPrompts.SURVEY_ID, Surveys.SURVEY_ID);

		String SURVEY_JOIN_CAMPAIGNS =
				Tables.SURVEYS
				+ " join " + Tables.CAMPAIGNS
					+ " on " + Tables.CAMPAIGNS + "." + Campaigns.CAMPAIGN_URN + "=" + Tables.SURVEYS + "." + Surveys.CAMPAIGN_URN;

	}

	interface Subqueries {
		// nested queries declared here
		// this may only be used on a PromptResponse query, since it references
		// PromptResponse.COMPOSITE_ID
		String PROMPTS_GET_TYPES = String.format(
				"(select * from %1$s where %1$s.%2$s=%3$s)",
				Tables.SURVEY_PROMPTS, SurveyPrompts.COMPOSITE_ID,
				PromptResponses.COMPOSITE_ID);
	}

	public DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.CAMPAIGNS + " ("
				+ Campaigns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ Campaigns.CAMPAIGN_URN + " TEXT, "
				+ Campaigns.CAMPAIGN_NAME + " TEXT, "
				+ Campaigns.CAMPAIGN_DESCRIPTION + " TEXT, "
				+ Campaigns.CAMPAIGN_CREATED + " TEXT, "
				+ Campaigns.CAMPAIGN_DOWNLOADED + " TEXT, "
				+ Campaigns.CAMPAIGN_CONFIGURATION_XML + " TEXT, "
				+ Campaigns.CAMPAIGN_STATUS + " INTEGER, "
				+ Campaigns.CAMPAIGN_ICON + " TEXT, "
				+ Campaigns.CAMPAIGN_PRIVACY + " TEXT, "
				+ Campaigns.CAMPAIGN_UPDATED + " INTEGER NOT NULL DEFAULT 0 " +
				");");

		db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.SURVEYS + " ("
				+ Surveys._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ Surveys.CAMPAIGN_URN + " TEXT, " // cascade delete from campaigns
				+ Surveys.SURVEY_ID + " TEXT, "
				+ Surveys.SURVEY_TITLE + " TEXT, "
				+ Surveys.SURVEY_DESCRIPTION + " TEXT, "
				+ Surveys.SURVEY_SUBMIT_TEXT + " TEXT, "
				+ Surveys.SURVEY_INTRO_TEXT + " TEXT, "
				+ Surveys.SURVEY_ANYTIME + " INTEGER DEFAULT 1, "
				+ Surveys.SURVEY_STATUS + " INTEGER DEFAULT 0"
				+ ");");

		db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.SURVEY_PROMPTS + " ("
				+ SurveyPrompts._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ SurveyPrompts.SURVEY_PID + " INTEGER, " // cascade delete from surveys
				+ SurveyPrompts.SURVEY_ID + " TEXT, "
				+ SurveyPrompts.COMPOSITE_ID + " TEXT, "
				+ SurveyPrompts.PROMPT_ID + " TEXT, "
				+ SurveyPrompts.SURVEY_PROMPT_TEXT + " TEXT, "
				+ SurveyPrompts.SURVEY_PROMPT_TYPE + " TEXT, "
				+ SurveyPrompts.SURVEY_PROMPT_PROPERTIES + " TEXT "
				+ ");");

		db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.RESPONSES + " ("
				+ Responses._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ Responses.RESPONSE_UUID + " TEXT, "
				+ Responses.CAMPAIGN_URN + " TEXT, " // cascade delete from campaigns
				+ Responses.RESPONSE_USERNAME + " TEXT, "
				+ Responses.RESPONSE_DATE + " TEXT, "
				+ Responses.RESPONSE_TIME + " INTEGER, "
				+ Responses.RESPONSE_TIMEZONE + " TEXT, "
				+ Responses.RESPONSE_LOCATION_STATUS + " TEXT, "
				+ Responses.RESPONSE_LOCATION_LATITUDE + " REAL, "
				+ Responses.RESPONSE_LOCATION_LONGITUDE + " REAL, "
				+ Responses.RESPONSE_LOCATION_PROVIDER + " TEXT, "
				+ Responses.RESPONSE_LOCATION_ACCURACY + " REAL, "
				+ Responses.RESPONSE_LOCATION_TIME + " INTEGER, "
				+ Responses.SURVEY_ID + " TEXT, "
				+ Responses.RESPONSE_SURVEY_LAUNCH_CONTEXT + " TEXT, "
				+ Responses.RESPONSE_JSON + " TEXT, "
				+ Responses.RESPONSE_STATUS + " INTEGER DEFAULT 0"
				+ ");");

		// make campaign URN unique in the campaigns table
		db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS " + Campaigns.CAMPAIGN_URN
				+ "_idx ON " + Tables.CAMPAIGNS + " (" + Campaigns.CAMPAIGN_URN + ");");

		// create a "flat" table of prompt responses so we can easily compute
		// aggregates
		// across multiple survey responses (and potentially prompts)
		db.execSQL("CREATE TABLE IF NOT EXISTS "
				+ Tables.PROMPT_RESPONSES
				+ " ("
				+ PromptResponses._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ PromptResponses.RESPONSE_ID + " INTEGER, " // cascade delete from responses
				+ PromptResponses.COMPOSITE_ID + " TEXT, "
				+ PromptResponses.PROMPT_ID + " TEXT, "
				+ PromptResponses.PROMPT_RESPONSE_VALUE + " TEXT, "
				+ PromptResponses.PROMPT_RESPONSE_EXTRA_VALUE + " TEXT"
				+ ");");

		// for responses, index the campaign and survey ID columns, as we'll be
		// selecting on them
		db.execSQL("CREATE INDEX IF NOT EXISTS " + Responses.CAMPAIGN_URN
				+ "_idx ON " + Tables.RESPONSES + " (" + Responses.CAMPAIGN_URN
				+ ");");
		db.execSQL("CREATE INDEX IF NOT EXISTS " + Responses.SURVEY_ID
				+ "_idx ON " + Tables.RESPONSES + " (" + Responses.SURVEY_ID
				+ ");");
		// also index the time column, as we'll use that for time-related
		// queries
		db.execSQL("CREATE INDEX IF NOT EXISTS " + Responses.RESPONSE_TIME + "_idx ON "
				+ Tables.RESPONSES + " (" + Responses.RESPONSE_TIME + ");");

		// for responses, to prevent duplicates, add a unique key on the
		// 'uuid' column, which is assigned by the client at survey creation time,
		// but persisted by the server
		db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS " + Responses.RESPONSE_UUID
				+ "_idx ON " + Tables.RESPONSES + " (" + Responses.RESPONSE_UUID
				+ ");");

		// for prompt values, index on the response id for fast lookups
		db.execSQL("CREATE INDEX IF NOT EXISTS " + PromptResponses.RESPONSE_ID
				+ "_idx ON " + Tables.PROMPT_RESPONSES + " ("
				+ PromptResponses.RESPONSE_ID + ");");

		// --------
		// --- set up the triggers to implement cascading deletes, too
		// --------

		// delete everything associated with a campaign when it's removed
		db.execSQL("CREATE TRIGGER IF NOT EXISTS " + Tables.CAMPAIGNS
				+ "_cascade_del AFTER DELETE ON " + Tables.CAMPAIGNS
				+ " BEGIN "

				+ "DELETE from " + Tables.SURVEYS + " WHERE "
				+ Surveys.CAMPAIGN_URN + "=old." + Campaigns.CAMPAIGN_URN + "; "
				+ "DELETE from " + Tables.RESPONSES + " WHERE "
				+ Responses.CAMPAIGN_URN + "=old." + Campaigns.CAMPAIGN_URN + "; "
				+ "END;");

		db.execSQL("CREATE TRIGGER IF NOT EXISTS " + Tables.SURVEYS
				+ "_cascade_del AFTER DELETE ON " + Tables.SURVEYS + " BEGIN "
				+ "DELETE from " + Tables.SURVEY_PROMPTS + " WHERE "
				+ SurveyPrompts.SURVEY_PID + "=old." + Surveys._ID + "; "
				+ "END;");

		db.execSQL("CREATE TRIGGER IF NOT EXISTS " + Tables.RESPONSES
				+ "_cascade_del AFTER DELETE ON " + Tables.RESPONSES
				+ " BEGIN " + "DELETE from " + Tables.PROMPT_RESPONSES
				+ " WHERE " + PromptResponses.RESPONSE_ID + "=old."
				+ Responses._ID + "; " + "END;");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion < 33) {
			db.execSQL("ALTER TABLE " + Tables.CAMPAIGNS + " ADD COLUMN " +  Campaigns.CAMPAIGN_UPDATED + " INTEGER NOT NULL DEFAULT 0");
		}
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
		// we acquire the handle manually here and then invoke clearAll(db) as
		// we normally do.
		SQLiteDatabase db = getWritableDatabase();
		clearAll(db);
		// we also have to close it, since it's not a managed reference as with
		// onUpgrade's db handle.
		// db.close();
	}

	// helper method that returns a hex-formatted string for some given input
	public static String getSHA1Hash(String input)
			throws NoSuchAlgorithmException {
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
	public long addResponseRow(SQLiteDatabase db, ContentValues values) {
		long rowId = -1;

		// extract data that we'll need to parse the json + insert prompt
		// responses
		String response = values.getAsString(Responses.RESPONSE_JSON);
		String campaignUrn = values.getAsString(Responses.CAMPAIGN_URN);
		String surveyId = values.getAsString(Responses.SURVEY_ID);

		boolean madeTransaction = !db.inTransaction();

		try {
			// start a transaction involving the following operations:
			// 1) insert feedback response row
			// 2) parse json-encoded responses and insert one row into prompts
			// per entry
			if(madeTransaction) db.beginTransaction();

			// do the actual insert into feedback responses
			rowId = db.insert(Tables.RESPONSES, null, values);

			// check if it succeeded; if not lets try to update a response that exists
			if (rowId == -1) {
				if(values.containsKey(Responses.RESPONSE_UUID)) {
					Cursor c = db.query(Tables.RESPONSES, new String[] { BaseColumns._ID }, Responses.RESPONSE_UUID + "=?", new String[] { values.getAsString(Responses.RESPONSE_UUID) }, null, null, null);
					if(c.moveToFirst()) {
						rowId = c.getLong(0);
						c.close();
						db.update(Tables.RESPONSES, values, Responses.RESPONSE_UUID + "=?", new String[] { values.getAsString(Responses.RESPONSE_UUID) });
					}
				}
				if(rowId != -1 && madeTransaction)
					db.setTransactionSuccessful();
			} else {
				if (populatePromptsFromResponseJSON(db, rowId, response,
						campaignUrn, surveyId)) {
					// and we're done; finalize the transaction
					if(madeTransaction) db.setTransactionSuccessful();
				}
			}
		} finally {
			if(madeTransaction) db.endTransaction();
		}

		return rowId;
	}

	/**
	 * Flags a response as having been uploaded. This is used exclusively by the
	 * upload service.
	 * 
	 * @param _id
	 *            the ID of the response row to set as uploaded
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean setResponseRowUploaded(long _id) {
		ContentValues values = new ContentValues();
		ContentResolver cr = mContext.getContentResolver();
		values.put(Responses.RESPONSE_STATUS, Response.STATUS_UPLOADED);
		return cr.update(Responses.CONTENT_URI, values,
				Responses._ID + "=" + _id, null) > 0;
	}

	/**
	 * Removes survey responses that are "stale" for the given campaignUrn.
	 * 
	 * Staleness is defined as a survey response whose source field is "remote",
	 * or a response whose source field is "local" and uploaded field is 1.
	 * 
	 * @return
	 */
	public int removeStaleResponseRows(String campaignUrn) {
		// build and execute the delete on the response table
		String whereClause = "(" + Responses.RESPONSE_STATUS + "="
				+ Response.STATUS_DOWNLOADED + " or " + Responses.RESPONSE_STATUS + "="
				+ Response.STATUS_UPLOADED + ")";

		if (campaignUrn != null)
			whereClause += " and " + Responses.CAMPAIGN_URN + "='" + campaignUrn
					+ "'";

		// get a contentresolver and pass the delete onto it (so it can notify,
		// etc.)
		ContentResolver cr = mContext.getContentResolver();
		return cr.delete(Responses.CONTENT_URI, whereClause, null);
	}

	/**
	 * Returns survey responses for the given campaign that were stored before
	 * the given cutoff value. Note: this only returns *local* survey responses
	 * that have not already been uploaded.
	 * 
	 * @param campaignUrn
	 *            the campaign for which to retrieve survey responses
	 * @param cutoffTime
	 *            the time before which survey responses should be returned
	 * @return a List<{@link Response}> of survey responses
	 */
	public List<Response> getSurveyResponsesBefore(String campaignUrn,
			long cutoffTime) {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(Campaigns.buildResponsesUri(campaignUrn),
				null, Responses.RESPONSE_TIME + " < " + Long.toString(cutoffTime)
						+ " AND " + Responses.RESPONSE_STATUS + "="
						+ Response.STATUS_STANDBY, null, null);

		return Response.fromCursor(cursor);
	}

	/**
	 * Used by the ContentProvider to insert a campaign and also insert into
	 * interested tables. Don't use this directly; if you do, none of the
	 * contentobservers, etc. that are listening to Campaigns, Surveys, or
	 * SurveyPrompts will be notified.
	 * 
	 * @param values
	 *            a ContentValues collection, preferably generated by calling
	 *            {@link Campaign}'s toCV() method
	 * @return the ID of the inserted record
	 */
	public long addCampaign(SQLiteDatabase db, ContentValues values) {

		long rowId = -1; // the row ID for the campaign that we'll eventually be
							// returning

		boolean madeTransaction = !db.inTransaction();

		try {
			// start the transaction that will include inserting the campaign +
			// surveys + survey prompts
			if(madeTransaction) db.beginTransaction();

			// hold onto some variables for processing
			String configurationXml = values
					.getAsString(Campaigns.CAMPAIGN_CONFIGURATION_XML);
			String campaignUrn = values.getAsString(Campaigns.CAMPAIGN_URN);

			// actually insert the campaign
			rowId = db.insertWithOnConflict(Tables.CAMPAIGNS, null, values,SQLiteDatabase.CONFLICT_REPLACE);

			if (configurationXml != null) {
				// xml parsing below, inserts into Surveys and SurveyPrompts
				if (populateSurveysFromCampaignXML(db, campaignUrn,
						configurationXml)) {
					// i think we're done now; finish up the transaction
					if(madeTransaction) db.setTransactionSuccessful();
				}
				// else we fail and the transaction gets rolled back
			}
			else {
				if(madeTransaction) db.setTransactionSuccessful();
			}
		} finally {
			if(madeTransaction) db.endTransaction();
		}

		return rowId;
	}

	public Campaign getCampaign(String urn) {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(Campaigns.buildCampaignUri(urn), null, null,
				null, null);

		// ensure that only one record is returned
		if (cursor.getCount() != 1) {
			cursor.close();
			return null;
		}

		// since we know we have one record, we know index 0 will exist
		return Campaign.fromCursor(cursor).get(0);
	}

	public List<Campaign> getReadyCampaigns() {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(Campaigns.CONTENT_URI, null, Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY, null, null);
		return Campaign.fromCursor(cursor);
	}

	/**
	 * Utility method that populates the Survey and SurveyPrompt tables for the
	 * campaign identified by campaignUrn and containing the given xml as
	 * campaignXML.
	 * 
	 * Note that this method takes a db handle so that it can be used in a
	 * transaction.
	 * 
	 * @param db
	 *            a handle to an existing writable db
	 * @param campaignUrn
	 *            the urn of the campaign for which we're populating subtables
	 * @param campaignXML
	 *            the XML for the campaign (not validated by this method)
	 * @return
	 * 
	 */
	public boolean populateSurveysFromCampaignXML(SQLiteDatabase db, String campaignUrn, String campaignXML) {
		try {
			// dump all the surveys (and consequently survey prompts) before we
			// do anything
			// this is (perhaps surprisingly) desired behavior, as the surveys +
			// survey prompts
			// should always reflect the state of the campaign XML, valid or not
			db.delete(Tables.SURVEYS, Surveys.CAMPAIGN_URN + "=?",
					new String[] { campaignUrn });

			// We don't need to do anything else if there is no xml
			if(TextUtils.isEmpty(campaignXML))
				return true;

			// do a pass over the XML to gather surveys and survey prompts
			XmlPullParser xpp = Xml.newPullParser();
			xpp.setInput(
					new ByteArrayInputStream(campaignXML.getBytes("UTF-8")),
					"UTF-8");
			int eventType = xpp.getEventType();
			String tagName;

			// various stacks to maintain state while walking through the xml
			// tree
			Stack<String> tagStack = new Stack<String>();
			Survey curSurvey = null; // valid only within a survey, null
										// otherwise
			Vector<SurveyPrompt> prompts = new Vector<SurveyPrompt>(); // valid
																		// only
																		// within
																		// a
																		// survey,
																		// empty
																		// otherwise
			Vector<JSONObject> properties = new Vector<JSONObject>(); // valid
																		// only
																		// within
																		// a
																		// prompt,
																		// empty
																		// otherwise

			// iterate through the xml, paying attention only to surveys and
			// prompts
			// note that this does no validation outside of preventing itself
			// from crashing catastrophically
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					tagName = xpp.getName();
					tagStack.push(tagName);

					if (tagName.equalsIgnoreCase("survey")) {
						if (curSurvey != null)
							throw new XmlPullParserException(
									"encountered a survey tag inside another survey tag");

						curSurvey = new Survey();
						curSurvey.mCampaignUrn = campaignUrn;
					}
					else if (tagName.equalsIgnoreCase("prompt")) {
						SurveyPrompt sp = new SurveyPrompt();
						// FIXME: add the campaign + survey ID to make lookups
						// easier?
						prompts.add(sp);
					}
					else if (tagName.equalsIgnoreCase("property")) {
						properties.add(new JSONObject());
					}
				}
				else if (eventType == XmlPullParser.TEXT) {
					if (tagStack.size() >= 2) {
						// we may be in an entity>property situation, so check
						// and assign accordingly
						if (tagStack.get(tagStack.size() - 2).equalsIgnoreCase(
								"survey")) {
							// populating the current survey object with its
							// properties here
							if (tagStack.peek().equalsIgnoreCase("id"))
								curSurvey.mSurveyID = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase("title"))
								curSurvey.mTitle = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase(
									"description"))
								curSurvey.mDescription = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase(
									"submitText"))
								curSurvey.mSubmitText = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase(
									"introText"))
								curSurvey.mIntroText = xpp.getText();
							else if (tagStack.peek()
									.equalsIgnoreCase("anytime"))
								curSurvey.mAnytime = xpp.getText().equals(
										"true") ? true : false;
						}
						else if (tagStack.get(tagStack.size() - 2)
								.equalsIgnoreCase("prompt")) {
							SurveyPrompt sp = prompts.lastElement();

							// populating the last encountered survey prompt
							// with its properties here
							if (tagStack.peek().equalsIgnoreCase("id"))
								sp.mPromptID = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase(
									"promptText"))
								sp.mPromptText = xpp.getText();
							else if (tagStack.peek().equalsIgnoreCase(
									"promptType"))
								sp.mPromptType = xpp.getText();
						}
						else if (tagStack.get(tagStack.size() - 2)
								.equalsIgnoreCase("property")) {
							JSONObject curProperty = properties.lastElement();

							// populating the last encountered property
							if (tagStack.peek().equalsIgnoreCase("key"))
								curProperty.put("key", xpp.getText());
							else if (tagStack.peek().equalsIgnoreCase("label"))
								curProperty.put("label", xpp.getText());
							else if (tagStack.peek().equalsIgnoreCase("value"))
								curProperty.put("value", xpp.getText());
						}
					}
				}
				else if (eventType == XmlPullParser.END_TAG) {
					tagName = xpp.getName();
					tagStack.pop();

					if (tagName.equalsIgnoreCase("survey")) {
						// store the current survey to the database
						long surveyPID = db.insert(Tables.SURVEYS, null,
								curSurvey.toCV());

						// also store all the prompts we accumulated for it
						for (SurveyPrompt sp : prompts) {
							sp.mSurveyID = curSurvey.mSurveyID;
							sp.mSurveyPID = surveyPID;
							sp.mCompositeID = curSurvey.mCampaignUrn + ":"
									+ curSurvey.mSurveyID;
							db.insert(Tables.SURVEY_PROMPTS, null, sp.toCV());
						}

						// flush the prompts we've stored up so far
						prompts.clear();

						// and clear us from being in any survey
						curSurvey = null;
					}
					else if (tagName.equalsIgnoreCase("prompt")) {
						SurveyPrompt sp = prompts.lastElement();

						// update the current prompt with the collected
						// properties
						JSONArray propertyArray = new JSONArray();

						for (JSONObject property : properties)
							propertyArray.put(property);

						// encode it as json and stuff it in the surveyprompt
						sp.mProperties = propertyArray.toString();

						// and wipe the properties
						properties.clear();
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
		catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean populatePromptsFromResponseJSON(SQLiteDatabase db, long responseRowID, String response, String campaignUrn, String surveyId) {
		try {
			// create a list of metadata for this survey from the surveyprompts table
			// this will help in remapping values for single and multichoice prompts, etc.
			List<SurveyPrompt> promptsList = SurveyPrompt.fromCursor(
						db.query(Tables.SURVEY_PROMPTS, null, SurveyPrompts.COMPOSITE_ID + "='" + campaignUrn + ":" + surveyId + "'", null, null, null, null)
					);

			JSONArray responseData = new JSONArray(response);

			HashMap<String, JSONObject> promptsMap = new HashMap<String, JSONObject>();
			
			for (int i = 0; i < responseData.length(); ++i) {
				// nab the jsonobject, which contains "prompt_id" and "value"
				JSONObject item = responseData.getJSONObject(i);
				
				// if the entry we're looking at doesn't include prompt_id or value, continue
				if (!item.has("prompt_id") || !item.has("value"))
					continue;
				
				promptsMap.put(item.getString("prompt_id"), item);
			}

			for (SurveyPrompt promptData : promptsList) {

				// nab the jsonobject, which contains "prompt_id" and "value"
				JSONObject item = promptsMap.get(promptData.mPromptID);

				// construct a new PromptResponse object to populate
				PromptResponse p = new PromptResponse();

				p.mCompositeID = campaignUrn + ":" + surveyId;
				p.mResponseID = responseRowID;
				p.mPromptID = item.getString("prompt_id");

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
				else if (promptData.mPromptType.equalsIgnoreCase("single_choice")) {
					// unload the json properties
					JSONArray values = new JSONArray(promptData.mProperties);
					// set the explicit value as the default; if we don't find a match, it'll end up as this
					p.mValue = item.getString("value");

					// search for a key that matches the given value
					for (int ir = 0; ir < values.length(); ++ir) {
						JSONObject entry = values.getJSONObject(ir);
						if (entry.getString("key").equals(p.mValue)) {
							p.mValue = entry.getString("label");
							p.mExtraValue = item.getString("value");
							break;
						}
					}
				}
				else if (promptData.mPromptType.equalsIgnoreCase("multi_choice")) {
					// same procedure as above, except that we need to remap every value

					try {
						// unload the json properties
						JSONArray values = new JSONArray(promptData.mProperties);
						// set the explicit value as the default; if we don't find a match, it'll end up as this
						JSONArray newValues = new JSONArray(item.getString("value"));

						// for each entry in newValues...
						for (int io = 0; io < newValues.length(); ++io) {
							// search for a key that matches the given value
							for (int ir = 0; ir < values.length(); ++ir) {
								JSONObject entry = values.getJSONObject(ir);
								if (entry.getString("key").equals(newValues.getString(io))) {
									// assign the remapped value to this index
									newValues.put(io, entry.getString("label"));
									break;
								}
							}
						}

						// and reassign mValue here
						p.mValue = newValues.toString();
						p.mExtraValue = item.getString("value");
					}
					catch (JSONException e) {
						// it wasn't a json array, so just remap the value
						p.mValue = item.getString("value");
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
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Swaps newCursor into the given adapter and closes the old cursor if one exists 
	 * @param adapter the adapter into which to swap the new cursor
	 * @param newCursor the cursor to swap into the adapter
	 */
	public static void swapCursorSafe(CursorAdapter adapter, Cursor newCursor) {
		Cursor oldCursor = adapter.swapCursor(newCursor);
		
		if (oldCursor != null && !oldCursor.isClosed())
			oldCursor.close();
	}
}
