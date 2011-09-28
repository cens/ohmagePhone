package org.ohmage.service;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.Result;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.SurveyPrompts;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class UploadService extends WakefulIntentService {
	
	private static final String TAG = "UploadService";

	public UploadService() {
		super(TAG);
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		
		String serverUrl = SharedPreferencesHelper.DEFAULT_SERVER_URL;
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		String username = helper.getUsername();
		String hashedPassword = helper.getHashedPassword();
		
		OhmageApi api = new OhmageApi(this);
		DbHelper dbHelper = new DbHelper(this);
		
		Uri dataUri = intent.getData();
				
		ContentResolver cr = getContentResolver();
		
		String [] projection = new String [] {
										Tables.RESPONSES + "." + Responses._ID,
										Responses.RESPONSE_DATE,
										Responses.RESPONSE_TIME,
										Responses.RESPONSE_TIMEZONE,
										Responses.RESPONSE_LOCATION_STATUS,
										Responses.RESPONSE_LOCATION_LATITUDE,
										Responses.RESPONSE_LOCATION_LONGITUDE,
										Responses.RESPONSE_LOCATION_PROVIDER,
										Responses.RESPONSE_LOCATION_ACCURACY,
										Responses.RESPONSE_LOCATION_TIME,
										Tables.RESPONSES + "." + Responses.SURVEY_ID,
										Responses.RESPONSE_SURVEY_LAUNCH_CONTEXT,
										Responses.RESPONSE_JSON,
										Tables.RESPONSES + "." + Responses.CAMPAIGN_URN,
										Campaigns.CAMPAIGN_CREATED};
		
		String select = intent.getStringExtra("select");
		
		Cursor cursor = cr.query(dataUri, projection, select, null, null);

		cursor.moveToFirst();
		
//		ContentValues cv = new ContentValues();
//		cv.put(Tables.RESPONSES + "." + Response.STATUS, Response.STATUS_QUEUED);
//		cr.update(dataUri, cv, select, null);
		
		for (int i = 0; i < cursor.getCount(); i++) {
			
			long responseId = cursor.getLong(cursor.getColumnIndex(Responses._ID));
			
			ContentValues values = new ContentValues();
			values.put(Responses.RESPONSE_STATUS, Response.STATUS_UPLOADING);
			cr.update(Responses.CONTENT_URI, values, Tables.RESPONSES + "." + Responses._ID + "=" + responseId, null);
			
			JSONArray responsesJsonArray = new JSONArray(); 
			JSONObject responseJson = new JSONObject();
			final ArrayList<String> photoUUIDs = new ArrayList<String>();
            
			try {
				responseJson.put("date", cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_DATE)));
				responseJson.put("time", cursor.getLong(cursor.getColumnIndex(Responses.RESPONSE_TIME)));
				responseJson.put("timezone", cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_TIMEZONE)));
				String locationStatus = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_STATUS));
				responseJson.put("location_status", locationStatus);
				if (! locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE)) {
					JSONObject locationJson = new JSONObject();
					locationJson.put("latitude", cursor.getDouble(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_LATITUDE)));
					locationJson.put("longitude", cursor.getDouble(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_LONGITUDE)));
					locationJson.put("provider", cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_PROVIDER)));
					locationJson.put("accuracy", cursor.getFloat(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_ACCURACY)));
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String locationTimestamp = dateFormat.format(new Date(cursor.getLong(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_TIME))));
					locationJson.put("timestamp", locationTimestamp);
					responseJson.put("location", locationJson);
				}
				responseJson.put("survey_id", cursor.getString(cursor.getColumnIndex(Responses.SURVEY_ID)));
				responseJson.put("survey_launch_context", new JSONObject(cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_SURVEY_LAUNCH_CONTEXT))));
				responseJson.put("responses", new JSONArray(cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_JSON))));
				
				ContentResolver cr2 = getContentResolver();
				Cursor promptsCursor = cr2.query(Responses.buildPromptResponsesUri(responseId), new String [] {PromptResponses.PROMPT_RESPONSE_VALUE, SurveyPrompts.SURVEY_PROMPT_TYPE}, SurveyPrompts.SURVEY_PROMPT_TYPE + "='photo'", null, null);
				
				while (promptsCursor.moveToNext()) {
					photoUUIDs.add(promptsCursor.getString(promptsCursor.getColumnIndex(PromptResponses.PROMPT_RESPONSE_VALUE)));
				}
				
				promptsCursor.close();
				
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			
			responsesJsonArray.put(responseJson);
			
			String campaignUrn = cursor.getString(cursor.getColumnIndex(Responses.CAMPAIGN_URN));
			String campaignCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_CREATED));
			
			File [] photos = Campaign.getCampaignImageDir(this, campaignUrn).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String filename) {
					if (photoUUIDs.contains(filename.split("\\.")[0])) {
						return true;
					}
					return false;
				}
			});
			
			OhmageApi.UploadResponse response = api.surveyUpload(serverUrl, username, hashedPassword, SharedPreferencesHelper.CLIENT_STRING, campaignUrn, campaignCreationTimestamp, responsesJsonArray.toString(), photos);
			
			if (response.getResult() == Result.SUCCESS) {
				dbHelper.setResponseRowUploaded(responseId);
			} else {
				ContentValues values2 = new ContentValues();
				values2.put(Responses.RESPONSE_STATUS, Response.STATUS_ERROR_OTHER);
				String select2 = Tables.RESPONSES + "." + Responses._ID + "=" + responseId;
				cr.update(Responses.CONTENT_URI, values2, select2, null);
			}
			
			cursor.moveToNext();
		}
		
		cursor.close();
	}

}
