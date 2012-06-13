package org.ohmage.async;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import edu.ucla.cens.systemlog.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.ConfigHelper;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApi.Response;
import org.ohmage.OhmageApi.Result;
import org.ohmage.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.activity.ErrorDialogActivity;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class CampaignReadTask extends AuthenticatedTaskLoader<CampaignReadResponse> {

	private static final String TAG = "CampaignReadTask";
	private OhmageApi mApi;
	private final Context mContext;
	private final UserPreferencesHelper mPrefs;

	public CampaignReadTask(Context context) {
		super(context);
		mContext = context;
		mPrefs = new UserPreferencesHelper(mContext);
	}

	public CampaignReadTask(Context context, String username, String hashedPassword) {
		super(context, username, hashedPassword);
		mContext = context;
		mPrefs = new UserPreferencesHelper(mContext);
	}

	@Override
	public CampaignReadResponse loadInBackground() {
		if(mApi == null)
			mApi = new OhmageApi(mContext);

		CampaignReadResponse response = mApi.campaignRead(ConfigHelper.serverUrl(), getUsername(), getHashedPassword(), OhmageApi.CLIENT_NAME, "short", null);

		if (response.getResult() == Result.SUCCESS) {
			ContentResolver cr = getContext().getContentResolver();

			//build list of urns of all campaigns
			Cursor cursor = cr.query(Campaigns.CONTENT_URI, new String [] {Campaigns.CAMPAIGN_URN, Campaigns.CAMPAIGN_CREATED, Campaigns.CAMPAIGN_STATUS}, null, null, null);
			cursor.moveToFirst();

			HashMap<String, Campaign> localCampaignUrns = new HashMap<String, Campaign>();
			HashSet<String> toDelete = new HashSet<String>();

			for (int i = 0; i < cursor.getCount(); i++) {
				if(cursor.getInt(2) != Campaign.STATUS_REMOTE) {
					// Here we store a list of campaigns we have downloaded
					Campaign c = new Campaign();
					c.mUrn = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_URN));
					c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_CREATED));
					localCampaignUrns.put(c.mUrn, c);
				} else {
					// Here we store a list of campaigns we may have to delete if the server doesn't return them
					toDelete.add(cursor.getString(0));
				}
				cursor.moveToNext();
			}

			cursor.close();

			// The old urn thats used for single campaign mode. This has to be determined before the new data is downloaded in case the
			// state changes. This is used to determine if there is a better choice for the single campaign mode after the download is complete.
			final String oldUrn = Campaign.getSingleCampaign(getContext());

			ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

			try { // parse response
				JSONArray jsonItems = response.getMetadata().getJSONArray("items");

				for(int i = 0; i < jsonItems.length(); i++) {
					Campaign c = new Campaign();
					JSONObject data = response.getData();
					try {
						c.mUrn = jsonItems.getString(i); 
						c.mName = data.getJSONObject(c.mUrn).getString("name");
						c.mDescription = data.getJSONObject(c.mUrn).getString("description");
						c.mCreationTimestamp = data.getJSONObject(c.mUrn).getString("creation_timestamp");
						c.mDownloadTimestamp = null;
						c.mXml = null;
						c.mStatus = Campaign.STATUS_REMOTE;
						c.mPrivacy = data.getJSONObject(c.mUrn).optString("privacy_state", Campaign.PRIVACY_UNKNOWN);
						c.mIcon = data.getJSONObject(c.mUrn).optString("icon_url", null);
						c.updated = startTime;
						boolean running = data.getJSONObject(c.mUrn).getString("running_state").equalsIgnoreCase("running");
						boolean participant = false;
						JSONArray roles = data.getJSONObject(c.mUrn).getJSONArray("user_roles");
						for(int j=0;j<roles.length();j++) {
							if("participant".equals(roles.getString(j))) {
								participant = true;
								break;
							}
						}

						if (localCampaignUrns.containsKey(c.mUrn)) { //campaign has already been downloaded

							Campaign old = localCampaignUrns.get(c.mUrn);
							localCampaignUrns.remove(c.mUrn);

							ContentValues values = new ContentValues();
							// FAISAL: include things here that may change at any time on the server
							values.put(Campaigns.CAMPAIGN_PRIVACY, c.mPrivacy);
							values.put(Campaigns.CAMPAIGN_UPDATED, c.updated);

							if(!c.mCreationTimestamp.equals(old.mCreationTimestamp))
								values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_OUT_OF_DATE);
							else if(running && !participant)
								values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_INVALID_USER_ROLE);
							else
								values.put(Campaigns.CAMPAIGN_STATUS, (running) ? Campaign.STATUS_READY : Campaign.STATUS_STOPPED);

							operations.add(ContentProviderOperation.newUpdate(Campaigns.buildCampaignUri(c.mUrn)).withValues(values).build());

						} else { //campaign has not been downloaded

							if (running) { //campaign is running
								// We don't need to delete it
								toDelete.remove(c.mUrn);
								operations.add(ContentProviderOperation.newInsert(Campaigns.CONTENT_URI).withValues(c.toCV()).build());
							}
						}
					} catch (JSONException e) {
						Log.e(TAG, "Error parsing json data for " + jsonItems.getString(i), e);
					}
				}
			} catch (JSONException e) {
				Log.e(TAG, "Error parsing response json: 'items' key doesn't exist or is not a JSONArray", e);
			}

			for(String urn : toDelete) {
				operations.add(ContentProviderOperation.newDelete(Campaigns.buildCampaignUri(urn)).build());
			}

			//leftover local campaigns were not returned by campaign read, therefore must be in some unavailable state
			for (String urn : localCampaignUrns.keySet()) {
				ContentValues values = new ContentValues();
				values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_NO_EXIST);
				values.put(Campaigns.CAMPAIGN_UPDATED, startTime);
				operations.add(ContentProviderOperation.newUpdate(Campaigns.buildCampaignUri(urn)).withValues(values).build());
			}

			if(!mPrefs.isAuthenticated()) {
				Log.e(TAG, "User isn't logged in, terminating task");
				return response;
			}

			try {
				cr.applyBatch(DbContract.CONTENT_AUTHORITY, operations);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OperationApplicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// If we are in single campaign mode, we should automatically download the xml for the best campaign
			if(ConfigHelper.isSingleCampaignMode()) {
				Campaign newCampaign = Campaign.getFirstAvaliableCampaign(getContext());

				// If there is no good new campaign, the new campaign is different from the old one, or the old one is out of date, we should update it
				if(newCampaign == null || TextUtils.isEmpty(newCampaign.mUrn) || !newCampaign.mUrn.equals(oldUrn) || newCampaign.mStatus == Campaign.STATUS_OUT_OF_DATE) {

					// Download the new xml
					if(newCampaign != null && !TextUtils.isEmpty(newCampaign.mUrn)) {
						CampaignXmlDownloadTask campaignDownloadTask = new CampaignXmlDownloadTask(getContext(), newCampaign.mUrn, getUsername(), getHashedPassword());
						campaignDownloadTask.registerListener(0, new OnLoadCompleteListener<OhmageApi.Response>() {

							@Override
							public void onLoadComplete(Loader<Response> loader, Response data) {
								// If it was successful then we can set the single campaign
								if(data.getResult() == Result.SUCCESS) {

									if(!TextUtils.isEmpty(oldUrn)) {
										// If we are removing the old campaign show the notification
										Intent intent = new Intent(getContext(), ErrorDialogActivity.class);
										intent.putExtra(ErrorDialogActivity.EXTRA_TITLE, getContext().getString(R.string.single_campaign_changed_title));
										intent.putExtra(ErrorDialogActivity.EXTRA_MESSAGE, getContext().getString(R.string.single_campaign_changed_message));
										NotificationHelper.showNotification(getContext(), getContext().getString(R.string.single_campaign_changed_title), getContext().getString(R.string.click_more_info), intent);
									}
								}
							}
						});
						campaignDownloadTask.startLoading();
						campaignDownloadTask.waitForLoader();
					}
				}

				// Make all other campaigns remote
				Campaign.ensureSingleCampaign(getContext());
			}
		} 

		return response;
	}

	public void setOhmageApi(OhmageApi api) {
		mApi = api;
	}
}