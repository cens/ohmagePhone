package org.ohmage.activity;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApi.Result;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.slezica.tools.async.ManagedAsyncTask;
import java.util.concurrent.ExecutionException;

class CampaignReadTask extends ManagedAsyncTask<String, Void, CampaignReadResponse>{
	
	private static final String TAG = "CampaignReadTask";
		
	private final Context mContext;
//	private final Context mActivity;

	public CampaignReadTask(FragmentActivity activity) {
		super(activity);
//		mActivity = activity;
		mContext = activity.getApplicationContext();
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected CampaignReadResponse doInBackground(String... params) {
		String username = params[0];
		String hashedPassword = params[1];
		OhmageApi api = new OhmageApi(mContext);
		CampaignReadResponse response = api.campaignRead(SharedPreferencesHelper.DEFAULT_SERVER_URL, username, hashedPassword, "android", "short", null);
		
		if (response.getResult() == Result.SUCCESS) {
			ContentResolver cr = mContext.getContentResolver();
			
			//delete all remote campaigns from content provider
			cr.delete(Campaigns.CONTENT_URI, Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_REMOTE, null);
			
			//build list of urns of all downloaded (local) campaigns
			Cursor cursor = cr.query(Campaigns.CONTENT_URI, new String [] {Campaigns.CAMPAIGN_URN, Campaigns.CAMPAIGN_CREATED}, Campaigns.CAMPAIGN_STATUS + "!=" + Campaign.STATUS_REMOTE, null, null);
			cursor.moveToFirst();
			
			HashMap<String, Campaign> localCampaignUrns = new HashMap<String, Campaign>();
			
			for (int i = 0; i < cursor.getCount(); i++) {
				Campaign c = new Campaign();
				c.mUrn = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_URN));
				c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaigns.CAMPAIGN_CREATED));
				localCampaignUrns.put(c.mUrn, c);
				cursor.moveToNext();
			}
    		
    		cursor.close();
    		
			// The old urn thats used for single campaign mode. This has to be determined before the new data is downloaded in case the
			// state changes. This is used to determine if there is a better choice for the single campaign mode after the download is complete.
			String oldUrn = Campaign.getSingleCampaign(getActivity());

    		ArrayList<ContentValues> allCampaignValues = new ArrayList<ContentValues>();

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
						boolean running = data.getJSONObject(c.mUrn).getString("running_state").equalsIgnoreCase("running");
						
						if (localCampaignUrns.containsKey(c.mUrn)) { //campaign has already been downloaded

							Campaign old = localCampaignUrns.get(c.mUrn);
							localCampaignUrns.remove(c.mUrn);
							
							ContentValues values = new ContentValues();
							// FAISAL: include things here that may change at any time on the server
							values.put(Campaigns.CAMPAIGN_PRIVACY, c.mPrivacy);

							if(!c.mCreationTimestamp.equals(old.mCreationTimestamp))
								values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_OUT_OF_DATE);
							else
								values.put(Campaigns.CAMPAIGN_STATUS, (running) ? Campaign.STATUS_READY : Campaign.STATUS_STOPPED);

							cr.update(Campaigns.CONTENT_URI, values, Campaigns.CAMPAIGN_URN + "= '" + c.mUrn + "'" , null);

						} else { //campaign has not been downloaded
							
							if (running) { //campaign is running
								
//								cr.insert(Campaigns.CONTENT_URI, c.toCV()); //insert remote campaign into content provider
								allCampaignValues.add(c.toCV());
							}
						}
					} catch (JSONException e) {
						Log.e(TAG, "Error parsing json data for " + jsonItems.getString(i), e);
					}
				}
			} catch (JSONException e) {
				Log.e(TAG, "Error parsing response json: 'items' key doesn't exist or is not a JSONArray", e);
			}
			
			ContentValues [] vals = allCampaignValues.toArray(new ContentValues[allCampaignValues.size()]);
			cr.bulkInsert(Campaigns.CONTENT_URI, vals);
			
			//leftover local campaigns were not returned by campaign read, therefore must be in some unavailable state
			for (String urn : localCampaignUrns.keySet()) {
				ContentValues values = new ContentValues();
				values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_NO_EXIST);
				cr.update(Campaigns.CONTENT_URI, values, Campaigns.CAMPAIGN_URN + "= '" + urn + "'" , null);
			}

			// If we are in single campaign mode, we should automatically download the xml for the best campaign
			if(SharedPreferencesHelper.IS_SINGLE_CAMPAIGN) {
				Campaign newCampaign = Campaign.getFirstAvaliableCampaign(getActivity());

				// If there is no good new campaign, the new campaign is different from the old one, or the old one is out of date, we should update it
				if(newCampaign == null || TextUtils.isEmpty(newCampaign.mUrn) || !newCampaign.mUrn.equals(oldUrn) || newCampaign.mStatus == Campaign.STATUS_OUT_OF_DATE) {

					if(!TextUtils.isEmpty(oldUrn)) {
						// If we are removing the old campaign show the notification
						NotificationHelper.showNotification(getActivity(), getActivity().getString(R.string.single_campaign_changed_title), getActivity().getString(R.string.single_campaign_changed_message));
						Campaign.setRemote(getActivity(), oldUrn);
					}

					// Download the new xml
					if(newCampaign != null && !TextUtils.isEmpty(newCampaign.mUrn)) {
						CampaignXmlDownloadTask campaignDownloadTask = new CampaignXmlDownloadTask(getActivity(), newCampaign.mUrn);
						campaignDownloadTask.execute(username, hashedPassword);
						try {
							campaignDownloadTask.get();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					// All campaigns which aren't ready should just be ignored, so they are set to remote
					ContentValues values = new ContentValues();
					values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_REMOTE);
					cr.update(Campaigns.CONTENT_URI, values, Campaigns.CAMPAIGN_STATUS + "!=" + Campaign.STATUS_READY, null);
				}
			}
		} 
		
		return response;
	}
	
	@Override
	protected void onPostExecute(CampaignReadResponse response) {
		super.onPostExecute(response);	
		
		if (response.getResult() == Result.SUCCESS) {
			
		} else if (response.getResult() == Result.FAILURE) {
			Log.e(TAG, "Read failed due to error codes: " + Utilities.stringArrayToString(response.getErrorCodes(), ", "));
			
			boolean isAuthenticationError = false;
			boolean isUserDisabled = false;
			
			for (String code : response.getErrorCodes()) {
				if (code.charAt(1) == '2') {
					isAuthenticationError = true;
					
					if (code.equals("0201")) {
						isUserDisabled = true;
					}
				}
			}
			
			if (isUserDisabled) {
				new SharedPreferencesHelper(mContext).setUserDisabled(true);
			}
			
			if (isAuthenticationError) {
				NotificationHelper.showAuthNotification(mContext);
				Toast.makeText(mContext, R.string.campaign_read_auth_error, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(mContext, R.string.campaign_read_unexpected_response, Toast.LENGTH_SHORT).show();
			}
			
		} else if (response.getResult() == Result.HTTP_ERROR) {
			Log.e(TAG, "http error");
			
			Toast.makeText(mContext, R.string.campaign_read_network_error, Toast.LENGTH_SHORT).show();
		} else {
			Log.e(TAG, "internal error");
			
			Toast.makeText(mContext, R.string.campaign_read_internal_error, Toast.LENGTH_SHORT).show();
		} 
	}
}