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
package edu.ucla.cens.andwellness.service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.andwellness.AndWellnessApi;
import edu.ucla.cens.andwellness.CampaignManager;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.Utilities;
import edu.ucla.cens.andwellness.activity.CampaignListActivity;
import edu.ucla.cens.andwellness.activity.LoginActivity;
import edu.ucla.cens.andwellness.db.Campaign;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.db.Response;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPrompt;
import edu.ucla.cens.mobility.glue.MobilityInterface;
import edu.ucla.cens.systemlog.Log;

public class UploadService extends WakefulIntentService{
	
	private static final String TAG = "UploadService";
	
	private static final int ERROR_AUTHENTICATION = 1;
	private static final int ERROR_CAMPAIGN_REMOVED = 2;
	
	private AndWellnessApi mApi;

	public UploadService() {
		super(TAG);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mApi = new AndWellnessApi(this);
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		
		uploadSurveyResponses();
		
		uploadMobilityData();
		
		uploadMedia();
	}

	private void uploadSurveyResponses() {
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		String username = helper.getUsername();
		String hashedPassword = helper.getHashedPassword();
		
		DbHelper dbHelper = new DbHelper(this);

		long cutoffTime = System.currentTimeMillis() - SurveyGeotagService.LOCATION_STALENESS_LIMIT;

		for (Campaign campaign : dbHelper.getCampaigns()) {
				
			Log.i(TAG, "Attempting to upload responses for " + campaign.mUrn);
			
			String serverUrl = SharedPreferencesHelper.DEFAULT_SERVER_URL; //campaign.serverUrl;
        
			//List<Response> responseRows = dbHelper.getSurveyResponses(campaign.mUrn);
			List<Response> responseRows = dbHelper.getSurveyResponsesBefore(campaign.mUrn, cutoffTime);
        
			if (responseRows.size() > 0) {
        
				JSONArray responsesJsonArray = new JSONArray(); 
          
				for (int i = 0; i < responseRows.size(); i++) {
					JSONObject responseJson = new JSONObject();
            
					try {
						responseJson.put("date", responseRows.get(i).date);
						responseJson.put("time", responseRows.get(i).time);
						responseJson.put("timezone", responseRows.get(i).timezone);
						responseJson.put("location_status", responseRows.get(i).locationStatus);
						if (! responseRows.get(i).locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE)) {
							JSONObject locationJson = new JSONObject();
							locationJson.put("latitude", responseRows.get(i).locationLatitude);
							locationJson.put("longitude", responseRows.get(i).locationLongitude);
							locationJson.put("provider", responseRows.get(i).locationProvider);
							locationJson.put("accuracy", responseRows.get(i).locationAccuracy);
							SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							String locationTimestamp = dateFormat.format(new Date(responseRows.get(i).locationTime));
							locationJson.put("timestamp", locationTimestamp);
							responseJson.put("location", locationJson);
						}
						responseJson.put("survey_id", responseRows.get(i).surveyId);
						responseJson.put("survey_launch_context", new JSONObject(responseRows.get(i).surveyLaunchContext));
						responseJson.put("responses", new JSONArray(responseRows.get(i).response));
					} catch (JSONException e) {
						throw new RuntimeException(e);
					}
					
					responsesJsonArray.put(responseJson);
				}
				
				AndWellnessApi.UploadResponse response = mApi.surveyUpload(serverUrl, username, hashedPassword, SharedPreferencesHelper.CLIENT_STRING, campaign.mUrn, campaign.mCreationTimestamp, responsesJsonArray.toString());
			
				if (response.getResult().equals(AndWellnessApi.Result.SUCCESS)) {
					Log.i(TAG, "Successfully uploaded survey responses for " + campaign.mUrn);
					for (int i = 0; i < responseRows.size(); i++) {
						dbHelper.removeResponseRow(responseRows.get(i)._id);
					}
				} else {
					Log.e(TAG, "Failed to upload survey responses for " + campaign.mUrn);

					handleErrors(response, campaign);
				}

			} else {
				Log.i(TAG, "No survey responses to upload for " + campaign.mUrn);
			}
		}
	}
	
	private void uploadMobilityData() {
		boolean uploadSensorData = true;
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		
		String username = helper.getUsername();
		String hashedPassword = helper.getHashedPassword();
		Long lastMobilityUploadTimestamp = helper.getLastMobilityUploadTimestamp();
		
		Long now = System.currentTimeMillis();
		Cursor c = MobilityInterface.getMobilityCursor(this, lastMobilityUploadTimestamp);
		
		AndWellnessApi.UploadResponse response = new AndWellnessApi.UploadResponse(AndWellnessApi.Result.SUCCESS, null);
		
		if (c != null && c.getCount() > 0) {
			
			Log.i(TAG, "There are " + String.valueOf(c.getCount()) + " mobility points to upload.");
			
			c.moveToFirst();
			
			int remainingCount = c.getCount();
			int limit = 60;
			
			while (remainingCount > 0) {
				
				if (remainingCount < limit) {
					limit = remainingCount;
				}
				
				Log.i(TAG, "Attempting to upload a batch with " + String.valueOf(limit) + " mobility points.");
				
				JSONArray mobilityJsonArray = new JSONArray();
				
				for (int i = 0; i < limit; i++) {
					JSONObject mobilityPointJson = new JSONObject();
					
					try {
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Long time = c.getLong(c.getColumnIndex(MobilityInterface.KEY_TIME));
						if (i == limit - 1) {
							lastMobilityUploadTimestamp = time;
						}
						mobilityPointJson.put("date", dateFormat.format(new Date(time)));
						mobilityPointJson.put("time", time);
						mobilityPointJson.put("timezone", c.getString(c.getColumnIndex(MobilityInterface.KEY_TIMEZONE)));
						if (uploadSensorData) {
							mobilityPointJson.put("subtype", "sensor_data");
							JSONObject dataJson = new JSONObject();
							dataJson.put("mode", c.getString(c.getColumnIndex(MobilityInterface.KEY_MODE)));
							dataJson.put("speed", Float.parseFloat(c.getString(c.getColumnIndex(MobilityInterface.KEY_SPEED))));
							dataJson.put("accel_data", new JSONArray(c.getString(c.getColumnIndex(MobilityInterface.KEY_ACCELDATA))));
							dataJson.put("wifi_data", new JSONObject(c.getString(c.getColumnIndex(MobilityInterface.KEY_WIFIDATA))));
							mobilityPointJson.put("data", dataJson);
						} else {
							mobilityPointJson.put("subtype", "mode_only");
							mobilityPointJson.put("mode", c.getString(c.getColumnIndex(MobilityInterface.KEY_MODE)));
						}
						String locationStatus = c.getString(c.getColumnIndex(MobilityInterface.KEY_STATUS));
						mobilityPointJson.put("location_status", locationStatus);
						if (! locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE)) {
							JSONObject locationJson = new JSONObject();
							locationJson.put("latitude", Double.parseDouble(c.getString(c.getColumnIndex(MobilityInterface.KEY_LATITUDE))));
							locationJson.put("longitude", Double.parseDouble(c.getString(c.getColumnIndex(MobilityInterface.KEY_LONGITUDE))));
							locationJson.put("provider", c.getString(c.getColumnIndex(MobilityInterface.KEY_PROVIDER)));
							locationJson.put("accuracy", Float.parseFloat(c.getString(c.getColumnIndex(MobilityInterface.KEY_ACCURACY))));
							locationJson.put("timestamp", dateFormat.format(new Date(Long.parseLong(c.getString(c.getColumnIndex(MobilityInterface.KEY_LOC_TIMESTAMP))))));
							mobilityPointJson.put("location", locationJson);
						}
						
					} catch (JSONException e) {
						throw new RuntimeException(e);
					}
					
					mobilityJsonArray.put(mobilityPointJson);
					
					c.moveToNext();
				}
				SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);
				response = mApi.mobilityUpload(SharedPreferencesHelper.DEFAULT_SERVER_URL, username, hashedPassword, SharedPreferencesHelper.CLIENT_STRING, mobilityJsonArray.toString());
				
				if (response.getResult().equals(AndWellnessApi.Result.SUCCESS)) {
					Log.i(TAG, "Successfully uploaded " + String.valueOf(limit) + " mobility points.");
					helper.putLastMobilityUploadTimestamp(lastMobilityUploadTimestamp);
					remainingCount -= limit;
					Log.i(TAG, "There are " + String.valueOf(remainingCount) + " mobility points remaining to be uploaded.");
				} else {
					Log.e(TAG, "Failed to upload mobility points. Cancelling current round of mobility uploads.");
					handleErrors(response, null);
					break;						
				}
			}
			
			c.close();
		} else {
			Log.i(TAG, "No mobility points to upload.");
		}
		
	}

	private void uploadMedia() {

		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		String username = helper.getUsername();
		String hashedPassword = helper.getHashedPassword();
		
		DbHelper dbHelper = new DbHelper(this);
		
		for (Campaign campaign : dbHelper.getCampaigns()) {
			
			String serverUrl = SharedPreferencesHelper.DEFAULT_SERVER_URL; //campaign.serverUrl;
			
			File [] files = new File(PhotoPrompt.IMAGE_PATH + "/" + campaign.mUrn.replace(':', '_')).listFiles();
			
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].getName().contains("temp")) {
						Log.i(TAG, "Temporary image was discarded.");
						files[i].delete();
					} else {
						AndWellnessApi.UploadResponse response = mApi.mediaUpload(serverUrl, username, hashedPassword, SharedPreferencesHelper.CLIENT_STRING, campaign.mUrn, campaign.mCreationTimestamp, files[i].getName().split("\\.")[0], files[i]);
						
						if (response.getResult().equals(AndWellnessApi.Result.SUCCESS)) {
							Log.i(TAG, "Successfully uploaded an image.");
							files[i].delete();
						} else {
							Log.e(TAG, "Failed to upload an image.");
							handleErrors(response, campaign);
							return;
						}
					}
				}
			} else {
				Log.e(TAG, PhotoPrompt.IMAGE_PATH + "/" + campaign.mUrn.replace(':', '_') + " does not exist.");
			}
		}		
	}	

	private void handleErrors(AndWellnessApi.UploadResponse response, Campaign problematicCampaign) {
		switch (response.getResult()) {
		case FAILURE:
			Log.e(TAG, "Upload failed due to error codes: " + Utilities.stringArrayToString(response.getErrorCodes(), ", "));
			
			boolean isAuthenticationError = false;
			boolean isUserDisabled = false;
			boolean removeCampaign = false;
			
			for (String code : response.getErrorCodes()) {
				if (code.charAt(1) == '2') {
					isAuthenticationError = true;
					
					if (code.equals("0201")) {
						isUserDisabled = true;
					}
				}
				
				if (code.equals("0604") || code.equals("0607") || code.equals("0608") || code.equals("0609")) {
					removeCampaign = true;
				}
			}
			
			if (isUserDisabled) {
				SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);
				prefs.setUserDisabled(true);
				
				showErrorNotification(ERROR_AUTHENTICATION);
			} else if (isAuthenticationError) {
				//show auth notification
				showErrorNotification(ERROR_AUTHENTICATION);
			} else if (removeCampaign){
				CampaignManager.removeCampaign(this, problematicCampaign.mUrn);
				showErrorNotification(ERROR_CAMPAIGN_REMOVED);
			} else {
				//show internal error notification? with error codes?
			}
			break;
		case HTTP_ERROR:
			Log.e(TAG, "Upload failed due to http error");
			//do nothing?
			break;
		case INTERNAL_ERROR:
			Log.e(TAG, "Upload failed due to internal error");
			//show internal error notification?
			break;
		}
	}
	
	private void showErrorNotification(int errorType) {
		NotificationManager noteManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		Notification note = new Notification();
		
		if (errorType == ERROR_AUTHENTICATION) {
			Intent intentToLaunch = new Intent(this, LoginActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentToLaunch, 0);
			String title = "ohmage authentication failed!";
			String body = "Tap here to re-enter password.";
			note.icon = android.R.drawable.stat_notify_error;
			note.tickerText = "Authentication failed!";
			note.defaults |= Notification.DEFAULT_ALL;
			note.when = System.currentTimeMillis();
			note.flags = Notification.FLAG_AUTO_CANCEL;
			note.setLatestEventInfo(this, title, body, pendingIntent);
			noteManager.notify(1, note);
		} else if (errorType == ERROR_CAMPAIGN_REMOVED) {
			Intent intentToLaunch = new Intent(this, CampaignListActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentToLaunch, 0);
			String title = "Invalid campaign!";
			String body = "An invalid campaign was removed from your phone.";
			note.icon = android.R.drawable.stat_notify_error;
			note.tickerText = "Invalid campaign!";
			note.defaults |= Notification.DEFAULT_ALL;
			note.when = System.currentTimeMillis();
			note.flags = Notification.FLAG_AUTO_CANCEL;
			note.setLatestEventInfo(this, title, body, pendingIntent);
			noteManager.notify(1, note);
		}
	}
}
