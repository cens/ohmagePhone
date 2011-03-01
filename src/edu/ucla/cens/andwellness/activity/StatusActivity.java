package edu.ucla.cens.andwellness.activity;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jbcrypt.BCrypt;

import edu.ucla.cens.andwellness.AndWellnessApi;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.AndWellnessApi.Result;
import edu.ucla.cens.andwellness.AndWellnessApi.ServerResponse;
import edu.ucla.cens.andwellness.R.id;
import edu.ucla.cens.andwellness.R.layout;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.db.Response;
import edu.ucla.cens.andwellness.prompts.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.service.SurveyGeotagService;
import edu.ucla.cens.mobility.glue.MobilityInterface;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class StatusActivity extends Activity {
	
	private static final String TAG = "StatusActivity";
	
	private static final int DIALOG_UPLOAD_PROGRESS = 1;
	private static final int DIALOG_NETWORK_ERROR = 2;
	private static final int DIALOG_INTERNAL_ERROR = 3;
	private static final int DIALOG_AUTHENTICATION_ERROR = 4;
	
	private static final int UPLOAD_RESPONSES = 1;
	private static final int UPLOAD_MOBILITY = 2;
	private static final int UPLOAD_PHOTOS = 3;
	
	private UploadTask mTask;
	
	private TextView mUsernameText;
	private TextView mResponsesText;
	private TextView mMobilityText;
	private TextView mPhotosText;
	private Button mPasswordButton;
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
		mUploadResponsesButton = (Button) findViewById(R.id.status_upload_responses_button);
		mUploadMobilityButton = (Button) findViewById(R.id.status_upload_mobility_button);
		mUploadPhotosButton = (Button) findViewById(R.id.status_upload_photos_button);
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		
		mUsernameText.setText(helper.getUsername());
		
		mResponsesText.setText(String.valueOf(getResponsesCount()));
		mMobilityText.setText(String.valueOf(getMobilityCount()));
		mPhotosText.setText(String.valueOf(getPhotosCount()));
		
		mPasswordButton.setOnClickListener(mClickListener);
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
		DbHelper dbHelper = new DbHelper(this);
		return dbHelper.getSurveyResponses().size();
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
		File [] files = new File(PhotoPrompt.IMAGE_PATH).listFiles(new FilenameFilter() {
			
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
			return files.length;
		} else {
			return 0;
		}
	}

	private OnClickListener mClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.status_password_button:
				startActivity(new Intent(StatusActivity.this, LoginActivity.class));
				finish();
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
	
	private void onUploadTaskDone(AndWellnessApi.ServerResponse response) {
		
		mTask = null;
		
		mResponsesText.setText(String.valueOf(getResponsesCount()));
		mMobilityText.setText(String.valueOf(getMobilityCount()));
		mPhotosText.setText(String.valueOf(getPhotosCount()));
		
		try {
			dismissDialog(DIALOG_UPLOAD_PROGRESS);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Attempting to dismiss dialog that had not been shown.");
			e.printStackTrace();
		}
		
		switch (response.getResult()) {
		case SUCCESS:
			Log.d(TAG, "Uploaded!");
			break;
		case FAILURE:
			Log.e(TAG, "Upload FAILED!!");
			
			boolean isAuthenticationError = false;
			
			for (String code : response.getErrorCodes()) {
				if (code.charAt(1) == '2') {
					isAuthenticationError = true;
				}
			}
			
			if (isAuthenticationError) {
				showDialog(DIALOG_AUTHENTICATION_ERROR);
			} else {
				showDialog(DIALOG_INTERNAL_ERROR);
			}
			break;
		case HTTP_ERROR:
			Log.e(TAG, "Upload FAILED!!");
			showDialog(DIALOG_NETWORK_ERROR);
			break;
		case INTERNAL_ERROR:
			Log.e(TAG, "Upload FAILED!!");
			showDialog(DIALOG_INTERNAL_ERROR);
			break;
		}
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
        				.setMessage("Unable to authenticate. Please check username and re-enter password.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null);
        	dialog = dialogBuilder.create();        	
        	break;
        }
		
		return dialog;
	}
	
	private static class UploadTask extends AsyncTask<Integer, Void, AndWellnessApi.ServerResponse> {
		
		private StatusActivity mActivity;
		private boolean mIsDone = false;
		private SharedPreferences mPreferences;
		private String mUsername;
		private String mHashedPassword;
		private AndWellnessApi.ServerResponse mResponse = null;

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
		protected AndWellnessApi.ServerResponse doInBackground(Integer... params) {
			
			AndWellnessApi.ServerResponse response;
			
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
				response = new AndWellnessApi.ServerResponse(AndWellnessApi.Result.SUCCESS, null);
				break;
			}
			
			return response;
		}
		
		@Override
		protected void onPostExecute(AndWellnessApi.ServerResponse response) {
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
		
		private AndWellnessApi.ServerResponse uploadSurveyResponses() {
			
			SharedPreferencesHelper helper = new SharedPreferencesHelper(mActivity);
			String campaign = helper.getCampaignName();
			String campaignVersion = helper.getCampaignVersion();
			String username = helper.getUsername();
			String hashedPassword = helper.getHashedPassword();
			
			DbHelper dbHelper = new DbHelper(mActivity);

			//long cutoffTime = System.currentTimeMillis() - SurveyLocationService.LOCATION_STALENESS_LIMIT;
			
			List<Response> responseRows = dbHelper.getSurveyResponses();
			//List<Response> responseRows = dbHelper.getSurveyResponsesBefore(cutoffTime);
			
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
				
				AndWellnessApi.ServerResponse response = AndWellnessApi.surveyUpload(username, hashedPassword, "android", campaign, campaignVersion, responsesJsonArray.toString());
				
				if (response.getResult().equals(AndWellnessApi.Result.SUCCESS)) {
					Log.i(TAG, "Successfully uploaded survey responses.");
					for (int i = 0; i < responseRows.size(); i++) {
						dbHelper.removeResponseRow(responseRows.get(i)._id);
					}
				} else {
					Log.e(TAG, "Failed to upload survey responses.");
				}
				
				return response;
			} else {
				Log.i(TAG, "No survey responses to upload.");
				return new AndWellnessApi.ServerResponse(AndWellnessApi.Result.SUCCESS, null);
			}
		}
		
		private AndWellnessApi.ServerResponse uploadMobilityData() {
			
			boolean uploadSensorData = true;
			
			SharedPreferencesHelper helper = new SharedPreferencesHelper(mActivity);
			
			String username = helper.getUsername();
			String hashedPassword = helper.getHashedPassword();
			Long lastMobilityUploadTimestamp = helper.getLastMobilityUploadTimestamp();
			
			Long now = System.currentTimeMillis();
			Cursor c = MobilityInterface.getMobilityCursor(mActivity, lastMobilityUploadTimestamp);
			
			AndWellnessApi.ServerResponse response = new AndWellnessApi.ServerResponse(AndWellnessApi.Result.SUCCESS, null);
			
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
					
					response = AndWellnessApi.mobilityUpload(username, hashedPassword, "android", mobilityJsonArray.toString());
					
					if (response.getResult().equals(AndWellnessApi.Result.SUCCESS)) {
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
				
				////
				
				/*JSONArray mobilityJsonArray = new JSONArray();
				
				for (int i = 0; i < c.getCount(); i++) {
					
					JSONObject mobilityPointJson = new JSONObject();
					
					try {
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Long time = c.getLong(c.getColumnIndex(MobilityInterface.KEY_TIME));
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
				
				c.close();
				
				String json = mobilityJsonArray.toString();
				
				AndWellnessApi.ServerResponse response = AndWellnessApi.mobilityUpload(username, hashedPassword, "android", mobilityJsonArray.toString());
				
				if (response.getResult().equals(AndWellnessApi.Result.SUCCESS)) {
					Log.i(TAG, "Successfully uploaded mobility points.");
					helper.putLastMobilityUploadTimestamp(now);
				} else {
					Log.e(TAG, "Failed to upload mobility points.");
				}*/
				
				//return response;
			} else {
				Log.i(TAG, "No mobility points to upload.");
				//return new AndWellnessApi.ServerResponse(AndWellnessApi.Result.SUCCESS, null);
				return response;
			}
		}
		
		private AndWellnessApi.ServerResponse uploadMedia() {

			SharedPreferencesHelper helper = new SharedPreferencesHelper(mActivity);
			String campaign = helper.getCampaignName();
			String campaignVersion = helper.getCampaignVersion();
			String username = helper.getUsername();
			String hashedPassword = helper.getHashedPassword();
			
			AndWellnessApi.ServerResponse response = new AndWellnessApi.ServerResponse(AndWellnessApi.Result.SUCCESS, null);
			
			File [] files = new File(PhotoPrompt.IMAGE_PATH).listFiles();
			
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].getName().contains("temp")) {
						Log.i(TAG, "Temporary image was discarded.");
						files[i].delete();
					} else {
						response = AndWellnessApi.mediaUpload(username, hashedPassword, "android", campaign, files[i].getName().split("\\.")[0], files[i]);
						
						if (response.getResult().equals(AndWellnessApi.Result.SUCCESS)) {
							Log.i(TAG, "Successfully uploaded an image.");
							files[i].delete();
						} else {
							Log.e(TAG, "Failed to upload an image.");
							break;
						}
					}
				}
			} else {
				Log.e(TAG, PhotoPrompt.IMAGE_PATH + " does not exist.");
			}
			
			return response;
		}
	}
}
