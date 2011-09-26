package org.ohmage.service;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.Result;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.PromptResponse;
import org.ohmage.db.DbContract.Response;
import org.ohmage.db.DbContract.SurveyPrompts;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.prompt.photo.PhotoPrompt;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

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
										Tables.RESPONSES + "." + Response._ID,
										Response.DATE,
										Response.TIME,
										Response.TIMEZONE,
										Response.LOCATION_STATUS,
										Response.LOCATION_LATITUDE,
										Response.LOCATION_LONGITUDE,
										Response.LOCATION_PROVIDER,
										Response.LOCATION_ACCURACY,
										Response.LOCATION_TIME,
										Tables.RESPONSES + "." + Response.SURVEY_ID,
										Response.SURVEY_LAUNCH_CONTEXT,
										Response.RESPONSE,
										Tables.RESPONSES + "." + Response.CAMPAIGN_URN,
										Campaigns.CAMPAIGN_CREATED};
		
		String select = intent.getStringExtra("select");
		
		Cursor cursor = cr.query(dataUri, projection, select, null, null);

		cursor.moveToFirst();
		
//		ContentValues cv = new ContentValues();
//		cv.put(Tables.RESPONSES + "." + Response.STATUS, Response.STATUS_QUEUED);
//		cr.update(dataUri, cv, select, null);
		
		for (int i = 0; i < cursor.getCount(); i++) {
			
			long responseId = cursor.getLong(cursor.getColumnIndex(Response._ID));
			
			ContentValues values = new ContentValues();
			values.put(Response.STATUS, Response.STATUS_UPLOADING);
			cr.update(Response.CONTENT_URI, values, Tables.RESPONSES + "." + Response._ID + "=" + responseId, null);
			
			JSONArray responsesJsonArray = new JSONArray(); 
			JSONObject responseJson = new JSONObject();
			final ArrayList<String> photoUUIDs = new ArrayList<String>();
            
			try {
				responseJson.put("date", cursor.getString(cursor.getColumnIndex(Response.DATE)));
				responseJson.put("time", cursor.getLong(cursor.getColumnIndex(Response.TIME)));
				responseJson.put("timezone", cursor.getString(cursor.getColumnIndex(Response.TIMEZONE)));
				String locationStatus = cursor.getString(cursor.getColumnIndex(Response.LOCATION_STATUS));
				responseJson.put("location_status", locationStatus);
				if (! locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE)) {
					JSONObject locationJson = new JSONObject();
					locationJson.put("latitude", cursor.getDouble(cursor.getColumnIndex(Response.LOCATION_LATITUDE)));
					locationJson.put("longitude", cursor.getDouble(cursor.getColumnIndex(Response.LOCATION_LONGITUDE)));
					locationJson.put("provider", cursor.getString(cursor.getColumnIndex(Response.LOCATION_PROVIDER)));
					locationJson.put("accuracy", cursor.getFloat(cursor.getColumnIndex(Response.LOCATION_ACCURACY)));
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String locationTimestamp = dateFormat.format(new Date(cursor.getLong(cursor.getColumnIndex(Response.LOCATION_TIME))));
					locationJson.put("timestamp", locationTimestamp);
					responseJson.put("location", locationJson);
				}
				responseJson.put("survey_id", cursor.getString(cursor.getColumnIndex(Response.SURVEY_ID)));
				responseJson.put("survey_launch_context", new JSONObject(cursor.getString(cursor.getColumnIndex(Response.SURVEY_LAUNCH_CONTEXT))));
				responseJson.put("responses", new JSONArray(cursor.getString(cursor.getColumnIndex(Response.RESPONSE))));
				
				ContentResolver cr2 = getContentResolver();
				Cursor promptsCursor = cr2.query(PromptResponse.getPromptsByResponseID(responseId), new String [] {PromptResponse.PROMPT_VALUE, SurveyPrompts.SURVEY_PROMPT_TYPE}, SurveyPrompts.SURVEY_PROMPT_TYPE + "='photo'", null, null);
				
				while (promptsCursor.moveToNext()) {
					photoUUIDs.add(promptsCursor.getString(promptsCursor.getColumnIndex(PromptResponse.PROMPT_VALUE)));
				}
				
				promptsCursor.close();
				
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			
			responsesJsonArray.put(responseJson);
			
			String campaignUrn = cursor.getString(cursor.getColumnIndex(Response.CAMPAIGN_URN));
			String campaignCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_CREATED));
			
			File [] photos = new File(PhotoPrompt.IMAGE_PATH + "/" + campaignUrn.replace(':', '_')).listFiles(new FilenameFilter() {
				
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
				values2.put(Response.STATUS, Response.STATUS_ERROR_OTHER);
				String select2 = Tables.RESPONSES + "." + Response._ID + "=" + responseId;
				cr.update(Response.CONTENT_URI, values2, select2, null);
			}
			
			cursor.moveToNext();
		}
		
		cursor.close();
	}

}
