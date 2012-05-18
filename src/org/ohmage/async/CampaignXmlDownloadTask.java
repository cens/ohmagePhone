package org.ohmage.async;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.systemlog.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.Config;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApi.CampaignXmlResponse;
import org.ohmage.OhmageApi.Response;
import org.ohmage.OhmageApi.Result;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.responsesync.ResponseSyncService;
import org.ohmage.triggers.glue.TriggerFramework;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CampaignXmlDownloadTask extends AuthenticatedTaskLoader<Response> {

	private static final String TAG = "CampaignXmlDownloadTask";

	private final String mCampaignUrn;

	private final Context mContext;
	private OhmageApi mApi;

	private final SharedPreferencesHelper mPrefs;

	public CampaignXmlDownloadTask(Context context, String campaignUrn, String username, String hashedPassword) {
        super(context, username, hashedPassword);
        mCampaignUrn = campaignUrn;
        mContext = context;
		mPrefs = new SharedPreferencesHelper(mContext);
    }

    @Override
    public Response loadInBackground() {
		if(mApi == null)
			mApi = new OhmageApi(mContext);

		int status = Campaign.STATUS_INVALID_USER_ROLE;

		ContentResolver cr = getContext().getContentResolver();

		CampaignReadResponse campaignResponse = mApi.campaignRead(Config.DEFAULT_SERVER_URL, getUsername(), getHashedPassword(), OhmageApi.CLIENT_NAME, "short", mCampaignUrn);

		if(!mPrefs.isAuthenticated()) {
			Log.e(TAG, "User isn't logged in, terminating task");
			return campaignResponse;
		}

		if(campaignResponse.getResult() == Result.SUCCESS) {
			ContentValues values = new ContentValues();
			// Update campaign created timestamp when we download xml
			try {
				JSONObject campaignJson = campaignResponse.getData().getJSONObject(mCampaignUrn);
				values.put(Campaigns.CAMPAIGN_CREATED, campaignJson.getString("creation_timestamp"));
				values.put(Campaigns.CAMPAIGN_UPDATED, startTime);
				cr.update(Campaigns.buildCampaignUri(mCampaignUrn), values, null, null);

				// We iterate through the list of user roles, if participant is included, then the status of this
				// campaign can be set to ready.
				JSONArray roles = campaignJson.getJSONArray("user_roles");
				for(int i=0;i<roles.length();i++) {
					if("participant".equals(roles.getString(i))) {
						status = Campaign.STATUS_READY;
						break;
					}
				}
			} catch (JSONException e) {
				Log.e(TAG, "Error parsing json data for " + mCampaignUrn, e);
			}
		} else {
			return campaignResponse;
		}

		CampaignXmlResponse response =  mApi.campaignXmlRead(Config.DEFAULT_SERVER_URL, getUsername(), getHashedPassword(), OhmageApi.CLIENT_NAME, mCampaignUrn);

		if(!mPrefs.isAuthenticated()) {
			Log.e(TAG, "User isn't logged in, terminating task");
			return response;
		}

		if (response.getResult() == Result.SUCCESS) {
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String downloadTimestamp = dateFormat.format(new Date());
		
			ContentValues values = new ContentValues();
			values.put(Campaigns.CAMPAIGN_URN, mCampaignUrn);
			values.put(Campaigns.CAMPAIGN_DOWNLOADED, downloadTimestamp);
			values.put(Campaigns.CAMPAIGN_CONFIGURATION_XML, response.getXml());
			values.put(Campaigns.CAMPAIGN_STATUS, status);
			values.put(Campaigns.CAMPAIGN_UPDATED, startTime);
			int count = cr.update(Campaigns.buildCampaignUri(mCampaignUrn), values, null, null);
			if (count < 1) {
				//nothing was updated
			} else if (count > 1) {
				//too many things were updated
			} else {
				//update occurred successfully
			}
			
			if (Config.ALLOWS_FEEDBACK) {
				// create an intent to fire off the feedback service
				Intent fbIntent = new Intent(getContext(), ResponseSyncService.class);
				// annotate the request with the current campaign's URN
				fbIntent.putExtra(ResponseSyncService.EXTRA_CAMPAIGN_URN, mCampaignUrn);
				fbIntent.putExtra(ResponseSyncService.EXTRA_FORCE_ALL, true);
				// and go!
				WakefulIntentService.sendWakefulWork(getContext(), fbIntent);
			}
		} else { 
			ContentValues values = new ContentValues();
			values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_REMOTE);
			values.put(Campaigns.CAMPAIGN_UPDATED, startTime);
			cr.update(Campaigns.buildCampaignUri(mCampaignUrn), values, null, null);
		}
		
		return response;
    }

    @Override
    public void deliverResult(Response response) {

		if(!mPrefs.isAuthenticated()) {
			Log.e(TAG, "User isn't logged in, terminating task");
			return;
		}

		if(response.getResult() != Result.SUCCESS) {
			// revert the db back to remote
			ContentResolver cr = getContext().getContentResolver();
			ContentValues values = new ContentValues();
			values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_REMOTE);
			values.put(Campaigns.CAMPAIGN_UPDATED, startTime);
			cr.update(Campaigns.buildCampaignUri(mCampaignUrn), values, null, null);
		}

		if (response.getResult() == Result.SUCCESS) {
			// setup initial triggers for this campaign
			TriggerFramework.setDefaultTriggers(getContext(), mCampaignUrn);
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
				new SharedPreferencesHelper(getContext()).setUserDisabled(true);
			}
			
			if (isAuthenticationError) {
				NotificationHelper.showAuthNotification(getContext());
				Toast.makeText(getContext(), R.string.campaign_xml_auth_error, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getContext(), R.string.campaign_xml_unexpected_response, Toast.LENGTH_SHORT).show();
			}
			
		} else if (response.getResult() == Result.HTTP_ERROR) {
			Log.e(TAG, "Read failed due to http error");
			Toast.makeText(getContext(), R.string.campaign_xml_network_error, Toast.LENGTH_SHORT).show();
		} else {
			Log.e(TAG, "Read failed due to internal error");
			Toast.makeText(getContext(), R.string.campaign_xml_internal_error, Toast.LENGTH_SHORT).show();
		} 

        super.deliverResult(response);
    }

    @Override
    protected void onForceLoad() {
		super.onForceLoad();

		if(!mPrefs.isAuthenticated()) {
			Log.e(TAG, "User isn't logged in, terminating task");
			return;
		}

		ContentResolver cr = getContext().getContentResolver();
		ContentValues values = new ContentValues();
		values.put(Campaigns.CAMPAIGN_STATUS, Campaign.STATUS_DOWNLOADING);
		values.put(Campaigns.CAMPAIGN_UPDATED, startTime);
		cr.update(Campaigns.buildCampaignUri(mCampaignUrn), values, null, null);
    }
}