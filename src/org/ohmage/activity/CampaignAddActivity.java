package org.ohmage.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApi.CampaignXmlResponse;
import org.ohmage.OhmageApi.Result;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.activity.CampaignListFragment.OnCampaignClickListener;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.feedback.FeedbackService;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.slezica.tools.async.ManagedAsyncTask;

public class CampaignAddActivity extends FragmentActivity implements OnCampaignClickListener{
	
	private static final String TAG = "CampaignAddActivity";
	
	SharedPreferencesHelper mSharedPreferencesHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.campaign_add);
		
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		
		ContentResolver cr = getContentResolver();
		
		//delete all remote campaigns from content provider
		cr.delete(Campaign.CONTENT_URI, Campaign.STATUS + "=" + Campaign.STATUS_REMOTE, null);
		
		new CampaignReadTask(this).execute(mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword());
	}
	
	@Override
	public void onCampaignItemClick(String campaignUrn) {
		Toast.makeText(this, "Launching Campaign Info Activity", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onCampaignActionClick(String campaignUrn) {
		new CampaignXmlDownloadTask(this).execute(mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword(), campaignUrn);
	}
	
	public void onCampaignReadTaskDone() {		
		
	}
	
	private void onCampaignXmlDownloadTaskDone() {
		
	}

	private static class CampaignReadTask extends ManagedAsyncTask<String, Void, CampaignReadResponse>{
		
		private Context mContext;

		public CampaignReadTask(FragmentActivity activity) {
			super(activity);
			mContext = activity.getApplicationContext();
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			Toast.makeText(mContext, "Starting " + this.getClass().getName(), Toast.LENGTH_SHORT).show();
			
//			mActivity.mFooter.setVisibility(View.VISIBLE);
//			mActivity.mFooter.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
//			mActivity.mFooter.findViewById(R.id.error_text).setVisibility(View.GONE);
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
//				cr.delete(Campaign.CONTENT_URI, Campaign.STATUS + "=" + Campaign.STATUS_REMOTE, null);
				
				//build list of urns of all downloaded (local) campaigns
				Cursor cursor = cr.query(Campaign.CONTENT_URI, new String [] {Campaign._ID, Campaign.URN}, Campaign.STATUS + "!=" + Campaign.STATUS_REMOTE, null, null);
				cursor.moveToFirst();
				
				ArrayList<String> localCampaignUrns = new ArrayList<String>();
				
	    		for (int i = 0; i < cursor.getCount(); i++) {
	    			
	    			String urn = cursor.getString(cursor.getColumnIndex(Campaign.URN));
	    			localCampaignUrns.add(urn);
	    			
	    			cursor.moveToNext();
	    		}
	    		
	    		cursor.close();

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
							c.mIcon = "http://www.lecs.cs.ucla.edu/~mmonibi/android_small_image_med.jpg";
							boolean running = data.getJSONObject(c.mUrn).getString("running_state").equalsIgnoreCase("running");
							
							if (localCampaignUrns.remove(c.mUrn)) { //campaign has already been downloaded
								
								if (running) { //campaign is running
										
									ContentValues values = new ContentValues();
									values.put(Campaign.STATUS, Campaign.STATUS_READY);
									cr.update(Campaign.CONTENT_URI, values, Campaign.URN + "= '" + c.mUrn + "'" , null);
									
								} else { //campaign is stopped
									
									ContentValues values = new ContentValues();
									values.put(Campaign.STATUS, Campaign.STATUS_STOPPED);
									cr.update(Campaign.CONTENT_URI, values, Campaign.URN + "= '" + c.mUrn + "'" , null);
								}
								
							} else { //campaign has not been downloaded
								
								if (running) { //campaign is running
									
									cr.insert(Campaign.CONTENT_URI, c.toCV()); //insert remote campaign into content provider
								}
							}
						} catch (JSONException e) {
							Log.e(TAG, "Error parsing json data for " + jsonItems.getString(i), e);
						}
					}
				} catch (JSONException e) {
					Log.e(TAG, "Error parsing response json: 'items' key doesn't exist or is not a JSONArray", e);
				}
				
				//leftover local campaigns were not returned by campaign read, therefore must be in some unavailable state
				for (String urn : localCampaignUrns) { 
					ContentValues values = new ContentValues();
					values.put(Campaign.STATUS, Campaign.STATUS_VAGUE);
					cr.update(Campaign.CONTENT_URI, values, Campaign.URN + "= '" + urn + "'" , null);
				}				

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
//					new SharedPreferencesHelper(this).setUserDisabled(true);
//					mFooter.setVisibility(View.VISIBLE);
//					mFooter.findViewById(R.id.progress_bar).setVisibility(View.GONE);
//					mFooter.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
//					((TextView)mFooter.findViewById(R.id.error_text)).setText("This user account has been disabled.");
				} else if (isAuthenticationError) {
//					mFooter.setVisibility(View.VISIBLE);
//					mFooter.findViewById(R.id.progress_bar).setVisibility(View.GONE);
//					mFooter.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
//					((TextView)mFooter.findViewById(R.id.error_text)).setText("Unable to authenticate. Please check username and update the password.");
				} else {
//					mFooter.setVisibility(View.VISIBLE);
//					mFooter.findViewById(R.id.progress_bar).setVisibility(View.GONE);
//					mFooter.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
//					((TextView)mFooter.findViewById(R.id.error_text)).setText("Internal error.");
				}
				
			} else if (response.getResult() == Result.HTTP_ERROR) {
				Log.e(TAG, "http error");
				
//				mFooter.setVisibility(View.VISIBLE);
//				mFooter.findViewById(R.id.progress_bar).setVisibility(View.GONE);
//				mFooter.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
//				((TextView)mFooter.findViewById(R.id.error_text)).setText("Unable to communicate with server at this time.");
			} else {
				Log.e(TAG, "internal error");
				
//				mFooter.setVisibility(View.VISIBLE);
//				mFooter.findViewById(R.id.progress_bar).setVisibility(View.GONE);
//				mFooter.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
//				((TextView)mFooter.findViewById(R.id.error_text)).setText("Internal server communication error.");
			} 
			
			return response;
		}
		
		@Override
		protected void onPostExecute(CampaignReadResponse response) {
			super.onPostExecute(response);
			
			Toast.makeText(mContext, "Finished " + this.getClass().getName(), Toast.LENGTH_SHORT).show();
			
			((CampaignAddActivity)getActivity()).onCampaignReadTaskDone();		
			
			// dismissing dialog from other task!!!
//			if (mActivity.mShowingProgressDialog) {
//				mActivity.dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
//				mActivity.mShowingProgressDialog = false;
//			}			
		}
	}
	
	private static class CampaignXmlDownloadTask extends ManagedAsyncTask<String, Void, CampaignXmlResponse>{
		
		private Context mContext;
		
		public CampaignXmlDownloadTask(FragmentActivity activity) {
			super(activity);
			mContext = activity.getApplicationContext();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			Toast.makeText(mContext, "Starting " + this.getClass().getName(), Toast.LENGTH_SHORT).show();
//			mActivity.showDialog(DIALOG_DOWNLOAD_PROGRESS);
		}

		@Override
		protected CampaignXmlResponse doInBackground(String... params) {
			String username = (String) params[0];
			String hashedPassword = (String) params[1];
			String campaignUrn = (String) params[2];
			OhmageApi api = new OhmageApi(mContext);
			CampaignXmlResponse response =  api.campaignXmlRead(SharedPreferencesHelper.DEFAULT_SERVER_URL, username, hashedPassword, "android", campaignUrn);
			
			if (response.getResult() == Result.SUCCESS) {
				
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String downloadTimestamp = dateFormat.format(new Date());
			
				ContentResolver cr = mContext.getContentResolver();
				ContentValues values = new ContentValues();
				values.put(Campaign.DOWNLOAD_TIMESTAMP, downloadTimestamp);
				values.put(Campaign.CONFIGURATION_XML, response.getXml());
				values.put(Campaign.STATUS, Campaign.STATUS_READY);
				int count = cr.update(Campaign.CONTENT_URI, values, Campaign.URN + "= '" + campaignUrn + "'", null); 
				if (count < 1) {
					//nothing was updated
				} else if (count > 1) {
					//too many things were updated
				} else {
					//update occurred successfully
				}
				
				if (SharedPreferencesHelper.ALLOWS_FEEDBACK) {
					// create an intent to fire off the feedback service
					Intent fbIntent = new Intent(mContext, FeedbackService.class);
					// annotate the request with the current campaign's URN
					fbIntent.putExtra("campaign_urn", campaignUrn);
					// and go!
					WakefulIntentService.sendWakefulWork(mContext, fbIntent);
				}
			} else { 
				
//				try {
//					dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
//				} catch (IllegalArgumentException e) {
//					Log.e(TAG, "Attempting to dismiss dialog that had not been shown.", e);
//				}
				
				if (response.getResult() == Result.FAILURE) {
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
//						new SharedPreferencesHelper(this).setUserDisabled(true);
//						showDialog(DIALOG_USER_DISABLED);
					} else if (isAuthenticationError) {
//						showDialog(DIALOG_AUTH_ERROR);
					} else {
//						showDialog(DIALOG_INTERNAL_ERROR);
					}
					
				} else if (response.getResult() == Result.HTTP_ERROR) {
					Log.e(TAG, "http error");
					
//					showDialog(DIALOG_NETWORK_ERROR);
				} else {
					Log.e(TAG, "internal error");
					
//					showDialog(DIALOG_INTERNAL_ERROR);
				}
			}
			
			return response;
		}
		
		@Override
		protected void onPostExecute(CampaignXmlResponse response) {
			super.onPostExecute(response);
			
			Toast.makeText(mContext, "Finished " + this.getClass().getName(), Toast.LENGTH_SHORT).show();
			
			((CampaignAddActivity)getActivity()).onCampaignXmlDownloadTaskDone();
		}
	}
}
