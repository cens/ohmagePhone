package edu.ucla.cens.andwellness.feedback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.andwellness.AndWellnessApi;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.AndWellnessApi.SurveyReadResponse;
import edu.ucla.cens.andwellness.db.Campaign;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.systemlog.Log;

import android.content.Intent;
import android.widget.Toast;

public class FeedbackService extends WakefulIntentService {
	private static final String TAG = "FeedbackService";

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

		Toast.makeText(this, "beginning fb sync...", Toast.LENGTH_SHORT).show();
		
		AndWellnessApi api = new AndWellnessApi(this);
		SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);
		String username = prefs.getUsername();
		String hashedPassword = prefs.getHashedPassword();
		
		// helper instance for parsing utc timestamps
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setLenient(false);
		
		// opening two databases here:
		// the standard db which lists the users' campaigns...
		DbHelper dbHelper = new DbHelper(this);
        List<Campaign> campaigns = dbHelper.getCampaigns();
        // ...and the feedback database into which we'll be inserting their responses (if necessary)
        FeedbackDatabase fbDB = new FeedbackDatabase(this);
        
		// we'll have to iterate through all the campaigns in which this user
		// is participating in order to gather all of their data
		for (Campaign c : campaigns) {
			SurveyReadResponse apiResponse = api.surveyResponseRead(SharedPreferencesHelper.DEFAULT_SERVER_URL, username, hashedPassword, "android", c.mUrn, username, null, null, "json-rows", null, null);
			
			// check if it was successful or not
			switch (apiResponse.getResult()) {
				case FAILURE:
					Toast.makeText(this, "survey response query failed", Toast.LENGTH_LONG).show();
					return;
				case HTTP_ERROR:
					Toast.makeText(this, "http error during request", Toast.LENGTH_LONG).show();
					return;
				case INTERNAL_ERROR:
					Toast.makeText(this, "internal error during request", Toast.LENGTH_LONG).show();
					return;
				case SUCCESS:
					Toast.makeText(this, "survey response read, continuing", Toast.LENGTH_SHORT).show();
			}
		
			// gather our survey response array from the response
			JSONArray data = apiResponse.getData();
			
			// if they have nothing on the server, data may be null
			// if that's the case, don't do anything
			if (data == null) {
				// Toast.makeText(this, "no data on server, exiting", Toast.LENGTH_SHORT).show();
				return;
			}
			
			// for each survey, insert a record into our feedback db
			// if we're unable to insert, just continue (likely a duplicate)
			// also, note the schema follows the definition in the documentation
			for (int i = 0; i < data.length(); ++i) {
				try {
					JSONObject survey = data.getJSONObject(i);
					
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
					
					String surveyId = survey.getString("survey_id");
					String surveyLaunchContext = survey.getString("launch_context_long").toString();
					
					// we need to parse out the responses and put them in
					// the same format as what we collect from the local activity
					JSONObject inputResponses = survey.getJSONObject("responses");
					
					// iterate through inputResponses and create a new JSON object of prompt_ids and values
					JSONArray responseJson = new JSONArray();
					Iterator<String> keys = inputResponses.keys();
					while (keys.hasNext()) {
						// for each prompt response, create an object with a prompt_id/value pair
						// FIXME: ignoring the "custom_fields" field for now, since it's unused
						String key = keys.next();
						JSONObject curItem = inputResponses.getJSONObject(key);
						JSONObject newItem = new JSONObject();
						try {
							newItem.put("prompt_id", key);
							newItem.put("value", curItem.getString("prompt_response"));
						} catch (JSONException e) {
							Log.e(TAG, "JSONException when trying to generate response json", e);
							throw new RuntimeException(e);
						}
						responseJson.put(newItem);
					}
					// render it to a string for storage into our db
					String response = responseJson.toString();
					
					// ok, gathered everything; time to insert into the feedback DB
					// note that we mark this entry as "remote", meaning it came from the server
					fbDB.addResponseRow(
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
							"remote");
					
					// it's possible that the above will fail, in which case it silently returns -1
					// we don't do anything differently in that case, so there's no need to check
				}
				catch(ParseException e) {
					// this is a date parse exception, likely thrown from where we parse the utc timestamp
					Log.e(TAG, "Problem parsing survey response timestamp", e);
					continue;
				}
		        catch (JSONException e) {
					Log.e(TAG, "Problem parsing response json", e);
					continue;
				}
			}
		}
		
		// done!
		Toast.makeText(this, "Sync done!", Toast.LENGTH_LONG).show();
	}
}
