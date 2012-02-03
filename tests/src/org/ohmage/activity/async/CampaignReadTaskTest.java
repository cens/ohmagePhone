package org.ohmage.activity.async;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApi.Result;
import org.ohmage.async.CampaignReadTask;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.test.CampaignCursor;
import org.ohmage.db.test.EmptyMockCursor;
import org.ohmage.db.test.OhmageUriMatcher;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests the {@link CampaignReadTask}
 * 
 * @author cketcham
 *
 */
public class CampaignReadTaskTest extends TestCase {

	private CampaignReadTask mReadTask;

	@Override
	protected void tearDown() throws Exception{
		mReadTask = null;
		super.tearDown();
	}

	private CampaignReadResponse generateCampaignJSON(Campaign... campaigns) {
		CampaignReadResponse response = new CampaignReadResponse();
		response.setResult(Result.SUCCESS);
		generateCampaignJSON(response, campaigns);
		return response;
	}

	private void generateCampaignJSON(CampaignReadResponse response, Campaign... campaigns) {
		try {
			JSONArray items = new JSONArray();
			JSONObject data = new JSONObject();
			for(Campaign c : campaigns) {
				items.put(c.mUrn);

				JSONObject campaign = new JSONObject();
				campaign.put("name", c.mName);
				campaign.put("creation_timestamp", c.mCreationTimestamp);
				campaign.put("description", "");
				if(c.mStatus == Campaign.STATUS_READY || c.mStatus == Campaign.STATUS_REMOTE)
					campaign.put("running_state","running");
				else
					campaign.put("running_state","not running");

				data.put(c.mUrn, campaign);
			}

			JSONObject metadata = new JSONObject();
			metadata.put("items", items);
			response.setMetadata(metadata);
			response.setData(data);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public abstract static class CampaignReadTaskContext extends MockContext {

		private MockContentResolver mResolver;
		private final Campaign[] mCampaigns;

		public CampaignReadTaskContext(Campaign... campaigns) {
			mCampaigns = campaigns;
		}

		public CampaignReadTaskContext(int status) {
			this(status, "0");
		}

		public CampaignReadTaskContext(int status, String creationTimestamp) {
			mCampaigns = new Campaign[1];
			mCampaigns[0] = new Campaign();
			mCampaigns[0].mUrn = CampaignCursor.DEFAULT_CAMPAIGN_URN;
			mCampaigns[0].mName = CampaignCursor.DEFAULT_CAMPAIGN_NAME;
			mCampaigns[0].mStatus = status;
			mCampaigns[0].mCreationTimestamp = creationTimestamp;
		}

		@Override
		public Context getApplicationContext() {
			return this;
		}

		@Override
		public ContentResolver getContentResolver() {
			if(mResolver == null) {

				mResolver = new MockContentResolver() { 
					@Override
					public ContentProviderResult[] applyBatch(String authority,
							ArrayList<ContentProviderOperation> operations)
									throws RemoteException, OperationApplicationException {
						CampaignReadTaskContext.this.applyBatch(operations);
						return null;
					}
				};

				mResolver.addProvider(DbContract.CONTENT_AUTHORITY, new MockContentProvider(this) {

					@Override
					public int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
						return 0;
					}

					@Override
					public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
						switch(OhmageUriMatcher.getMatcher().match(uri)) {
							case OhmageUriMatcher.CAMPAIGNS:
								return new CampaignCursor(projection, mCampaigns);
						}
						return new EmptyMockCursor();
					}
				});
			}
			return mResolver;
		}

		public  abstract void applyBatch(ArrayList<ContentProviderOperation> operations);

		public Campaign[] getCampaigns() {
			return mCampaigns;
		}
	}

	public interface TestOperations {
		public void test(ArrayList<ContentProviderOperation> operations);
	}

	private void helperTestStateChange(int localState, final int remoteState, final TestOperations tests) {
		final CampaignReadTaskContext context = new CampaignReadTaskContext(localState) {

			@Override
			public void applyBatch(ArrayList<ContentProviderOperation> operations) {
				tests.test(operations);
			} 
		};
		mReadTask = new CampaignReadTask(context);
		mReadTask.setOhmageApi(new OhmageApi() {

			@Override
			public CampaignReadResponse campaignRead(String serverUrl, String username, String hashedPassword, String client, String outputFormat, String campaignUrnList) {
				Campaign c = CampaignCursor.cloneCampaign(context.getCampaigns()[0]);

				if(remoteState == Campaign.STATUS_NO_EXIST)
					return generateCampaignJSON();
				else if(remoteState == Campaign.STATUS_OUT_OF_DATE)
					c.mCreationTimestamp = String.valueOf(Long.valueOf(c.mCreationTimestamp) + 1);
				else
					c.mStatus = remoteState;
				return generateCampaignJSON(c);
			}
		});
		mReadTask.loadInBackground();
	}

