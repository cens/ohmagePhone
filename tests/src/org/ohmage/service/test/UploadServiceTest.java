package org.ohmage.service.test;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.db.test.MockContentProviderContext;
import org.ohmage.db.test.ResponseCursor;
import org.ohmage.service.SurveyGeotagService;
import org.ohmage.service.UploadService;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.test.ServiceTestCase;
import android.test.mock.MockContext;

import java.io.File;

/**
 * Tests the {@link UploadService}
 * We might want to change how these tests are performed, they are fairly tightly
 * coupled with the implementation of the service. It might be better if it was less coupled
 * to implementation and more to functionality
 * @author cketcham
 *
 */
public class UploadServiceTest extends ServiceTestCase<UploadService> {

	private OhmageApi mOhmageApi = new OhmageApi() {

		@Override
		public UploadResponse surveyUpload(String serverUrl, String username, String hashedPassword, String client, String campaignUrn, String campaignCreationTimestamp, String responseJson, File [] photos) {
			return new UploadResponse(Result.SUCCESS, null);
		}
	};

	public UploadServiceTest() {
		super(UploadService.class);
	}

	/**
	 * Test uploading response with an image
	 * @throws InterruptedException
	 */
	public void testResponseUploadWithImage() throws InterruptedException {
		fail("todo");
	}

	/**
	 * Test that the response is formatted correctly for the server
	 * @throws InterruptedException
	 */
	public void testResponseIsFormattedForServer() throws InterruptedException {
		Intent i =new Intent();
		i.setData(Responses.CONTENT_URI);
		i.putExtra("upload_surveys", true);

		final Response response = new Response();
		response._id = 3;
		response.campaignUrn = ResponseCursor.MOCK_CAMPAIGN_URN;
		response.timezone = "America/Los_Angeles";
		response.time = Long.parseLong("1321489758496");
		response.surveyLaunchContext = "{\"launch_time\":\"2011-11-16 16:29:00\",\"active_triggers\":[]}";
		response.surveyId = "AdvertisingMedia";
		response.locationStatus = SurveyGeotagService.LOCATION_UNAVAILABLE;
		response.response = "[]";

		UploadServiceResponsesContext context = new UploadServiceResponsesContext(mContext, i.getData(), response);

		setContext(context);

		startService(i, new OhmageApi() {

			@Override
			public UploadResponse surveyUpload(String serverUrl, String username, String hashedPassword, String client, String campaignUrn, String campaignCreationTimestamp, String responseJson, File [] photos) {
				assertEquals(response.campaignUrn, campaignUrn);
				try {
					JSONArray json = new JSONArray(responseJson);
					assertEquals(1, json.length());
					JSONObject object = json.getJSONObject(0);
					//TODO: timestamp and prompt responses
					assertEquals(response.timezone, object.getString("timezone"));
					assertEquals(response.time, object.getLong("time"));
					assertEquals(response.surveyLaunchContext, object.getString("survey_launch_context"));
					assertEquals(response.surveyId, object.getString("survey_id"));
					assertEquals(response.locationStatus, object.getString("location_status"));
					assertFalse(object.has("location"));

					JSONArray responses = new JSONArray(object.getString("responses"));

				} catch (JSONException e) {
					fail();
				}
				return new UploadResponse(Result.SUCCESS, null);
			}
		});
	}

	/**
	 * Tests that the upload service fails gracefully with invalid uri
	 * @throws InterruptedException
	 */
	public void testUploadInvalidUri() throws InterruptedException {
		Intent i =new Intent();
		i.setData(Uri.parse("content://blah"));
		i.putExtra("upload_surveys", true);

		UploadServiceResponsesContext context = new UploadServiceResponsesContext(mContext, i.getData(), 5) {
			int first = 0;

			@Override
			public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
				if(first++ == 0) {
					assertEquals(Responses.buildResponseUri(0), uri);
				}
				return super.query(uri, projection, selection, selectionArgs, sortOrder);
			}
		};

