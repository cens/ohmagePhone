package org.ohmage.service;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.mobility.glue.MobilityInterface;
import edu.ucla.cens.systemlog.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.Config;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.Result;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.SurveyPrompts;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.db.Models.Response;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class UploadService extends WakefulIntentService {

	/** Extra to tell the upload service to upload mobility points */
	public static final String EXTRA_UPLOAD_MOBILITY = "upload_mobility";

	/** Extra to tell the upload service to upload surveys */
	public static final String EXTRA_UPLOAD_SURVEYS = "upload_surveys";

	/** Extra to tell the upload service if it is running in the background */
	public static final String EXTRA_BACKGROUND = "is_background";

	private static final String TAG = "UploadService";
	
	public static final String MOBILITY_UPLOAD_STARTED = "org.ohmage.MOBILITY_UPLOAD_STARTED";
	public static final String MOBILITY_UPLOAD_FINISHED = "org.ohmage.MOBILITY_UPLOAD_FINISHED";

	public UploadService() {
		super(TAG);
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		
		if (intent.getBooleanExtra(EXTRA_UPLOAD_SURVEYS, false)) {
			uploadSurveyResponses(intent);
		}
		
		if (intent.getBooleanExtra(EXTRA_UPLOAD_MOBILITY, false)) {
			uploadMobility(intent);
		}
	}

	private void uploadSurveyResponses(Intent intent) {
		String serverUrl = Config.DEFAULT_SERVER_URL;
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		String username = helper.getUsername();
		String hashedPassword = helper.getHashedPassword();
		boolean isBackground = intent.getBooleanExtra(EXTRA_BACKGROUND, false);
		boolean uploadErrorOccurred = false;
		boolean authErrorOccurred = false;
		
		OhmageApi api = new OhmageApi();
		DbHelper dbHelper = new DbHelper(this);
		
		Uri dataUri = intent.getData();
		if(!Responses.isResponseUri(dataUri)) {
			Log.e(TAG, "Upload service can only be called with a response URI");
			return;
		}

		ContentResolver cr = getContentResolver();
		
		String [] projection = new String [] {
										Tables.RESPONSES + "." + Responses._ID,
										Responses.RESPONSE_UUID,
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
		
		String select =  Responses.RESPONSE_STATUS + "!=" + Response.STATUS_DOWNLOADED + " AND " + 
						Responses.RESPONSE_STATUS + "!=" + Response.STATUS_UPLOADED + " AND " + 
						Responses.RESPONSE_STATUS + "!=" + Response.STATUS_WAITING_FOR_LOCATION;
		
		Cursor cursor = cr.query(dataUri, projection, select, null, null);

		cursor.moveToFirst();
		
		ContentValues cv = new ContentValues();
		cv.put(Responses.RESPONSE_STATUS, Response.STATUS_QUEUED);
		cr.update(dataUri, cv, select, null);
		
		for (int i = 0; i < cursor.getCount(); i++) {
			
			long responseId = cursor.getLong(cursor.getColumnIndex(Responses._ID));
			
			ContentValues values = new ContentValues();
			values.put(Responses.RESPONSE_STATUS, Response.STATUS_UPLOADING);
			cr.update(Responses.buildResponseUri(responseId), values, null, null);
//			cr.update(Responses.CONTENT_URI, values, Tables.RESPONSES + "." + Responses._ID + "=" + responseId, null);
			
			JSONArray responsesJsonArray = new JSONArray(); 
			JSONObject responseJson = new JSONObject();
			final ArrayList<String> photoUUIDs = new ArrayList<String>();
            
			try {
				responseJson.put("survey_key", cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_UUID)));
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
					locationJson.put("time", cursor.getLong(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_TIME)));
					locationJson.put("timezone", cursor.getLong(cursor.getColumnIndex(Responses.RESPONSE_TIMEZONE)));
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
			
			File [] photos = Response.getResponsesImageDir(this, campaignUrn, String.valueOf(responseId)).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String filename) {
					if (photoUUIDs.contains(filename.split("\\.")[0])) {
						return true;
					}
					return false;
				}
			});
			
			OhmageApi.UploadResponse response = api.surveyUpload(serverUrl, username, hashedPassword, SharedPreferencesHelper.CLIENT_STRING, campaignUrn, campaignCreationTimestamp, responsesJsonArray.toString(), photos);
			
			int responseStatus = Response.STATUS_UPLOADED;

			if (response.getResult() == Result.SUCCESS) {
				NotificationHelper.hideUploadErrorNotification(this);
				NotificationHelper.hideAuthNotification(this);
			} else {
				responseStatus = Response.STATUS_ERROR_OTHER;
				
				switch (response.getResult()) {
				case FAILURE:
					Log.e(TAG, "Upload failed due to error codes: " + Utilities.stringArrayToString(response.getErrorCodes(), ", "));
					
					uploadErrorOccurred = true;
					
					boolean isAuthenticationError = false;
					boolean isUserDisabled = false;
					
					String errorCode = null;
					
					for (String code : response.getErrorCodes()) {
						if (code.charAt(1) == '2') {
							authErrorOccurred = true;
							
							isAuthenticationError = true;
							
							if (code.equals("0201")) {
								isUserDisabled = true;
							}
						}
						
						if (code.equals("0700") || code.equals("0707") || code.equals("0703") || code.equals("0710")) {
							errorCode = code;
							break;
						}
					}
					
					if (isUserDisabled) {
						new SharedPreferencesHelper(this).setUserDisabled(true);
					}
					
					if (isAuthenticationError) {
						responseStatus = Response.STATUS_ERROR_AUTHENTICATION;

					} else if ("0700".equals(errorCode)) {
						responseStatus = Response.STATUS_ERROR_CAMPAIGN_NO_EXIST;
					} else if ("0707".equals(errorCode)) {
						responseStatus = Response.STATUS_ERROR_INVALID_USER_ROLE;
					} else if ("0703".equals(errorCode)) {
						responseStatus = Response.STATUS_ERROR_CAMPAIGN_STOPPED;
					} else if ("0710".equals(errorCode)) {
						responseStatus = Response.STATUS_ERROR_CAMPAIGN_OUT_OF_DATE;
					} else {
						responseStatus = Response.STATUS_ERROR_OTHER;
					}
					
					break;

				case INTERNAL_ERROR:
					uploadErrorOccurred = true;
					responseStatus = Response.STATUS_ERROR_OTHER;
					break;
					
				case HTTP_ERROR:
					responseStatus = Response.STATUS_ERROR_HTTP;
					break;
				}
			}

			ContentValues cv2 = new ContentValues();
			cv2.put(Responses.RESPONSE_STATUS, responseStatus);
			cr.update(Responses.buildResponseUri(responseId), cv2, null, null);

			cursor.moveToNext();
		}

		cursor.close();
		
		if (isBackground) {
			if (authErrorOccurred) {
				NotificationHelper.showAuthNotification(this);
			} else if (uploadErrorOccurred) {
				NotificationHelper.showUploadErrorNotification(this);
			}
		}
	}
	
	private void uploadMobility(Intent intent) {
		
		sendBroadcast(new Intent(UploadService.MOBILITY_UPLOAD_STARTED));
		
		boolean uploadSensorData = true;
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		
		String username = helper.getUsername();
		String hashedPassword = helper.getHashedPassword();
		long uploadAfterTimestamp = helper.getLastMobilityUploadTimestamp();
		if (uploadAfterTimestamp == 0) {
			uploadAfterTimestamp = helper.getLoginTimestamp();
		}
		
		Long now = System.currentTimeMillis();
		Cursor c = MobilityInterface.getMobilityCursor(this, uploadAfterTimestamp);
		
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
						Long time = Long.parseLong(c.getString(c.getColumnIndex(MobilityInterface.KEY_TIME)));
						if (i == limit - 1) {
							uploadAfterTimestamp = time;
						}
						
						mobilityPointJson.put("id", c.getString(c.getColumnIndex(MobilityInterface.KEY_ID)));
						mobilityPointJson.put("time", time);
						mobilityPointJson.put("timezone", c.getString(c.getColumnIndex(MobilityInterface.KEY_TIMEZONE)));
						if (uploadSensorData) {
							mobilityPointJson.put("subtype", "sensor_data");
							JSONObject dataJson = new JSONObject();
							dataJson.put("mode", c.getString(c.getColumnIndex(MobilityInterface.KEY_MODE)));
							
							try {
								dataJson.put("speed", Float.parseFloat(c.getString(c.getColumnIndex(MobilityInterface.KEY_SPEED))));
							} catch (NumberFormatException e) {
								dataJson.put("speed", "NaN");
							} catch (JSONException e) {
								dataJson.put("speed", "NaN");
							}
							
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
							
							try {
								locationJson.put("latitude", Double.parseDouble(c.getString(c.getColumnIndex(MobilityInterface.KEY_LATITUDE))));
							} catch (NumberFormatException e) {
								locationJson.put("latitude", "NaN");
							} catch (JSONException e) {
								locationJson.put("latitude", "NaN");
							}
							
							try {
								locationJson.put("longitude", Double.parseDouble(c.getString(c.getColumnIndex(MobilityInterface.KEY_LONGITUDE))));
							} catch (NumberFormatException e) {
								locationJson.put("longitude", "NaN");
							}  catch (JSONException e) {
								locationJson.put("longitude", "NaN");
							}
							
							locationJson.put("provider", c.getString(c.getColumnIndex(MobilityInterface.KEY_PROVIDER)));
							
							try {
								locationJson.put("accuracy", Float.parseFloat(c.getString(c.getColumnIndex(MobilityInterface.KEY_ACCURACY))));
							} catch (NumberFormatException e) {
								locationJson.put("accuracy", "NaN");
							} catch (JSONException e) {
								locationJson.put("accuracy", "NaN");
							}
							
							locationJson.put("time", Long.parseLong(c.getString(c.getColumnIndex(MobilityInterface.KEY_LOC_TIMESTAMP))));
							locationJson.put("timezone", c.getString(c.getColumnIndex(MobilityInterface.KEY_TIMEZONE)));
							
							mobilityPointJson.put("location", locationJson);
						}
						
					} catch (JSONException e) {
						Log.e(TAG, "error creating mobility json", e);
						NotificationHelper.showMobilityErrorNotification(this);
						throw new RuntimeException(e);
					}
					
					mobilityJsonArray.put(mobilityPointJson);
					
					c.moveToNext();
				}
				SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);
				OhmageApi api = new OhmageApi();
				response = api.mobilityUpload(Config.DEFAULT_SERVER_URL, username, hashedPassword, SharedPreferencesHelper.CLIENT_STRING, mobilityJsonArray.toString());
				
				if (response.getResult().equals(OhmageApi.Result.SUCCESS)) {
					Log.i(TAG, "Successfully uploaded " + String.valueOf(limit) + " mobility points.");
					helper.putLastMobilityUploadTimestamp(uploadAfterTimestamp);
					remainingCount -= limit;
					Log.i(TAG, "There are " + String.valueOf(remainingCount) + " mobility points remaining to be uploaded.");

					NotificationHelper.hideMobilityErrorNotification(this);
				} else {
					Log.e(TAG, "Failed to upload mobility points. Cancelling current round of mobility uploads.");
					
					switch (response.getResult()) {
					case FAILURE:
						Log.e(TAG, "Upload failed due to error codes: " + Utilities.stringArrayToString(response.getErrorCodes(), ", "));
						NotificationHelper.showMobilityErrorNotification(this);
						break;
						
					case INTERNAL_ERROR:
						Log.e(TAG, "Upload failed due to unknown internal error");
						NotificationHelper.showMobilityErrorNotification(this);
						break;
						
					case HTTP_ERROR:
						Log.e(TAG, "Upload failed due to network error");
						break;
					}
					
					break;						
				}
			}
			
			c.close();
		} else {
			Log.i(TAG, "No mobility points to upload.");
		}
		
		sendBroadcast(new Intent(UploadService.MOBILITY_UPLOAD_FINISHED));
	}
}
