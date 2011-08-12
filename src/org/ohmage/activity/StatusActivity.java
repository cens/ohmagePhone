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
package org.ohmage.activity;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApplication;
import org.ohmage.CampaignManager;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.db.Campaign;
import org.ohmage.db.DbHelper;
import org.ohmage.db.Response;
import org.ohmage.prompt.photo.PhotoPrompt;
import org.ohmage.service.SurveyGeotagService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import org.ohmage.R;
import edu.ucla.cens.mobility.glue.MobilityInterface;
import edu.ucla.cens.systemlog.Log;

public class StatusActivity extends Activity {
	
	private static final String TAG = "StatusActivity";
	
	private static final int DIALOG_UPLOAD_PROGRESS = 1;
	private static final int DIALOG_NETWORK_ERROR = 2;
	private static final int DIALOG_INTERNAL_ERROR = 3;
	private static final int DIALOG_AUTHENTICATION_ERROR = 4;
	private static final int DIALOG_USER_DISABLED = 5;
	private static final int DIALOG_CAMPAIGN_REMOVED = 6;
	private static final int DIALOG_CAMPAIGN_REMOVED_INVALID_URN = 7;
	private static final int DIALOG_CAMPAIGN_REMOVED_INVALID_ROLE = 8;
	private static final int DIALOG_CAMPAIGN_REMOVED_NOT_RUNNING = 9;
	private static final int DIALOG_CAMPAIGN_REMOVED_OUT_OF_DATE = 10;
	private static final int DIALOG_CLEAR_USER_CONFIRM = 11;
	
	private static final int UPLOAD_RESPONSES = 1;
	private static final int UPLOAD_MOBILITY = 2;
	private static final int UPLOAD_PHOTOS = 3;
	
	private UploadTask mTask;
	private static Campaign problematicCampaign = null;
	