	public void testRunningCampaignOutOfDate() {
		helperTestStateChange(Campaign.STATUS_READY, Campaign.STATUS_OUT_OF_DATE, new TestOperations() {

			@Override
			public void test(ArrayList<ContentProviderOperation> operations) {
				// Check that we are doing an update to set the campaign to deleted
				assertEquals(1, operations.size());
				assertTrue(operations.get(0).toString().startsWith("mType: 2"));
				assertTrue(operations.get(0).toString().matches(".*mValues: .*campaign_status=" + Campaign.STATUS_OUT_OF_DATE + ".*" ));
			} 
		});
	}

	public void testRunningCampaignDeleted() {
		helperTestStateChange(Campaign.STATUS_READY, Campaign.STATUS_NO_EXIST, new TestOperations() {

			@Override
			public void test(ArrayList<ContentProviderOperation> operations) {
				// Check that we are doing an update to set the campaign to deleted
				assertEquals(1, operations.size());
				assertTrue(operations.get(0).toString().startsWith("mType: 2"));
				assertTrue(operations.get(0).toString().matches(".*mValues: .*campaign_status=" + Campaign.STATUS_NO_EXIST + ".*" ));
			} 
		});
	}

	public void testRemoteCampaignDeleted() {
		helperTestStateChange(Campaign.STATUS_REMOTE, Campaign.STATUS_NO_EXIST, new TestOperations() {

			@Override
			public void test(ArrayList<ContentProviderOperation> operations) {
				// Check that we are doing an update to set the campaign to deleted
				assertEquals(1, operations.size());
				assertTrue(operations.get(0).toString().startsWith("mType: 3"));
				assertTrue(operations.get(0).getUri().compareTo(Campaigns.buildCampaignUri(CampaignCursor.DEFAULT_CAMPAIGN_URN)) == 0);
			} 
		});
	}

	public void testRunningCampaignRunning() {
		helperTestStateChange(Campaign.STATUS_READY, Campaign.STATUS_READY, new TestOperations() {

			@Override
			public void test(ArrayList<ContentProviderOperation> operations) {
				// Check that we are doing an update to set the campaign to deleted
				assertEquals(1, operations.size());
				assertTrue(operations.get(0).toString().startsWith("mType: 2"));
				assertTrue(operations.get(0).toString().matches(".*mValues: .*campaign_status=" + Campaign.STATUS_READY + ".*" ));
			} 
		});
	}

	public void testRemoteCampaignRunning() {
		helperTestStateChange(Campaign.STATUS_REMOTE, Campaign.STATUS_READY, new TestOperations() {

			@Override
			public void test(ArrayList<ContentProviderOperation> operations) {
				// Check that we are doing an update to set the campaign to deleted
				assertEquals(1, operations.size());
				assertTrue(operations.get(0).toString().startsWith("mType: 1"));
				assertTrue(operations.get(0).toString().matches(".*mValues: .*campaign_status=" + Campaign.STATUS_REMOTE + ".*" ));
			} 
		});
	}

	public void testRunningCampaignStopped() {
		helperTestStateChange(Campaign.STATUS_READY, Campaign.STATUS_STOPPED, new TestOperations() {

			@Override
			public void test(ArrayList<ContentProviderOperation> operations) {
				// Check that we are doing an update to set the campaign to deleted
				assertEquals(1, operations.size());
				assertTrue(operations.get(0).toString().startsWith("mType: 2"));
				assertTrue(operations.get(0).toString().matches(".*mValues: .*campaign_status=" + Campaign.STATUS_STOPPED + ".*" ));
			} 
		});
	}

	public void testRemoteCampaignStopped() {
		helperTestStateChange(Campaign.STATUS_REMOTE, Campaign.STATUS_STOPPED, new TestOperations() {

			@Override
			public void test(ArrayList<ContentProviderOperation> operations) {
				assertEquals(1, operations.size());
				assertTrue(operations.get(0).toString().startsWith("mType: 3"));
				assertTrue(operations.get(0).getUri().compareTo(Campaigns.buildCampaignUri(CampaignCursor.DEFAULT_CAMPAIGN_URN)) == 0);
			}
		});
	}
}
