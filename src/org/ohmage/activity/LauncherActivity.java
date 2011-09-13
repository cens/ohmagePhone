package org.ohmage.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.Response;
import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApi.CampaignXmlResponse;
import org.ohmage.OhmageApi.Result;
import org.ohmage.db.DbHelper;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.feedback.FeedbackService;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class LauncherActivity extends Activity {
	
	private static final String TAG = "LauncherActivity";
	
	private ProgressBar mProgressBar;
	private TextView mMessageText;
	private Button mRetryButton;
	private Button mLogoutButton;
	
	private CampaignLoadTask mTask;
	private SharedPreferencesHelper preferencesHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		preferencesHelper = new SharedPreferencesHelper(this);
		
		if (preferencesHelper.isUserDisabled()) {
        	((OhmageApplication) getApplication()).resetAll();
        }
		
		if (!preferencesHelper.isAuthenticated()) {
			Log.i(TAG, "no credentials saved, so launch Login");
			startActivity(new Intent(this, LoginActivity.class));
			finish();
		} else {
			if (SharedPreferencesHelper.IS_SINGLE_CAMPAIGN) {
		
				setContentView(R.layout.launcher_activity);
			
				mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
				mMessageText = (TextView) findViewById(R.id.message_text);
				mRetryButton = (Button) findViewById(R.id.retry_button);
				mLogoutButton = (Button) findViewById(R.id.logout_button);
				
				mRetryButton.setOnClickListener(mClickListener);
				mLogoutButton.setOnClickListener(mClickListener);
				
				mProgressBar.setVisibility(View.VISIBLE);
				mMessageText.setVisibility(View.VISIBLE);
				mRetryButton.setVisibility(View.GONE);
				mLogoutButton.setVisibility(View.GONE);
				
				mMessageText.setText("Downloading campaign configuration...");
			
		        
	        	DbHelper dbHelper = new DbHelper(this);
				List<Campaign> campaigns = dbHelper.getCampaigns();
				if (campaigns.size() < 1) {
					Log.e(TAG, "A default campaign has not been loaded.");
					Log.i(TAG, "Re-attempting to load default campaign.");
					mTask = new CampaignLoadTask(this);
					mTask.execute(preferencesHelper.getUsername(), preferencesHelper.getHashedPassword());
				} else {
					if (campaigns.size() > 1) {
						Log.w(TAG, "There should only be one campaign but there are " + campaigns.size() + ". Launching first campaign in db.");
						Toast.makeText(this, "There should only be one campaign but there are " + campaigns.size() + ". Launching first campaign in database.", Toast.LENGTH_LONG).show();
					}
			        Campaign defaultCampaign = campaigns.get(0);
					Intent intent = new Intent(this, SurveyListActivity.class);
					intent.putExtra("campaign_urn", defaultCampaign.mUrn);
					intent.putExtra("campaign_name", defaultCampaign.mName);
					startActivity(intent);
					finish();
				}
			} else {
				Intent intent = new Intent(this, DashboardActivity.class);
				startActivity(intent);
				finish();
			}
		}
	}
	
	private OnClickListener mClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.retry_button:
				mProgressBar.setVisibility(View.VISIBLE);
				mMessageText.setVisibility(View.VISIBLE);
				mRetryButton.setVisibility(View.GONE);
				mLogoutButton.setVisibility(View.GONE);
				
				mMessageText.setText("Downloading campaign configuration...");
				
				mTask = new CampaignLoadTask(LauncherActivity.this);
				mTask.execute(preferencesHelper.getUsername(), preferencesHelper.getHashedPassword());
				break;

			case R.id.logout_button:
				((OhmageApplication)getApplication()).resetAll();
				startActivity(new Intent(LauncherActivity.this, LoginActivity.class));
				finish();
				break;
			}
		}
	};
	
	private void showErrorMessage(String message) {
		mProgressBar.setVisibility(View.GONE);
		mMessageText.setVisibility(View.VISIBLE);
		mRetryButton.setVisibility(View.VISIBLE);
		mLogoutButton.setVisibility(View.VISIBLE);
		
		mMessageText.setText(message);
	}
	
	private void onCampaignLoadDone(CampaignLoadResult result) {
		
		mTask = null;
		
		switch (result) {
		case SUCCESS:
			DbHelper dbHelper = new DbHelper(this);
			List<Campaign> campaigns = dbHelper.getCampaigns();
			if (campaigns.size() < 1) {
				Log.e(TAG, "Campaign was just added to db but cannot be found!");
			} else {
				if (campaigns.size() > 1) {
					Log.w(TAG, "There should only be one campaign but there are " + campaigns.size() + ". Launching first campaign in db.");
//					Toast.makeText(this, "There should only be one campaign but there are " + campaigns.size() + ". Launching first campaign in database.", Toast.LENGTH_LONG).show();
				}
				Campaign defaultCampaign = campaigns.get(0);
				Intent intent = new Intent(this, SurveyListActivity.class);
				intent.putExtra("campaign_urn", defaultCampaign.mUrn);
				intent.putExtra("campaign_name", defaultCampaign.mName);
				startActivity(intent);
				finish();
			}
			break;
			
		case NO_CAMPAIGNS:
			showErrorMessage("There are no active campaigns assigned to this user.");
			break;
		case JSON_ERROR:
			showErrorMessage("An error occured while parsing the json data retrieved from the server.");
			break;
		case HTTP_ERROR:
			showErrorMessage("Unable to communicate with the server at this time.");
			break;
		case USER_DISABLED:
			showErrorMessage("This user account has been disabled. Please exit and relaunch the app.");
			break;
		case AUTH_ERROR:
			showErrorMessage("Unable to authenticate. Please check username and update the password.");
			break;
		case INTERNAL_ERROR:
			showErrorMessage("Internal server communication error.");
			break;
		default:
			showErrorMessage("Unknown error.");
			break;
		}
	}
	
	private enum CampaignLoadResult {
		SUCCESS,
		NO_CAMPAIGNS,
		JSON_ERROR,
		HTTP_ERROR,
		USER_DISABLED,
		AUTH_ERROR,
		INTERNAL_ERROR
	}
	
	private static class CampaignLoadTask extends AsyncTask<String, Void, CampaignLoadResult>{
		
		private LauncherActivity mActivity;
		private boolean mIsDone = false;
		private CampaignLoadResult mResult = null;

		private CampaignLoadTask(LauncherActivity activity) {
			this.mActivity = activity;
		}
		
		public void setActivity(LauncherActivity activity) {
			this.mActivity = activity;
			if (mIsDone) {
				notifyTaskDone();
			}
		}
		
		@Override
		protected CampaignLoadResult doInBackground(String... params) {
			String username = params[0];
			String hashedPassword = params[1];
			OhmageApi api = new OhmageApi(mActivity);
			CampaignReadResponse response = api.campaignRead(SharedPreferencesHelper.DEFAULT_SERVER_URL, username, hashedPassword, "android", "short", null);
			
			if (response.getResult() == Result.SUCCESS) {
				
				ArrayList<Campaign> campaigns = new ArrayList<Campaign>();
				
				try {
					JSONArray jsonItems = response.getMetadata().getJSONArray("items");
					for(int i = 0; i < jsonItems.length(); i++) {
						Campaign c = new Campaign();
						JSONObject data = response.getData();
						c.mUrn = jsonItems.getString(i); 
						c.mName = data.getJSONObject(c.mUrn).getString("name");
						c.mCreationTimestamp = data.getJSONObject(c.mUrn).getString("creation_timestamp");
						
						if (data.getJSONObject(c.mUrn).getString("running_state").equalsIgnoreCase("running")) {
							campaigns.add(c);
						}
					}
				} catch (JSONException e) {
					Log.e(TAG, "Error parsing response json", e);
					return CampaignLoadResult.JSON_ERROR;
				}
				
				if (campaigns.size() < 1) {
					Log.e(TAG, "There are no campaigns assigned to this user.");
//					Toast.makeText(LauncherActivity.this, "There are no campaigns assigned to this user.", Toast.LENGTH_LONG).show();
					return CampaignLoadResult.NO_CAMPAIGNS;
				} else {
					if (campaigns.size() > 1) {
						Log.w(TAG, "There should only be one campaign but there are " + campaigns.size() + " assigned to this user. Loading just the first campaign.");
//						Toast.makeText(this, "There should only be one campaign but there are " + campaigns.size() + " assigned to this user. Loading just the first campaign.", Toast.LENGTH_LONG).show();
					}
					
					Campaign defaultCampaign = campaigns.get(0);
					CampaignXmlResponse xmlResponse = api.campaignXmlRead(SharedPreferencesHelper.DEFAULT_SERVER_URL, username, hashedPassword, "android", defaultCampaign.mUrn);
					
					if (xmlResponse.getResult() == Result.SUCCESS) {
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String downloadTimestamp = dateFormat.format(new Date());
						//String downloadTimestamp = DateFormat.format("yyyy-MM-dd kk:mm:ss", System.currentTimeMillis());
						
						DbHelper dbHelper = new DbHelper(mActivity);
						if (dbHelper.getCampaign(defaultCampaign.mUrn) == null) {
							dbHelper.addCampaign(defaultCampaign.mUrn, defaultCampaign.mName, defaultCampaign.mDescription, defaultCampaign.mCreationTimestamp, downloadTimestamp, xmlResponse.getXml());
						} else {
							Log.w(TAG, "Campaign already exists. This should never happen. Replacing previous entry with new one.");
							dbHelper.removeCampaign(defaultCampaign.mUrn);
							dbHelper.addCampaign(defaultCampaign.mUrn, defaultCampaign.mName, defaultCampaign.mDescription, defaultCampaign.mCreationTimestamp, downloadTimestamp, xmlResponse.getXml());
						}
						
						if (SharedPreferencesHelper.ALLOWS_FEEDBACK) {
							// create an intent to fire off the feedback service
							Intent fbIntent = new Intent(mActivity, FeedbackService.class);
							// annotate the request with the current campaign's URN
							fbIntent.putExtra("campaign_urn", defaultCampaign.mUrn);
							// and go!
							WakefulIntentService.sendWakefulWork(mActivity, fbIntent);
						}
						
						return CampaignLoadResult.SUCCESS;
					} else if (xmlResponse.getResult() == Result.FAILURE) {
						return handleErrors(xmlResponse);
					} else if (xmlResponse.getResult() == Result.HTTP_ERROR) {
						return CampaignLoadResult.HTTP_ERROR;
					} else {
						return CampaignLoadResult.INTERNAL_ERROR;
					}
				}
				
			} else if (response.getResult() == Result.FAILURE) {
				return handleErrors(response);
			} else if (response.getResult() == Result.HTTP_ERROR) {
				return CampaignLoadResult.HTTP_ERROR;
			} else {
				return CampaignLoadResult.INTERNAL_ERROR;
			}
		}
		
		@Override
		protected void onPostExecute(CampaignLoadResult result) {
			super.onPostExecute(result);
			
			mResult = result;
			mIsDone = true;
			notifyTaskDone();
		}
		
		private void notifyTaskDone() {
			if (mActivity != null) {
				mActivity.onCampaignLoadDone(mResult);
			}
		}
		
		private CampaignLoadResult handleErrors(Response response) {
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
				new SharedPreferencesHelper(mActivity).setUserDisabled(true);
				return CampaignLoadResult.USER_DISABLED;
			} else if (isAuthenticationError) {
				return CampaignLoadResult.AUTH_ERROR;
			} else {
				return CampaignLoadResult.INTERNAL_ERROR;
			}
		}
	}

}