	private TextView mUsernameText;
	private TextView mResponsesText;
	private TextView mMobilityText;
	private TextView mPhotosText;
	private Button mPasswordButton;
	private Button mClearUserButton;
	private Button mUploadResponsesButton;
	private Button mUploadMobilityButton;
	private Button mUploadPhotosButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.status_activity);
		
		mUsernameText = (TextView) findViewById(R.id.status_username_text);
		mResponsesText = (TextView) findViewById(R.id.status_responses_text);
		mMobilityText = (TextView) findViewById(R.id.status_mobility_text);
		mPhotosText = (TextView) findViewById(R.id.status_photos_text);
		mPasswordButton = (Button) findViewById(R.id.status_password_button);
		mClearUserButton = (Button) findViewById(R.id.status_clear_user_button);
		mUploadResponsesButton = (Button) findViewById(R.id.status_upload_responses_button);
		mUploadMobilityButton = (Button) findViewById(R.id.status_upload_mobility_button);
		mUploadPhotosButton = (Button) findViewById(R.id.status_upload_photos_button);
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		
		mUsernameText.setText(helper.getUsername());
		
		mResponsesText.setText(String.valueOf(getResponsesCount()));
		mMobilityText.setText(String.valueOf(getMobilityCount()));
		mPhotosText.setText(String.valueOf(getPhotosCount()));
		
		mPasswordButton.setOnClickListener(mClickListener);
		mClearUserButton.setOnClickListener(mClickListener);
		mUploadResponsesButton.setOnClickListener(mClickListener);
		mUploadMobilityButton.setOnClickListener(mClickListener);
		mUploadPhotosButton.setOnClickListener(mClickListener);
		
		
		Object retained = getLastNonConfigurationInstance();
        
        if (retained instanceof UploadTask) {
        	Log.i(TAG, "creating after configuration changed, restored UploadTask instance");
        	mTask = (UploadTask) retained;
        	mTask.setActivity(this);
        }
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.i(TAG, "configuration change");
		if (mTask != null) {
			Log.i(TAG, "retaining UploadTask instance");
			mTask.setActivity(null);
			return mTask;
		}
		return null;
	}
	
	private int getResponsesCount() {
		int count = 0;
		DbHelper dbHelper = new DbHelper(this);
		for (Campaign campaign : dbHelper.getCampaigns()) {
			count += dbHelper.getSurveyResponses(campaign.mUrn).size();
		}
		return count;
	}
	
	private int getMobilityCount() {
		SharedPreferencesHelper prefHelper = new SharedPreferencesHelper(this);
		Long lastMobilityUploadTimestamp = prefHelper.getLastMobilityUploadTimestamp();
		Cursor c = MobilityInterface.getMobilityCursor(this, lastMobilityUploadTimestamp);
		if (c == null) {
			return 0;
		} else {
			int count = c.getCount();
			c.close();
			return count;
		}
	}
	
	private int getPhotosCount() {
		
		int count = 0;
		DbHelper dbHelper = new DbHelper(this);
		for (Campaign campaign : dbHelper.getCampaigns()) {
			
			File [] files = new File(PhotoPrompt.IMAGE_PATH + "/" + campaign.mUrn.replace(':', '_')).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.contains("temp")) {
						return false;
					} else {
						return true;
					}
				}
			});
			
			if (files != null) {
				count += files.length;
			}
		}
		
		return count;
	}

	private OnClickListener mClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.status_password_button:
				setResult(125);
				finish();
				break;
			case R.id.status_clear_user_button:
				showDialog(DIALOG_CLEAR_USER_CONFIRM);
				break;
			case R.id.status_upload_responses_button:
				doUpload(UPLOAD_RESPONSES);
				break;
			case R.id.status_upload_mobility_button:
				doUpload(UPLOAD_MOBILITY);
				break;
			case R.id.status_upload_photos_button:
				doUpload(UPLOAD_PHOTOS);
				break;
			}
		}
		
	};
	
	private void doUpload(int uploadType) {
		mTask = new UploadTask(StatusActivity.this);
		mTask.execute(uploadType);
	}
	
	private void onUploadTaskDone(OhmageApi.UploadResponse response) {
		
		mTask = null;
		
		try {
			dismissDialog(DIALOG_UPLOAD_PROGRESS);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Attempting to dismiss dialog that had not been shown.", e);
		}
		
		switch (response.getResult()) {
		case SUCCESS:
			Log.d(TAG, "Uploaded!");
			break;
		case FAILURE:
			Log.e(TAG, "Upload failed due to error codes: " + Utilities.stringArrayToString(response.getErrorCodes(), ", "));
			
			boolean isAuthenticationError = false;
			boolean isUserDisabled = false;
			boolean removeCampaign = false;
			
			String errorCode = null;
			
			for (String code : response.getErrorCodes()) {
				if (code.charAt(1) == '2') {
					isAuthenticationError = true;
					
					if (code.equals("0201")) {
						isUserDisabled = true;
					}
				}
				
				if (code.equals("0604") || code.equals("0607") || code.equals("0608") || code.equals("0609")) {
					removeCampaign = true;
					errorCode = code;
					break;
				}
			}
			
			if (isUserDisabled) {
				SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);
				prefs.setUserDisabled(true);
				
				showDialog(DIALOG_USER_DISABLED);				
			} else if (isAuthenticationError) {
				showDialog(DIALOG_AUTHENTICATION_ERROR);
			} else if (removeCampaign){
				CampaignManager.removeCampaign(this, problematicCampaign.mUrn);
				if (errorCode.equals("0604")) {
					showDialog(DIALOG_CAMPAIGN_REMOVED_INVALID_URN);
				} else if (errorCode.equals("0607")) {
					showDialog(DIALOG_CAMPAIGN_REMOVED_INVALID_ROLE);
				} else if (errorCode.equals("0608")) {
					showDialog(DIALOG_CAMPAIGN_REMOVED_NOT_RUNNING);
				} else if (errorCode.equals("0609")) {
					showDialog(DIALOG_CAMPAIGN_REMOVED_OUT_OF_DATE);
				} else {
					showDialog(DIALOG_CAMPAIGN_REMOVED);
				}
			} else {
				showDialog(DIALOG_INTERNAL_ERROR);
				//add error codes to dialog?
			}
			break;
		case HTTP_ERROR:
			Log.e(TAG, "Upload failed due to http error");
			showDialog(DIALOG_NETWORK_ERROR);
			break;
		case INTERNAL_ERROR:
			Log.e(TAG, "Upload failed due to internal error");
			showDialog(DIALOG_INTERNAL_ERROR);
			break;
		}
		
		mResponsesText.setText(String.valueOf(getResponsesCount()));
		mMobilityText.setText(String.valueOf(getMobilityCount()));
		mPhotosText.setText(String.valueOf(getPhotosCount()));
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_UPLOAD_PROGRESS:
			ProgressDialog pDialog = new ProgressDialog(this);
			pDialog.setMessage("Uploading data...");
			pDialog.setCancelable(false);
			//pDialog.setIndeterminate(true);
			dialog = pDialog;
        	break;
        	
		case DIALOG_NETWORK_ERROR:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Unable to communicate with server. Please try again later.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();
        	break;
        
		case DIALOG_INTERNAL_ERROR:
        	dialogBuilder.setTitle("Error")
        				.setMessage("The server returned an unexpected response. Please try again later.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();
        	break;
        	
		case DIALOG_AUTHENTICATION_ERROR:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Unable to authenticate. Please check username and update the password.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();        	
        	break;
        	
		case DIALOG_USER_DISABLED:
			dialogBuilder.setTitle("Error")
						.setMessage("User account is disabled. App state has been cleared.")
						.setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								setResult(125);
								finish();		
							}
						});
			dialog = dialogBuilder.create();        	
			break;
			
		case DIALOG_CAMPAIGN_REMOVED:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Campaign (" + problematicCampaign.mUrn + ") is no longer valid and has been removed from your phone.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();        	
        	break;
		case DIALOG_CAMPAIGN_REMOVED_INVALID_URN:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Campaign (" + problematicCampaign.mUrn + ") is not valid and has been removed from your phone.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();        	
        	break;
		case DIALOG_CAMPAIGN_REMOVED_INVALID_ROLE:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Due to an invalid user role, campaign (" + problematicCampaign.mUrn + ") has been removed from your phone.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();        	
        	break;
		case DIALOG_CAMPAIGN_REMOVED_NOT_RUNNING:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Campaign (" + problematicCampaign.mUrn + ") is no longer running and has been removed from your phone.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();        	
        	break;
		case DIALOG_CAMPAIGN_REMOVED_OUT_OF_DATE:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Campaign (" + problematicCampaign.mUrn + ") is out of date and has been removed from your phone.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();        	
        	break;
		case DIALOG_CLEAR_USER_CONFIRM:			
        	dialogBuilder.setTitle("Confirm")
			.setMessage("Are you sure you wish to clear all user data? Any data that has not been uploaded will be lost, and the app will be restored to its initial state.")
			.setNegativeButton("No", null)
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((OhmageApplication)getApplication()).resetAll();
					setResult(125);
					finish();
				}

			});
        	dialog = dialogBuilder.create();        	
        	break;
        }
		
		return dialog;
	}
	
	private static class UploadTask extends AsyncTask<Integer, Void, OhmageApi.UploadResponse> {
		
		private StatusActivity mActivity;
		private boolean mIsDone = false;
		private SharedPreferences mPreferences;
		private String mUsername;
		private String mHashedPassword;
		private OhmageApi.UploadResponse mResponse = null;

		private UploadTask(StatusActivity activity) {
			this.mActivity = activity;
		}
		
		public void setActivity(StatusActivity activity) {
			this.mActivity = activity;
			if (mIsDone) {
				notifyTaskDone();
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.i(TAG, "Upload Task Started.");
			mActivity.showDialog(DIALOG_UPLOAD_PROGRESS);
			SharedPreferencesHelper helper = new SharedPreferencesHelper(mActivity);
			mUsername = helper.getUsername();
			mHashedPassword = helper.getHashedPassword();
		}

		@Override
		protected OhmageApi.UploadResponse doInBackground(Integer... params) {
			
			OhmageApi.UploadResponse response;
			
			switch (params[0]) {
			case UPLOAD_RESPONSES:
				response = uploadSurveyResponses();
				break;

			case UPLOAD_MOBILITY:
				response = uploadMobilityData();
				break;

			case UPLOAD_PHOTOS:
				response = uploadMedia();
				break;
			default:
				//should report error here instead of success
				response = new OhmageApi.UploadResponse(OhmageApi.Result.SUCCESS, null);
				break;
			}
			
			return response;
		}
		
		@Override
		protected void onPostExecute(OhmageApi.UploadResponse response) {
			super.onPostExecute(response);
			
			Log.i(TAG, "Upload Task Finished.");
			
			mResponse = response;
			mIsDone = true;
			notifyTaskDone();			
		}
		
		private void notifyTaskDone() {
			if (mActivity != null) {
				mActivity.onUploadTaskDone(mResponse);
			}
		}
		
		private OhmageApi.UploadResponse uploadSurveyResponses() {
			
			SharedPreferencesHelper helper = new SharedPreferencesHelper(mActivity);
			String username = helper.getUsername();
			String hashedPassword = helper.getHashedPassword();
			
			problematicCampaign = null;
			
			DbHelper dbHelper = new DbHelper(mActivity);

			//long cutoffTime = System.currentTimeMillis() - SurveyLocationService.LOCATION_STALENESS_LIMIT;
			
			OhmageApi.UploadResponse response = new OhmageApi.UploadResponse(OhmageApi.Result.SUCCESS, null);
			
			for (Campaign campaign : dbHelper.getCampaigns()) {
				
				Log.i(TAG, "Attempting to upload responses for " + campaign.mUrn);
				
				List<Response> responseRows = dbHelper.getSurveyResponses(campaign.mUrn);
				//List<Response> responseRows = dbHelper.getSurveyResponsesBefore(campaign.mUrn, cutoffTime);
				
				String serverUrl = SharedPreferencesHelper.DEFAULT_SERVER_URL; //campaign.serverUrl;
				
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
					OhmageApi api = new OhmageApi(mActivity);
					response = api.surveyUpload(serverUrl, username, hashedPassword, SharedPreferencesHelper.CLIENT_STRING, campaign.mUrn, campaign.mCreationTimestamp, responsesJsonArray.toString());
					
					if (response.getResult().equals(OhmageApi.Result.SUCCESS)) {
						Log.i(TAG, "Successfully uploaded survey responses for " + campaign.mUrn);
						for (int i = 0; i < responseRows.size(); i++) {
							dbHelper.removeResponseRow(responseRows.get(i)._id);
						}
					} else {
						Log.e(TAG, "Failed to upload survey responses for " + campaign.mUrn);
						problematicCampaign = campaign;
						return response;
					}
					
				} else {
					Log.i(TAG, "No survey responses to upload for " + campaign.mUrn);
				}
			}
			
			return response;
		}
		
		private OhmageApi.UploadResponse uploadMobilityData() {
			
			boolean uploadSensorData = true;
			
			SharedPreferencesHelper helper = new SharedPreferencesHelper(mActivity);
			
			String username = helper.getUsername();
			String hashedPassword = helper.getHashedPassword();
			Long lastMobilityUploadTimestamp = helper.getLastMobilityUploadTimestamp();
			
			Long now = System.currentTimeMillis();
			Cursor c = MobilityInterface.getMobilityCursor(mActivity, lastMobilityUploadTimestamp);
			
			OhmageApi.UploadResponse response = new OhmageApi.UploadResponse(OhmageApi.Result.SUCCESS, null);
			
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
								String accelDataString = c.getString(c.getColumnIndex(MobilityInterface.KEY_ACCELDATA));
								if (accelDataString == null || accelDataString.equals("")) {
									accelDataString = "[]";
								}
								dataJson.put("accel_data", new JSONArray(accelDataString));
								String wifiDataString = c.getString(c.getColumnIndex(MobilityInterface.KEY_WIFIDATA));
								if (wifiDataString == null || wifiDataString.equals("")) {
									wifiDataString = "{}";
								}
								dataJson.put("wifi_data", new JSONObject(wifiDataString));
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
					
					OhmageApi api = new OhmageApi(mActivity);
					SharedPreferencesHelper prefs = new SharedPreferencesHelper(mActivity);
					response = api.mobilityUpload(SharedPreferencesHelper.DEFAULT_SERVER_URL, username, hashedPassword, SharedPreferencesHelper.CLIENT_STRING, mobilityJsonArray.toString());
					
					if (response.getResult().equals(OhmageApi.Result.SUCCESS)) {
						Log.i(TAG, "Successfully uploaded " + String.valueOf(limit) + " mobility points.");
						helper.putLastMobilityUploadTimestamp(lastMobilityUploadTimestamp);
						remainingCount -= limit;
						Log.i(TAG, "There are " + String.valueOf(remainingCount) + " mobility points remaining to be uploaded.");
					} else {
						Log.e(TAG, "Failed to upload mobility points. Cancelling current round of mobility uploads.");
						break;						
					}
				}
				
				c.close();
				
				return response;
				
			} else {
				Log.i(TAG, "No mobility points to upload.");
				
				return response;
			}
		}
		
		private OhmageApi.UploadResponse uploadMedia() {

			SharedPreferencesHelper helper = new SharedPreferencesHelper(mActivity);
			String username = helper.getUsername();
			String hashedPassword = helper.getHashedPassword();
			
			OhmageApi.UploadResponse response = new OhmageApi.UploadResponse(OhmageApi.Result.SUCCESS, null);
			
			DbHelper dbHelper = new DbHelper(mActivity);
			
			for (Campaign campaign : dbHelper.getCampaigns()) {
				
				String serverUrl = SharedPreferencesHelper.DEFAULT_SERVER_URL; //campaign.serverUrl;
				
				File [] files = new File(PhotoPrompt.IMAGE_PATH + "/" + campaign.mUrn.replace(':', '_')).listFiles();
				
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						if (files[i].getName().contains("temp")) {
							Log.i(TAG, "Temporary image was discarded.");
							files[i].delete();
						} else {
							OhmageApi api = new OhmageApi(mActivity);
							SharedPreferencesHelper prefs = new SharedPreferencesHelper(mActivity);
							response = api.mediaUpload(serverUrl, username, hashedPassword, SharedPreferencesHelper.CLIENT_STRING, campaign.mUrn, campaign.mCreationTimestamp, files[i].getName().split("\\.")[0], files[i]);
							
							if (response.getResult().equals(OhmageApi.Result.SUCCESS)) {
								Log.i(TAG, "Successfully uploaded an image.");
								files[i].delete();
							} else {
								Log.e(TAG, "Failed to upload an image.");
								break;
							}
						}
					}
				} else {
					Log.e(TAG, PhotoPrompt.IMAGE_PATH + "/" + campaign.mUrn.replace(':', '_') + " does not exist.");
				}
				
				if (response.getResult().equals(OhmageApi.Result.FAILURE)) {
					problematicCampaign = campaign;
					return response;
				}
			}
			
			return response;
		}
	}
}
