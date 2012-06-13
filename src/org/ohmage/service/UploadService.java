package org.ohmage.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.mobility.glue.MobilityInterface;
import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Analytics.Status;
import edu.ucla.cens.systemlog.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.ConfigHelper;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.MediaPart;
import org.ohmage.OhmageApi.Result;
import org.ohmage.OhmageApi.UploadResponse;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.SurveyPrompts;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.db.Models.Response;

import java.io.File;
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

	private OhmageApi mApi;

	private boolean isBackground;

	public UploadService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Analytics.service(this, Status.ON);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Analytics.service(this, Status.OFF);
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		
		if(mApi == null)
			setOhmageApi(new OhmageApi(this));

		isBackground = intent.getBooleanExtra(EXTRA_BACKGROUND, false);

		if (intent.getBooleanExtra(EXTRA_UPLOAD_SURVEYS, false)) {
			uploadSurveyResponses(intent);
		}
		
		if (intent.getBooleanExtra(EXTRA_UPLOAD_MOBILITY, false)) {
			uploadMobility(intent);
		}
	}

	public void setOhmageApi(OhmageApi api) {
		mApi = api;
	}

	private void uploadSurveyResponses(Intent intent) {
		String serverUrl = ConfigHelper.serverUrl();
		
		UserPreferencesHelper helper = new UserPreferencesHelper(this);
		String username = helper.getUsername();
		String hashedPassword = helper.getHashedPassword();
		boolean uploadErrorOccurred = false;
		boolean authErrorOccurred = false;
		
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

		// If there is no data we should just return
		if(cursor == null)
			return;
		else if(!cursor.moveToFirst()) {
			cursor.close();
			return;
		}

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
			final ArrayList<MediaPart> media = new ArrayList<MediaPart>();
            
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
					String provider = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_PROVIDER));
					locationJson.put("provider", provider);
					Log.i(TAG, "Response uploaded with " + provider + " location");
					locationJson.put("accuracy", cursor.getFloat(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_ACCURACY)));
					locationJson.put("time", cursor.getLong(cursor.getColumnIndex(Responses.RESPONSE_LOCATION_TIME)));
					locationJson.put("timezone", cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_TIMEZONE)));
					responseJson.put("location", locationJson);
				} else {
					Log.w(TAG, "Response uploaded without a location");
				}
				responseJson.put("survey_id", cursor.getString(cursor.getColumnIndex(Responses.SURVEY_ID)));
				responseJson.put("survey_launch_context", new JSONObject(cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_SURVEY_LAUNCH_CONTEXT))));
				responseJson.put("responses", new JSONArray(cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_JSON))));
				
				ContentResolver cr2 = getContentResolver();
				Cursor promptsCursor = cr2.query(Responses.buildPromptResponsesUri(responseId), new String [] {PromptResponses.PROMPT_RESPONSE_VALUE, SurveyPrompts.SURVEY_PROMPT_TYPE}, SurveyPrompts.SURVEY_PROMPT_TYPE + "=? OR " + SurveyPrompts.SURVEY_PROMPT_TYPE + "=?", new String [] { "photo", "video" }, null);
				
				while (promptsCursor.moveToNext()) {
					media.add(new MediaPart(new File(Response.getResponseMediaUploadDir(), promptsCursor.getString(0)), promptsCursor.getString(1)));
				}
				
				promptsCursor.close();
				
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			
			responsesJsonArray.put(responseJson);
			
			String campaignUrn = cursor.getString(cursor.getColumnIndex(Responses.CAMPAIGN_URN));
			String campaignCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_CREATED));

			OhmageApi.UploadResponse response = mApi.surveyUpload(serverUrl, username, hashedPassword, OhmageApi.CLIENT_NAME, campaignUrn, campaignCreationTimestamp, responsesJsonArray.toString(), media);
			response.handleError(this);

			int responseStatus = Response.STATUS_UPLOADED;

			if (response.getResult() == Result.SUCCESS) {
				NotificationHelper.hideUploadErrorNotification(this);
			} else {
				responseStatus = Response.STATUS_ERROR_OTHER;
				
                switch (response.getResult()) {
                    case FAILURE:
                        if (response.hasAuthError()) {
                            responseStatus = Response.STATUS_ERROR_AUTHENTICATION;
                        } else {
                            uploadErrorOccurred = true;

                            if (response.getErrorCodes().contains("0700")) {
                                responseStatus = Response.STATUS_ERROR_CAMPAIGN_NO_EXIST;
                            } else if (response.getErrorCodes().contains("0707")) {
                                responseStatus = Response.STATUS_ERROR_INVALID_USER_ROLE;
                            } else if (response.getErrorCodes().contains("0703")) {
                                responseStatus = Response.STATUS_ERROR_CAMPAIGN_STOPPED;
                            } else if (response.getErrorCodes().contains("0710")) {
                                responseStatus = Response.STATUS_ERROR_CAMPAIGN_OUT_OF_DATE;
                            }
                        }

                        break;

                    case INTERNAL_ERROR:
                        uploadErrorOccurred = true;
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
		
		if (isBackground && uploadErrorOccurred) {
			NotificationHelper.showUploadErrorNotification(this);
		}
	}
	
	private void uploadMobility(Intent intent) {
		
		sendBroadcast(new Intent(UploadService.MOBILITY_UPLOAD_STARTED));
		
		boolean uploadSensorData = true;
		
		UserPreferencesHelper helper = new UserPreferencesHelper(this);
		
		String username = helper.getUsername();
		String hashedPassword = helper.getHashedPassword();
		long uploadAfterTimestamp = helper.getLastMobilityUploadTimestamp();
		if (uploadAfterTimestamp == 0) {
			uploadAfterTimestamp = helper.getLoginTimestamp();
		}
		
		Long now = System.currentTimeMillis();
		Cursor c = MobilityInterface.getMobilityCursor(this, uploadAfterTimestamp);
		
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
						if(isBackground)
							NotificationHelper.showMobilityErrorNotification(this);
						throw new RuntimeException(e);
					}
					
					mobilityJsonArray.put(mobilityPointJson);
					
					c.moveToNext();
				}
				UploadResponse response = mApi.mobilityUpload(ConfigHelper.serverUrl(), username, hashedPassword, OhmageApi.CLIENT_NAME, mobilityJsonArray.toString());
				response.handleError(this);

				if (response.getResult().equals(OhmageApi.Result.SUCCESS)) {
					Log.i(TAG, "Successfully uploaded " + String.valueOf(limit) + " mobility points.");
					helper.putLastMobilityUploadTimestamp(uploadAfterTimestamp);
					remainingCount -= limit;
					Log.i(TAG, "There are " + String.valueOf(remainingCount) + " mobility points remaining to be uploaded.");

					NotificationHelper.hideMobilityErrorNotification(this);
				} else if(isBackground && !response.hasAuthError() && !response.getResult().equals(OhmageApi.Result.HTTP_ERROR)) {
					NotificationHelper.showMobilityErrorNotification(this);
				}
			}
			
			c.close();
		} else {
			Log.i(TAG, "No mobility points to upload.");
		}
		
		sendBroadcast(new Intent(UploadService.MOBILITY_UPLOAD_FINISHED));
	}
}
