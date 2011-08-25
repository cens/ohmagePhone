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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.OhmageApi.ImageReadResponse;
import org.ohmage.OhmageApi.Result;
import org.ohmage.OhmageApi.SurveyReadResponse;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.prompt.photo.PhotoPrompt;

import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.systemlog.Log;

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

	@SuppressWarnings("unchecked")
	@Override
	protected void doWakefulWork(Intent intent) {
		// for the time being, we just pull all the surveys and update our feedback cache with them
		// FIXME: in the future, we should only download what we need...two strategies for that:
		// 1) maintain a timestamp of the most recent refresh and request only things after it
		// 2) somehow figure out which surveys the server has and we don't via the hashcode and sync accordingly
		
		Log.v(TAG, "Feedback service starting");
		
		if (!SharedPreferencesHelper.ALLOWS_FEEDBACK) {
			Log.e(TAG, "Feedback service aborted, because feedback is not allowed in the preferences");
			return;
		}
		
		// ==================================================================
		// === 1. acquire handles to api and database, build campaign list
		// ==================================================================
		
		// grab an instance of the api connector so we can do calls to the server for responses
		OhmageApi api = new OhmageApi(this);
		SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);
		String username = prefs.getUsername();
		String hashedPassword = prefs.getHashedPassword();
		
		// open the db so we can read which campaigns we're in and populate the response cache for them
		DbHelper dbHelper = new DbHelper(this);
        List<Campaign> campaigns = dbHelper.getCampaigns();
        
		// helper instance for parsing utc timestamps
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat inputSDF = new SimpleDateFormat("yyyy-MM-dd");
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
        	campaigns = dbHelper.getCampaigns();
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
		
