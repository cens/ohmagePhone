package org.ohmage.activity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApi.Result;
import org.ohmage.db.DbContract.Campaign;

import com.slezica.tools.async.ManagedAsyncTask;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class CampaignAddActivity extends FragmentActivity {
	
	private static final String TAG = "CampaignAddActivity";
	
	SharedPreferencesHelper mSharedPreferencesHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.campaign_add);
		
		mSharedPreferencesHelper = new SharedPreferencesHelper(this);
		
		new CampaignReadTask(this).execute(mSharedPreferencesHelper.getUsername(), mSharedPreferencesHelper.getHashedPassword());
	}
	
	
	
	public void onCampaignReadTaskDone(CampaignReadResponse mResponse) {		
		
		
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

				// parse response
				try {
					JSONArray jsonItems = response.getMetadata().getJSONArray("items");
					for(int i = 0; i < jsonItems.length(); i++) {
						Campaign c = new Campaign();
						JSONObject data = response.getData();
						c.mUrn = jsonItems.getString(i); 
						c.mName = data.getJSONObject(c.mUrn).getString("name");
						c.mDescription = data.getJSONObject(c.mUrn).getString("description");
						c.mCreationTimestamp = data.getJSONObject(c.mUrn).getString("creation_timestamp");
						c.mDownloadTimestamp = null;
						c.mXml = null;
						c.mStatus = 1;
						c.mIcon = "http://www.lecs.cs.ucla.edu/~mmonibi/android_small_image_med.jpg";
						
						if (data.getJSONObject(c.mUrn).getString("running_state").equalsIgnoreCase("running")) {
							//insert into content provider
							cr.insert(Campaign.CONTENT_URI, c.toCV());
						}
//						} else {
//							for (Campaign localCampaign : mLocalCampaigns) {
//								if (c.mUrn.equals(localCampaign.mUrn)) {
//									removeCampaign(c.mUrn);
//								}
//							}
//						}
						
					}
				} catch (JSONException e) {
					Log.e(TAG, "Error parsing response json", e);
				}
				
//				for (Campaign localCampaign : mLocalCampaigns) {
//					
//					int remoteIndex = -1;
//					
//					for (int i = 0; i < mRemoteCampaigns.size(); i++) {
//						if (mRemoteCampaigns.get(i).mUrn.equals(localCampaign.mUrn)) {
//							remoteIndex = i;
//							break;
//						}
//					}
//					
//					if (remoteIndex != -1) {
//						mRemoteCampaigns.remove(remoteIndex);
//					} else {
//						removeCampaign(localCampaign.mUrn);
//					}
//				}
				

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
			
			((CampaignAddActivity)getActivity()).onCampaignReadTaskDone(response);		
			
			// dismissing dialog from other task!!!
//			if (mActivity.mShowingProgressDialog) {
//				mActivity.dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
//				mActivity.mShowingProgressDialog = false;
//			}			
		}
	}
}
