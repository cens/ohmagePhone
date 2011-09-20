package org.ohmage.activity;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApi.Result;
import org.ohmage.db.DbContract.Campaign;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.slezica.tools.async.ManagedAsyncTask;

class CampaignReadTask extends ManagedAsyncTask<String, Void, CampaignReadResponse>{
		
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
				cr.delete(Campaign.CONTENT_URI, Campaign.STATUS + "=" + Campaign.STATUS_REMOTE, null);
				
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
							c.mPrivacy = data.optString("privacy_state", "unknown");
							c.mIcon = "http://www.lecs.cs.ucla.edu/~mmonibi/android_small_image_med.jpg";
							boolean running = data.getJSONObject(c.mUrn).getString("running_state").equalsIgnoreCase("running");
							
							if (localCampaignUrns.remove(c.mUrn)) { //campaign has already been downloaded
								
								ContentValues values = new ContentValues();
								// FAISAL: include things here that may change at any time on the server
								values.put(Campaign.PRIVACY, c.mPrivacy);
								
								if (running) { //campaign is running
									
									values.put(Campaign.STATUS, Campaign.STATUS_READY);
									cr.update(Campaign.CONTENT_URI, values, Campaign.URN + "= '" + c.mUrn + "'" , null);
									
								} else { //campaign is stopped
									
									values.put(Campaign.STATUS, Campaign.STATUS_STOPPED);
									cr.update(Campaign.CONTENT_URI, values, Campaign.URN + "= '" + c.mUrn + "'" , null);
								}
								
							} else { //campaign has not been downloaded
								
								if (running) { //campaign is running
									
									cr.insert(Campaign.CONTENT_URI, c.toCV()); //insert remote campaign into content provider
								}
							}
						} catch (JSONException e) {
							Log.e(CampaignAddActivity.TAG, "Error parsing json data for " + jsonItems.getString(i), e);
						}
					}
				} catch (JSONException e) {
					Log.e(CampaignAddActivity.TAG, "Error parsing response json: 'items' key doesn't exist or is not a JSONArray", e);
				}
				
				//leftover local campaigns were not returned by campaign read, therefore must be in some unavailable state
				for (String urn : localCampaignUrns) { 
					ContentValues values = new ContentValues();
					values.put(Campaign.STATUS, Campaign.STATUS_VAGUE);
					cr.update(Campaign.CONTENT_URI, values, Campaign.URN + "= '" + urn + "'" , null);
				}				

			} else if (response.getResult() == Result.FAILURE) {
				Log.e(CampaignAddActivity.TAG, "Read failed due to error codes: " + Utilities.stringArrayToString(response.getErrorCodes(), ", "));
				
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
				Log.e(CampaignAddActivity.TAG, "http error");
				
//				mFooter.setVisibility(View.VISIBLE);
//				mFooter.findViewById(R.id.progress_bar).setVisibility(View.GONE);
//				mFooter.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
//				((TextView)mFooter.findViewById(R.id.error_text)).setText("Unable to communicate with server at this time.");
			} else {
				Log.e(CampaignAddActivity.TAG, "internal error");
				
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
			
			// dismissing dialog from other task!!!
//			if (mActivity.mShowingProgressDialog) {
//				mActivity.dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
//				mActivity.mShowingProgressDialog = false;
//			}			
		}
	}