/*		Calendar twoWeeksAgo = new GregorianCalendar();
		twoWeeksAgo.add(Calendar.WEEK_OF_YEAR, -2);
		
		Calendar tomorrow = new GregorianCalendar();
		tomorrow.add(Calendar.DAY_OF_MONTH, 1);
		
		// and convert times to timestamps we can feed to the api
		startDate = inputSDF.format(twoWeeksAgo.getTime());
		endDate = inputSDF.format(tomorrow.getTime());*/
		
		// ==================================================================
		// === 3. download responses for each campaign and insert into fbdb
		// ==================================================================

		// we'll have to iterate through all the campaigns in which this user
		// is participating in order to gather all of their data
		for (Campaign c : campaigns) {
			Log.v(TAG, "Requesting responses for campaign " + c.mUrn + "...");
			
			SurveyReadResponse apiResponse = api.surveyResponseRead(SharedPreferencesHelper.DEFAULT_SERVER_URL, username, hashedPassword, "android", c.mUrn, username, null, null, "json-rows", startDate, endDate);
			
			// check if it was successful or not
			String error = null;
			switch (apiResponse.getResult()) {
				case FAILURE:			error = "survey response query failed"; break;
				case HTTP_ERROR:		error = "http error during request"; break;
				case INTERNAL_ERROR:	error = "internal error during request"; break;
			}
			
			if (error != null) {
				Log.e(TAG, error);
				return;
			}
			
			Log.v(TAG, "Request for campaign " + c.mUrn + " complete!");
			
	        // dump all remote and local uploaded records before we start.
	        // they'll be replaced with fresh copies from the server.
	        // it's not critical if this fails, since duplicates will never be
	        // inserted into the db thanks to the hashcode unique index.
            int staleResponses = dbHelper.removeStaleResponseRows(c.mUrn);
            if (staleResponses >= 0)
            	Log.v(TAG, "Removed " + staleResponses + " stale response(s) for campaign " + c.mUrn + " prior to downloading");
            else
            	Log.e(TAG, "Error occurred when removing stale responses");
		
			// gather our survey response array from the response
			JSONArray data = apiResponse.getData();
			
			// also maintain a list of photo UUIDs that may or may not be on the device
			// this is campaign-specific, which is why it's happening in this loop over the campaigns
			ArrayList<String> photoUUIDs = new ArrayList<String>();
			
			// if they have nothing on the server, data may be null
			// if that's the case, don't do anything
			if (data == null) {
				Log.v(TAG, "No data to process, continuing feedback service");
				continue;
			}
			
			// create a little histogram to add up number of responses for each survey
			/*Map<String,Integer> responsesPerSurvey = new HashMap<String,Integer>();*/
			
			// for each survey, insert a record into our feedback db
			// if we're unable to insert, just continue (likely a duplicate)
			// also, note the schema follows the definition in the documentation
			for (int i = 0; i < data.length(); ++i) {
				Log.v(TAG, "Processing record " + (i+1) + "/" + data.length());
				
				try {
					JSONObject survey = data.getJSONObject(i);
					String surveyId = survey.getString("survey_id");
					
					/*// get the count for this time, creating it if it doesn't exist already
					int currentSurveyCount;
					
					if (responsesPerSurvey.containsKey(surveyId)) {
						currentSurveyCount = responsesPerSurvey.get(surveyId);
					}
					else
					{
						responsesPerSurvey.put(surveyId, 0);
						currentSurveyCount = 0;
					}
					
					// ensure that we haven't exceeded the limit for responses of this survey type
					// if we have, continue, since the next response may not be a response of this survey type
					if (currentSurveyCount >= MAX_RESPONSES_PER_SURVEY)
						break;
					
					// update the count for next times
					responsesPerSurvey.put(surveyId, currentSurveyCount+1);*/
					
					// first we need to gather all of the appropriate data
					// from the survey response. some of this data needs to
					// be transformed to match the format that SurveyActivity
					// uploads/broadcasts, since our survey responses can come
					// from either source and need to be stored the same way.
					String date = survey.getString("timestamp");
					String timezone = survey.getString("timezone");
					sdf.setTimeZone(TimeZone.getTimeZone(timezone));
					long time = sdf.parse(date).getTime();
					
					// much of the location data is optional, hence the "opt*()" calls
					String locationStatus = survey.getString("location_status");
					double locationLatitude = survey.optDouble("latitude");
					double locationLongitude = survey.optDouble("longitude");
					String locationProvider = survey.optString("location_provider");
					float locationAccuracy = (float)survey.optDouble("location_accuracy");
					long locationTime = survey.optLong("location_timestamp");
					
					String surveyLaunchContext = survey.getString("launch_context_long");
					
					// we need to parse out the responses and put them in
					// the same format as what we collect from the local activity
					JSONObject inputResponses = survey.getJSONObject("responses");
					
					// iterate through inputResponses and create a new JSON object of prompt_ids and values
					JSONArray responseJson = new JSONArray();
					Iterator<String> keys = inputResponses.keys();
					while (keys.hasNext()) {
						// for each prompt response, create an object with a prompt_id/value pair
						String key = keys.next();
						JSONObject curItem = inputResponses.getJSONObject(key);
						
						// FIXME: deal with repeatable sets here someday, although i'm not sure how
						// how do we visualize them on a line graph along with regular points? scatter chart?
						
						if (curItem.has("prompt_response")) {
							JSONObject newItem = new JSONObject();
							
							try {
								String value = curItem.getString("prompt_response");
								String type = curItem.getString("prompt_type");
								newItem.put("prompt_id", key);
								newItem.put("value", value);
								
								// also enter the custom_choices data if the type supports custom choices
								// and if the custom choice data is actually there (e.g. in the glossary for the prompt)
								if ((type.equals("single_choice_custom") || type.equals("multi_choice_custom"))
									&& curItem.has("prompt_choice_glossary"))
								{
									// unfortunately, the glossary is in a totally different format than
									// what the survey returns; we can't just store it directly.
									// we have to reformat the glossary entries to be of the following form:
									// [{"choice_value": "Exercise", "choice_id": 1}, etc.]
									
									JSONArray customChoiceArray = new JSONArray();
									JSONObject glossary = curItem.getJSONObject("prompt_choice_glossary");
									
									// create an iterator over the glossary so we can extract the keys + "label" value
									Iterator<String> glossaryKeys = glossary.keys();
									
									while (glossaryKeys.hasNext()) {
										// grab the glossary key and its corresponding element
										String glossaryKey = glossaryKeys.next();
										JSONObject curGlossaryItem = glossary.getJSONObject(glossaryKey);
										
										// create a new object that remaps the values from the glossary
										// to the custom choices format
										JSONObject newChoiceItem = new JSONObject();
										newChoiceItem.put("choice_value", curGlossaryItem.getString("label"));
										newChoiceItem.put("choice_id", glossaryKey);
										
										// and add it to our custom choices array
										customChoiceArray.put(newChoiceItem);
									}
									
									// put our newly reformatted custom choices array into the object, too
									newItem.put("custom_choices", customChoiceArray);
								}
								
								// if it's a photo, put its value (the photo's UUID) into the photoUUIDs list
								if (curItem.getString("prompt_type").equalsIgnoreCase("photo") && !value.equalsIgnoreCase("NOT_DISPLAYED")) {
									photoUUIDs.add(value);
								}
							} catch (JSONException e) {
								Log.e(TAG, "JSONException when trying to generate response json", e);
								throw new JSONException("error generating response json");
							}
							
							responseJson.put(newItem);
						}
					}
					
					// render it to a string for storage into our db
					String response = responseJson.toString();
					
					// ok, gathered everything; time to insert into the feedback DB
					// note that we mark this entry as "remote", meaning it came from the server
					
					if (dbHelper.addResponseRow(
							c.mUrn,
							username,
							date,
							time,
							timezone,
							locationStatus,
							locationLatitude,
							locationLongitude,
							locationProvider,
							locationAccuracy,
							locationTime,
							surveyId,
							surveyLaunchContext,
							response,
							"remote") == -1) {
						// display some note in the log that this failed
						Log.v(TAG, "Record " + (i+1) + "/" + data.length() + " was not inserted (insertion returned -1)");
					}
					
					// it's possible that the above will fail, in which case it silently returns -1
					// we don't do anything differently in that case, so there's no need to check
				}
				catch(ParseException e) {
					// this is a date parse exception, likely thrown from where we parse the utc timestamp
					Log.e(TAG, "Problem parsing survey response timestamp", e);
					continue;
				}
		        catch (JSONException e) {
					Log.e(TAG, "Problem parsing response json: " + e.getMessage(), e);
					continue;
				}
			}

			// ==================================================================
			// === 3b. download image data mentioned in responses (disabled for now)
			// ==================================================================
			
			// now that we're done inserting all that data from the server
			// let's see if we already have all the photos that were mentioned in the responses
			/*
			if (photoUUIDs.size() > 0) {
				// get the image directory for this campaign and ensure it exists
				File photoDir = new File(PhotoPrompt.IMAGE_PATH + "/" + c.mUrn.replace(':', '_'));
				photoDir.mkdirs();
				
				for (String photoUUID : photoUUIDs) {
					// check if it doesn't already exist in our photos directory
					File photo = new File(photoDir, photoUUID + ".jpg");
					
					Log.v(TAG, "Checking photo w/UUID " + photoUUID + "...");
					
					if (!photo.exists()) {
						// it doesn't exist, so we have to download it :(
						ImageReadResponse ir = api.imageRead(SharedPreferencesHelper.DEFAULT_SERVER_URL, username, hashedPassword, "android", c.mUrn, username, photoUUID, null);
					
						// if it succeeded, it contains data that we should save as the photo file above
						try {
							if (ir != null && ir.getResult() == Result.SUCCESS) {
								photo.createNewFile();
								FileOutputStream photoWriter = new FileOutputStream(photo);
								photoWriter.write(ir.getData());
								photoWriter.close();
								
								Log.v(TAG, "Downloaded photo w/UUID " + photoUUID);
							}
							else
								Log.e(TAG, "Unable to save photo w/UUID " + photoUUID + ": " + ir.getResult().toString());
						}
						catch (IOException e) {
							Log.e(TAG, "Unable to save photo w/UUID " + photoUUID, e);
							return;
						}	
					}
					else
						Log.v(TAG, "Photo w/UUID " + photoUUID + " already exists");
				}
			}
			*/
			
			// done with this campaign! on to the next one...
		}
		
		// ==================================================================
		// === 4. complete! save finish time and exit
		// ==================================================================
		
		// once we're completely done, it's safe to store the time at which this refresh happened.
		// this is to ensure that we don't incorrectly flag the range between the last and current
		// as completed in the case that there's an error mid-way through.
		prefs.putLastFeedbackRefreshTimestamp(thisRefresh);
		
		Log.v(TAG, "Feedback service complete");
	}
	
	public static boolean ensurePhotoExists(Context context, String campaignUrn, String photoUUID) {
		// get the image directory for this campaign and ensure it exists
		File photoDir = new File(PhotoPrompt.IMAGE_PATH + "/" + campaignUrn.replace(':', '_'));
		photoDir.mkdirs();

		// check if it doesn't already exist in our photos directory
		File photo = new File(photoDir, photoUUID + ".jpg");
		
		if (!photo.exists()) {
			// it doesn't exist, so we have to download it :(
			
			// assemble all the resources to connect to the server
			// and then do so!
			OhmageApi api = new OhmageApi(context);
			SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
			String username = prefs.getUsername();
			String hashedPassword = prefs.getHashedPassword();
			
			ImageReadResponse ir = api.imageRead(SharedPreferencesHelper.DEFAULT_SERVER_URL, username, hashedPassword, "android", campaignUrn, username, photoUUID, null);
		
			// if it succeeded, it contains data that we should save as the photo file above
			try {
				if (ir != null && ir.getResult() == Result.SUCCESS) {
					photo.createNewFile();
					FileOutputStream photoWriter = new FileOutputStream(photo);
					photoWriter.write(ir.getData());
					photoWriter.close();
					
					return true; // we downloaded it successfuly
				}
				else
					return false; // we were unable to download it for some reason
			}
			catch (IOException e) {
				return false; // something went wrong while downloading it
			}	
		}
		
		return true; // it was already there!
	}
}
