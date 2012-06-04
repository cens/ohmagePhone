package org.ohmage.responsesync;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.imageloader.ImageLoader;

import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Analytics.Status;
import edu.ucla.cens.systemlog.Log;

import org.codehaus.jackson.JsonNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.Config;
import org.ohmage.ConfigHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.Result;
import org.ohmage.OhmageApi.StreamingResponseListener;
import org.ohmage.OhmageApplication;
import org.ohmage.OhmageCache;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbProvider.Qualified;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.prompt.AbstractPrompt;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ResponseSyncService extends WakefulIntentService {
	private static final String TAG = "ResponseSyncService";

	// extras with which the service can be run
	/** If true, the service displays a toast when it completes */
	public static final String EXTRA_INTERACTIVE = "interactive";
	/** If present, runs the service only for the specified campaign */
	public static final String EXTRA_CAMPAIGN_URN = "campaign_urn";
	/** If present, the last synced time will be ignored */
	public static final String EXTRA_FORCE_ALL = "extra_force_all";

	private UserPreferencesHelper mPrefs;

	public ResponseSyncService() {
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
		// for the time being, we just pull all the surveys and update our feedback cache with them
		// FIXME: in the future, we should only download what we need...two strategies for that:
		// 1) maintain a timestamp of the most recent refresh and request only things after it
		// 2) somehow figure out which surveys the server has and we don't via the hashcode and sync accordingly
		
		Log.v(TAG, "Response sync service starting");
		
		if (!Config.ALLOWS_FEEDBACK) {
			Log.e(TAG, "Response sync service aborted, because feedback is not allowed in the preferences");
			return;
		}
		
		// ==================================================================
		// === 1. acquire handles to api and database, build campaign list
		// ==================================================================
		
		// grab an instance of the api connector so we can do calls to the server for responses
		OhmageApi api = new OhmageApi(this);
		mPrefs = new UserPreferencesHelper(this);
		String username = mPrefs.getUsername();
		String hashedPassword = mPrefs.getHashedPassword();

		if(!mPrefs.isAuthenticated()) {
			Log.e(TAG, "User isn't logged in, terminating task");

			return;
		}

		final ContentResolver cr = getContentResolver();
		final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		// and also create a list to hold some campaigns
		List<Campaign> campaigns;
        
		// helper instance for parsing utc timestamps
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setLenient(false);

        // if we received a campaign_urn in the intent, only download the data for that one campaign.
    	// the campaign object we create only inclues the mUrn field since we don't use anything else.
        if (intent.hasExtra(EXTRA_CAMPAIGN_URN)) {
        	campaigns = new ArrayList<Campaign>();
        	Campaign candidate = new Campaign();
        	candidate.mUrn = intent.getStringExtra(EXTRA_CAMPAIGN_URN);
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


		// attempt to construct a date range on which to query
		// we need three dates:
		// 1) far past, to get everything up to the cutoff date
		// 2) near future, to get everything since the cutoff date
		final SimpleDateFormat inputSDF = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
		Calendar farPast = new GregorianCalendar();
		farPast.add(Calendar.YEAR, -10);
		
		Calendar nearFuture = new GregorianCalendar();
		nearFuture.add(Calendar.DAY_OF_MONTH, 1);
		
		// and convert times to timestamps we can feed to the api
		String farPastDate = inputSDF.format(farPast.getTime());
		String nearFutureDate = inputSDF.format(nearFuture.getTime());
		
		// ==================================================================
		// === 3. process responses on server for each campaign
		// ==================================================================

		// we'll have to iterate through all the campaigns in which this user
		// is participating in order to gather all of their data
		for (final Campaign c : campaigns) {
			Log.v(TAG, "Requesting responses for campaign " + c.mUrn + "...");

			if(!mPrefs.isAuthenticated()) {
				Log.e(TAG, "User isn't logged in, terminating task");
				return;
			}

			String cutoffDate = null;
			if (!intent.getBooleanExtra(EXTRA_FORCE_ALL, false)) {
				// I add 1 second since the request is inclusive of this time
				cutoffDate = inputSDF.format(c.getLastDownloadedResponseTime(this) + 1000);
			}

			// ==================================================================
			// === 3a. download UUIDs of responses up to the cutoff date
			// ===   * anything not in this list should be deleted off the phone
			// ===   * anything in this list that's not on the phone should be downloaded
			// ==================================================================

			if(!mPrefs.isAuthenticated()) {
				Log.e(TAG, "User isn't logged in, terminating task");

				return;
			}

			api.surveyResponseRead(ConfigHelper.serverUrl(), username, hashedPassword, OhmageApi.CLIENT_NAME, c.mUrn, username, null, "urn:ohmage:survey:id", "json-rows", true, farPastDate, cutoffDate,
				new StreamingResponseListener() {
					List<String> responseIDs;
					
					@Override
					public void beforeRead() {
						responseIDs = new ArrayList<String>();
						Log.v(TAG, "Beginning UUID read...");
					}

					@Override
					public void readObject(JsonNode survey) {
						// build up a list of IDs
						// later, we'll attempt to delete everything that's not in this list
						responseIDs.add(survey.get("survey_key").asText());
						
						// TODO: we could also push back the cutoff date if we find
						// an ID that's present in this list that we don't have.
						// it's wasteful, but since we can't request items per ID
						// we have to just extend the time window on which we query.
						
						// TODO: ask server team for a way to specify responses by ID
					}

					@Override
					public void afterRead() {

						HashSet<String> idsSet = new HashSet<String>();
						idsSet.addAll(responseIDs);

						Cursor responses = cr.query(Responses.CONTENT_URI, new String[] { Responses.RESPONSE_UUID }, Responses.RESPONSE_STATUS + "=" + Response.STATUS_DOWNLOADED +
								" OR " + Responses.RESPONSE_STATUS + "=" + Response.STATUS_UPLOADED +
								" AND " + Qualified.RESPONSES_CAMPAIGN_URN + "=?", new String[] { c.mUrn }, null);


						String uuid;
						while(responses.moveToNext()) {
							uuid = responses.getString(0);
							if(!idsSet.contains(uuid)) {
								operations.add(ContentProviderOperation.newDelete(Responses.CONTENT_URI)
										.withSelection("(" + Responses.RESPONSE_STATUS + "=" + Response.STATUS_DOWNLOADED +
												" OR " + Responses.RESPONSE_STATUS + "=" + Response.STATUS_UPLOADED + ")" +
												" AND " + Responses.CAMPAIGN_URN + "=?" + " AND " + Responses.RESPONSE_UUID + "=?",
												new String[] {c.mUrn, uuid }).build());
							}
						}
						responses.close();
					}
			});

			// ==================================================================
			// === 3b. download responses from after the cutoff date
			// ==================================================================

			// also maintain a list of photo UUIDs that may or may not be on the device
			// this is campaign-response-specific, which is why it's happening in this loop over the campaigns
			class ResponseImage {
				public ResponseImage(String c, String id) {
					campaign = c;
					uuid = id;
				}
				String campaign;
				String uuid;
			}
			final LinkedList<ResponseImage> responsePhotos = new LinkedList<ResponseImage>();

			if(!mPrefs.isAuthenticated()) {
				Log.e(TAG, "User isn't logged in, terminating task");

				return;
			}

			// do the call and process the streaming response data
			api.surveyResponseRead(ConfigHelper.serverUrl(), username, hashedPassword, OhmageApi.CLIENT_NAME, c.mUrn, username, null, null, "json-rows", true, cutoffDate, nearFutureDate,
				new StreamingResponseListener() {
					int curRecord;
					
					@Override
					public void beforeRead() {
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
										String value = (curItem.get("prompt_response").isValueNode()) ? curItem.get("prompt_response").asText() : curItem.get("prompt_response").toString();
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
											responsePhotos.add(new ResponseImage(candidate.campaignUrn, value));
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

							operations.add(ContentProviderOperation.newInsert(Responses.CONTENT_URI).withValues(candidate.toCV()).build());
						}
				        catch (JSONException e) {
							Log.e(TAG, "Problem parsing response json: " + e.getMessage(), e);
						}
					}
					
					@Override
					public void afterRead() {
						Log.v(TAG, "Finished record read");
					}
					
					@Override
					public void readResult(Result result, String[] errorCodes) {
						String error = null;
						
						switch (result) {
							case FAILURE:			error = "survey response query failed";
							case HTTP_ERROR:		error = "http error during request";
							case INTERNAL_ERROR:	error = "internal error during request";
						}
						
						if (error != null) {
							Log.e(TAG, error);
							return;
						}

						// We can now download the thumbnails for each response from newest to oldest.
						// We only need to download OhmageApplication.MAX_DISK_CACHE_SIZE amount of data.
						ImageLoader imageLoader = ImageLoader.get(ResponseSyncService.this);
						long downloadedAmount = 0;
						long time = System.currentTimeMillis();
						String url;
						for(int i=0; i < responsePhotos.size(); i++) {
							ResponseImage responseImage = responsePhotos.get(i);
							if(!mPrefs.isAuthenticated()) {
								Log.e(TAG, "User isn't logged in, terminating task");

								return;
							}
							try {
								if(downloadedAmount < OhmageApplication.MAX_DISK_CACHE_SIZE) {
									url = OhmageApi.defaultImageReadUrl(responseImage.uuid, responseImage.campaign, "small");
									imageLoader.prefetchBlocking(url);
									File file = OhmageCache.getCachedFile(ResponseSyncService.this, URI.create(url));
									if(file == null) {
										Log.e(TAG, "Unable to save thumbnail, aborting sync process");
										return;
									}
									downloadedAmount += file.length();
									file.setLastModified(time - 1000 * i);
								}

								// As we download thumbnails, we can delete the old images
								Response.getTemporaryResponsesMedia(ResponseSyncService.this, responseImage.uuid).delete();
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						// Now that we have downloaded potentially a lot of images, we should remove any old ones
						OhmageApplication.checkCacheUsage();
					}
				});
		}

		if(!mPrefs.isAuthenticated()) {
			Log.e(TAG, "User isn't logged in, terminating task");

			return;
		}

		// Apply the operations
		try {
			cr.applyBatch(DbContract.CONTENT_AUTHORITY, operations);
		} catch (RemoteException e) {
			Log.e(TAG, "Error applying database operations");
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			Log.e(TAG, "Error applying database operations");
			e.printStackTrace();
		}

		// ==================================================================
		// === 4. complete!
		// ==================================================================
		
		Log.v(TAG, "Response sync service complete");
		
		if (intent.getBooleanExtra(EXTRA_INTERACTIVE, false)) {
			Toast.makeText(this, "Response sync service complete", Toast.LENGTH_SHORT);
		}
	}
}