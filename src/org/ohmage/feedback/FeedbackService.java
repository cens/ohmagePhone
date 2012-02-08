package org.ohmage.feedback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.codehaus.jackson.JsonNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.Config;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.StreamingResponseListener;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.OhmageApi.Result;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.prompt.AbstractPrompt;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.systemlog.Log;
import java.util.HashMap;

public class FeedbackService extends WakefulIntentService {
	private static final String TAG = "FeedbackService";
	private static final int MAX_RESPONSES_PER_SURVEY = 20;

	public FeedbackService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		// for the time being, we just pull all the surveys and update our feedback cache with them
		// FIXME: in the future, we should only download what we need...two strategies for that:
		// 1) maintain a timestamp of the most recent refresh and request only things after it
		// 2) somehow figure out which surveys the server has and we don't via the hashcode and sync accordingly
		
		Log.v(TAG, "Feedback service starting");
		
		if (!Config.ALLOWS_FEEDBACK) {
			Log.e(TAG, "Feedback service aborted, because feedback is not allowed in the preferences");
			return;
		}
		
		// ==================================================================
		// === 1. acquire handles to api and database, build campaign list
		// ==================================================================
		
		// grab an instance of the api connector so we can do calls to the server for responses
		OhmageApi api = new OhmageApi();
		SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);
		String username = prefs.getUsername();
		String hashedPassword = prefs.getHashedPassword();
		
		if (username == null || username.equals("")) {
			Log.e(TAG, "User isn't logged in, FeedbackService terminating");
			return;
		}
		
		// get a ref to the dbhelper for maintenance funcs that should be moved into the provider someday
		// grab a contentresolver for proper db queries 
		DbHelper dbHelper = new DbHelper(this);
		final ContentResolver cr = getContentResolver();
		// and also create a list to hold some campaigns
		List<Campaign> campaigns;
        
		// helper instance for parsing utc timestamps
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setLenient(false);

        // if we received a campaign_urn in the intent, only download the data for that one campaign.
    	// the campaign object we create only inclues the mUrn field since we don't use anything else.
        if (intent.hasExtra("campaign_urn")) {
        	campaigns = new ArrayList<Campaign>();
        	Campaign candidate = new Campaign();
        	candidate.mUrn = intent.getStringExtra("campaign_urn");
        	campaigns.add(candidate);
        }
        else {
        	// otherwise, do all the campaigns
        	// don't consider the ones that are remote
        	Cursor campaignCursor = cr.query(Campaigns.CONTENT_URI, null, Campaigns.CAMPAIGN_STATUS + "!=" + Campaign.STATUS_REMOTE, null, null);

    		campaigns = Campaign.fromCursor(campaignCursor);
        }

		// ==================================================================
		// === 2. determine time range on which to query
		// ==================================================================

		// get the time since the last refresh so we can retrieve entries only since then.
		// also save the time of this refresh, but only put it into the prefs at the end
		// long lastRefresh = prefs.getLastFeedbackRefreshTimestamp();
		long thisRefresh = System.currentTimeMillis();
        
		// attempt to construct a date range on which to query, if one exists
		// this will be from two weeks ago to tomorrow
		String startDate = null;
		String endDate = null;
		
		/*
		SimpleDateFormat inputSDF = new SimpleDateFormat("yyyy-MM-dd");
		Calendar twoWeeksAgo = new GregorianCalendar();
		twoWeeksAgo.add(Calendar.WEEK_OF_YEAR, -2);
		
		Calendar tomorrow = new GregorianCalendar();
		tomorrow.add(Calendar.DAY_OF_MONTH, 1);
		
		// and convert times to timestamps we can feed to the api
		startDate = inputSDF.format(twoWeeksAgo.getTime());
		endDate = inputSDF.format(tomorrow.getTime());
		*/
		
		// ==================================================================
		// === 3. download responses for each campaign and insert into fbdb
		// ==================================================================

		// we'll have to iterate through all the campaigns in which this user
		// is participating in order to gather all of their data
		for (final Campaign c : campaigns) {
			Log.v(TAG, "Requesting responses for campaign " + c.mUrn + "...");
			
			// dump all remote and local uploaded records before we start.
	        // they'll be replaced with fresh copies from the server.
	        // it's not critical if this fails, since duplicates will never be
	        // inserted into the db thanks to the hashcode unique index.
            int staleResponses = dbHelper.removeStaleResponseRows(c.mUrn);
            if (staleResponses >= 0)
            	Log.v(TAG, "Removed " + staleResponses + " stale response(s) for campaign " + c.mUrn + " prior to downloading");
            else
            	Log.e(TAG, "Error occurred when removing stale responses");
			
			// also maintain a list of photo UUIDs that may or may not be on the device
			// this is campaign-response-specific, which is why it's happening in this loop over the campaigns
			final HashMap<String, ArrayList<String>> responsePhotos = new HashMap<String, ArrayList<String>>();

			// do the call and process the streaming response data
			api.surveyResponseRead(Config.DEFAULT_SERVER_URL, username, hashedPassword, "android", c.mUrn, username, null, null, "json-rows", true, startDate, endDate,
				new StreamingResponseListener() {
					int curRecord;
					
					@Override
					public void onPreReadObject() {
						Log.v(TAG, "Beginning record read...");
						curRecord = 0;
					}
					
					@Override
					public void readObject(JsonNode survey) {
						// deal with the elements we read via stream parsing here
						Log.v(TAG, "Processing record " + ((curRecord++)+1) + " in " + c.mUrn + "...");
						
						// for each survey, insert a record into our feedback db
						// if we're unable to insert, just continue (likely a duplicate)
						// also, note the schema follows the definition in the documentation
						
						// store all the photos found in this thing, too
						ArrayList<String> photoUUIDs = new ArrayList<String>();
						
						try {
							// create an instance of a response to hold the data we're going to insert
							Response candidate = new Response();
							
							// we need to gather all of the appropriate data
							// from the survey response. some of this data needs to
							// be transformed to match the format that SurveyActivity
							// uploads/broadcasts, since our survey responses can come
							// from either source and need to be stored the same way.
							candidate.uuid = survey.get("survey_key").asText();
							candidate.surveyId = survey.get("survey_id").asText();
							candidate.campaignUrn = c.mUrn;
							candidate.username = survey.get("user").asText();
							candidate.date = survey.get("timestamp").asText();
							candidate.timezone = survey.get("timezone").asText();
							candidate.time = survey.get("time").asLong();
							
							// much of the location data is optional, hence the "opt*()" calls
							candidate.locationStatus = survey.get("location_status").asText();
							candidate.locationLatitude = survey.path("latitude").asDouble();
							candidate.locationLongitude = survey.path("longitude").asDouble();
							candidate.locationProvider = survey.path("location_provider").asText();
							candidate.locationAccuracy = (float)survey.path("location_accuracy").asDouble();
							candidate.locationTime = survey.path("location_timestamp").asLong();
							
							candidate.surveyLaunchContext = survey.get("launch_context_long").asText();
							
							// we need to parse out the responses and put them in
							// the same format as what we collect from the local activity
							JsonNode inputResponses = survey.get("responses");
							
							// iterate through inputResponses and create a new JSON object of prompt_ids and values
							JSONArray responseJson = new JSONArray();
							Iterator<String> keys = inputResponses.getFieldNames();
							
							while (keys.hasNext()) {
								// for each prompt response, create an object with a prompt_id/value pair
								String key = keys.next();
								JsonNode curItem = inputResponses.get(key);
								
								// FIXME: deal with repeatable sets here someday, although i'm not sure how
								// how do we visualize them on a line graph along with regular points? scatter chart?
								
								if (curItem.has("prompt_response")) {
									JSONObject newItem = new JSONObject();
									
									try {
										String value = curItem.get("prompt_response").asText();
										String type = curItem.get("prompt_type").asText();
										newItem.put("prompt_id", key);
	
										// also enter the custom_choices data if the type supports custom choices
										// and if the custom choice data is actually there (e.g. in the glossary for the prompt)
										if (curItem.has("prompt_choice_glossary")) {
											if (type.equals("single_choice_custom") || type.equals("multi_choice_custom"))
											{
												// unfortunately, the glossary is in a totally different format than
												// what the survey returns; we can't just store it directly.
												// we have to reformat the glossary entries to be of the following form:
												// [{"choice_value": "Exercise", "choice_id": 1}, etc.]
												
												JSONArray customChoiceArray = new JSONArray();
												JsonNode glossary = curItem.get("prompt_choice_glossary");
												
												// create an iterator over the glossary so we can extract the keys + "label" value
												Iterator<String> glossaryKeys = glossary.getFieldNames();
												
												while (glossaryKeys.hasNext()) {
													// grab the glossary key and its corresponding element
													String glossaryKey = glossaryKeys.next();
													JsonNode curGlossaryItem = glossary.get(glossaryKey);
													
													// create a new object that remaps the values from the glossary
													// to the custom choices format
													JSONObject newChoiceItem = new JSONObject();
													newChoiceItem.put("choice_value", curGlossaryItem.get("label").asText());
													newChoiceItem.put("choice_id", glossaryKey);
													
													// and add it to our custom choices array
													customChoiceArray.put(newChoiceItem);
												}
												
												// put our newly reformatted custom choices array into the object, too
												newItem.put("custom_choices", customChoiceArray);
											}
										}
										
										// if it's a photo, put its value (the photo's UUID) into the photoUUIDs list
										if (curItem.get("prompt_type").asText().equalsIgnoreCase("photo") && !value.equalsIgnoreCase(AbstractPrompt.NOT_DISPLAYED_VALUE) && !value.equalsIgnoreCase(AbstractPrompt.SKIPPED_VALUE)) {
											photoUUIDs.add(value);
										}
										
										// add the value, which is generally just a number
										newItem.put("value", value);
										
									} catch (JSONException e) {
										Log.e(TAG, "JSONException when trying to generate response json", e);
										throw new JSONException("error generating response json");
									}
									
									responseJson.put(newItem);
								}
							}
							
							// render it to a string for storage into our db
							candidate.response = responseJson.toString();
							candidate.status = Response.STATUS_DOWNLOADED;
							
							// ok, gathered everything; time to insert into the feedback DB
							// note that we mark this entry as "remote", meaning it came from the server
	
							try {
								Uri responseUri = cr.insert(Responses.CONTENT_URI, candidate.toCV());
								responsePhotos.put(Responses.getResponseId(responseUri), photoUUIDs);
							}
							catch(Exception e) {
								// display some note in the log that this failed
								Log.v(TAG, "Record was not inserted (insertion returned -1)");
							}
							
							// it's possible that the above will fail, in which case it silently returns -1
							// we don't do anything differently in that case, so there's no need to check
						}
				        catch (JSONException e) {
							Log.e(TAG, "Problem parsing response json: " + e.getMessage(), e);
						}
					}
					
					@Override
					public void readResult(Result result, String[] errorCodes) {
						// what do we do if there's an error? terminate immediately or just keep trucking?
						// it also doesn't help that this will always occur after the data is read, assuming it's there
						String error = null;
						
						switch (result) {
							case FAILURE:			error = "survey response query failed";
							case HTTP_ERROR:		error = "http error during request";
							case INTERNAL_ERROR:	error = "internal error during request";
						}
						
						if (error != null) {
							Log.e(TAG, error);
							
							// throw an exception here or what?
						}
					}
				});
		}
		
		// ==================================================================
		// === 4. complete! save finish time and exit
		// ==================================================================
		
		// once we're completely done, it's safe to store the time at which this refresh happened.
		// this is to ensure that we don't incorrectly flag the range between the last and current
		// as 'completed', in the case that there's an error mid-way through.
		prefs.putLastFeedbackRefreshTimestamp(thisRefresh);
		
		Log.v(TAG, "Feedback service complete");
	}
}