		setContext(context);
		startService(i);
	}

	/**
	 * Tests that the upload service fails gracefully with junk uri
	 * @throws InterruptedException
	 */
	public void testUploadBadUri() throws InterruptedException {
		Intent i =new Intent();
		i.setData(Uri.parse("blah"));
		i.putExtra("upload_surveys", true);

		UploadServiceResponsesContext context = new UploadServiceResponsesContext(mContext, i.getData(), 5) {
			int first = 0;

			@Override
			public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
				if(first++ == 0) {
					assertEquals(Responses.buildResponseUri(0), uri);
				}
				return super.query(uri, projection, selection, selectionArgs, sortOrder);
			}
		};

		setContext(context);
		startService(i);
	}

	/**
	 * Tests that the upload service fails gracefully when given a campaign uri
	 * @throws InterruptedException
	 */
	public void testUploadCampaign() throws InterruptedException {
		Intent i =new Intent();
		i.setData(Responses.buildResponseUri(0));
		i.putExtra("upload_surveys", true);

		UploadServiceResponsesContext context = new UploadServiceResponsesContext(mContext, i.getData(), 5) {
			int first = 0;

			@Override
			public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
				if(first++ == 0) {
					assertEquals(Responses.buildResponseUri(0), uri);
				}
				return super.query(uri, projection, selection, selectionArgs, sortOrder);
			}
		};

		setContext(context);
		startService(i);
	}

	/**
	 * Tests that the {@link UploadService} will try to upload a single
	 * response given to it by the intent
	 * @throws InterruptedException 
	 */
	public void testUploadSingleResponse() throws InterruptedException {
		Intent i =new Intent();
		i.setData(Responses.buildResponseUri(0));
		i.putExtra("upload_surveys", true);

		UploadServiceResponsesContext context = new UploadServiceResponsesContext(mContext, i.getData(), 5) {
			int first = 0;

			@Override
			public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
				if(first++ == 0) {
					assertEquals(Responses.buildResponseUri(0), uri);
				}
				return super.query(uri, projection, selection, selectionArgs, sortOrder);
			}
		};

		setContext(context);
		startService(i);
	}

	/**
	 * Tests setup before the upload happens
	 * <ui>
	 * <li>Downloaded responses are not included</li>
	 * <li>Uploaded responses are not included</li>
	 * <li>Responses which are waiting for loction are not included</li>
	 * </ui>
	 * @throws InterruptedException
	 */
	public void testSetupResponsesForUpload() throws InterruptedException {
		Intent i =new Intent();
		i.setData(Responses.CONTENT_URI);
		i.putExtra("upload_surveys", true);

		UploadServiceResponsesContext context = new UploadServiceResponsesContext(mContext, i.getData(), 5) {
			int first = 0;

			@Override
			public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
				if(first++ == 0) {
					assertEquals(Responses.CONTENT_URI, uri);
					assertTrue(selection.contains("response_status!=" + Response.STATUS_DOWNLOADED));
					assertTrue(selection.contains("response_status!=" + Response.STATUS_UPLOADED));
					assertTrue(selection.contains("response_status!=" + Response.STATUS_WAITING_FOR_LOCATION));
				}
				return super.query(uri, projection, selection, selectionArgs, sortOrder);
			}
		};

		setContext(context);
		startService(i);
	}

	/**
	 * Tests that all the correct items are set to queued when the service starts
	 * @throws InterruptedException
	 */
	public void testResponsesQueued() throws InterruptedException {
		Intent i =new Intent();
		i.setData(Responses.CONTENT_URI);
		i.putExtra("upload_surveys", true);

		UploadServiceResponsesContext context = new UploadServiceResponsesContext(mContext, i.getData(), 5) {
			int first = 0;

			@Override
			public int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
				if(first++ == 0) {
					assertEquals(Responses.CONTENT_URI, uri);
					//TODO: make sure this will only get responses which should be uploaded
					assertTrue(where.contains("response_status!=" + Response.STATUS_DOWNLOADED));
					assertTrue(where.contains("response_status!=" + Response.STATUS_UPLOADED));
					assertTrue(where.contains("response_status!=" + Response.STATUS_WAITING_FOR_LOCATION));
					assertTrue(values.getAsLong(Responses.RESPONSE_STATUS) == Response.STATUS_QUEUED);
				}
				return super.update(uri, values, where, selectionArgs);
			}
		};

		setContext(context);
		startService(i);
	}

	/**
	 * As each response starts to upload it should be set to uploading
	 * @throws InterruptedException
	 */
	public void testEachUploadingState() throws InterruptedException {
		Intent i =new Intent();
		i.setData(Responses.CONTENT_URI);
		i.putExtra("upload_surveys", true);

		UploadServiceResponsesContext context = new UploadServiceResponsesContext(mContext, i.getData(), 5) {
			int update = 0;

			@Override
			public int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
				if(update > 0 && update < 6) {
					assertResponse(0, uri, where);
					if(update%2==1) {
						// Set the first one to uploading
						assertTrue(values.getAsLong(Responses.RESPONSE_STATUS) == Response.STATUS_UPLOADING);	
					} else {
						// then set it to uploaded
						assertTrue(values.getAsLong(Responses.RESPONSE_STATUS) == Response.STATUS_UPLOADED);
					}
				}
				update++;
				return super.update(uri, values, where, selectionArgs);
			}
		};

		setContext(context);


		startService(i);
	}

	public void testCampaignDoesNotExistError() throws InterruptedException {
		errorTestHelper("0700", Response.STATUS_ERROR_CAMPAIGN_NO_EXIST, Campaign.STATUS_NO_EXIST);
	}

	public void testInvalidUserRoleError() throws InterruptedException {
		errorTestHelper("0707", Response.STATUS_ERROR_INVALID_USER_ROLE, Campaign.STATUS_INVALID_USER_ROLE);
	}

	public void testCampaignStopped() throws InterruptedException {
		errorTestHelper("0703", Response.STATUS_ERROR_CAMPAIGN_STOPPED, Campaign.STATUS_STOPPED);
	}

	public void testCampaignOutOfDate() throws InterruptedException {
		errorTestHelper("0710", Response.STATUS_ERROR_CAMPAIGN_OUT_OF_DATE, Campaign.STATUS_OUT_OF_DATE);
	}

	/**
	 * Help test error codes cause the correct updates to the response and campaigns
	 * @param code
	 * @param responseStatusId
	 * @param campaignStatusId
	 * @throws InterruptedException
	 */
	private void errorTestHelper(final String code, final int responseStatusId, final int campaignStatusId) throws InterruptedException {
		Intent i =new Intent();
		i.setData(Responses.CONTENT_URI);
		i.putExtra("upload_surveys", true);

		UploadServiceResponsesContext context = new UploadServiceResponsesContext(mContext, i.getData(), 5) {
			int update = 0;
			@Override
			public int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
				if(update == 2) {
					assertCampaign(ResponseCursor.MOCK_CAMPAIGN_URN, uri, where);
					assertTrue(values.getAsLong(Campaigns.CAMPAIGN_STATUS) == campaignStatusId);
				} else if(update == 3) {	
					assertResponse(0, uri, where);
					assertTrue(values.getAsLong(Responses.RESPONSE_STATUS) == responseStatusId);
				}
				update++;
				return 1;
			}
		};

		setContext(context);

		startService(i, new OhmageApi() {

			@Override
			public UploadResponse surveyUpload(String serverUrl, String username, String hashedPassword, String client, String campaignUrn, String campaignCreationTimestamp, String responseJson, File [] photos) {
				return new UploadResponse(Result.FAILURE, new String[] { code });
			}
		});
	}

	@Override
	protected void setupService() {
		super.setupService();

		getService().setOhmageApi(mOhmageApi);
	}

	@Override
	protected void startService(Intent intent) {
		super.startService(intent);

		// The WakefulIntentService requires that it gets a wake lock before it is started
		// The MockContext prevents it from actually starting the service since we already
		// took care of that
		WakefulIntentService.sendWakefulWork(new MockContext() {

			@Override
			public Context getApplicationContext() {
				return getService().getApplicationContext();
			}

			@Override
			public ComponentName startService(Intent intent) {
				return null;
			}
		}, new Intent());
	}

	/**
	 * Sets the api the service uses to help with testing
	 * @param intent
	 * @param api
	 */
	protected void startService(Intent intent, OhmageApi api) {
		mOhmageApi = api;
		startService(intent);
	}

	/**
	 * Context used for testing uploading survey responses. 
	 * This context makes it easy to block until the upload operation has completed
	 * @author cketcham
	 *
	 */
	private static class UploadServiceResponsesContext extends MockContentProviderContext {

		private final Uri mUri;
		private final Response[] mResponses;

		public UploadServiceResponsesContext(Context context, Uri uri, int count) {
			super(context);
			mUri = uri;
			mResponses = new Response[count];
		}

		public UploadServiceResponsesContext(Context context, Uri uri, Response... responses) {
			super(context);
			mUri = uri;
			mResponses = responses;
		}

		@Override
		public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
			if(uri.compareTo(mUri) == 0)
				return new ResponseCursor(projection, mResponses);
			return super.query(uri, projection, selection, selectionArgs, sortOrder);
		}

		protected Uri getUri() {
			return mUri;
		}
	}

	/**
	 * Asserts that this db operation applies to the response with the given id. 
	 * Might not be valid for certain cases of where.
	 * @param id
	 * @param uri
	 * @param where
	 */
	protected void assertResponse(int id, Uri uri, String where) {
		assertTrue(Responses.buildResponseUri(id).equals(uri) || (Responses.CONTENT_URI.equals(uri) && where.contains(Responses._ID + "=" + id)));
	}

	/**
	 * Asserts that the db operation applies to the given campaign urn. 
	 * Might not be valid for certain cases of where.
	 * @param urn
	 * @param uri
	 * @param where
	 */
	protected void assertCampaign(String urn, Uri uri, String where) {
		assertTrue(Campaigns.buildCampaignUri(urn).equals(uri) || (Campaigns.CONTENT_URI.equals(uri) && where.contains(Campaigns.CAMPAIGN_URN + "='" + urn + "'")));
	}
}
