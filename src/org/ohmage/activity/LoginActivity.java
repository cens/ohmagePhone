/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.slezica.tools.async.ManagedAsyncTask;

import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Analytics.Status;
import edu.ucla.cens.systemlog.Log;

import org.ohmage.BackgroundManager;
import org.ohmage.ConfigHelper;
import org.ohmage.MobilityHelper;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.async.CampaignReadTask;
import org.ohmage.db.Models.Campaign;

public class LoginActivity extends FragmentActivity {
	
	public static final String TAG = "LoginActivity";
	
	/**
	 * The {@link LoginActivity} looks for this extra to determine if
	 * it should update the credentials for the user rather than just passing through
	 */
	public static final String EXTRA_UPDATE_CREDENTIALS = "extra_update_credentials";
	
    private static final int DIALOG_FIRST_RUN = 1;
    private static final int DIALOG_LOGIN_ERROR = 2;
    private static final int DIALOG_NETWORK_ERROR = 3;
    private static final int DIALOG_LOGIN_PROGRESS = 4;
    private static final int DIALOG_INTERNAL_ERROR = 5;
    private static final int DIALOG_USER_DISABLED = 6;
	private static final int DIALOG_DOWNLOADING_CAMPAIGNS = 7;
	private static final int DIALOG_SERVER_LIST = 8;

	private static final int LOGIN_FINISHED = 0;
	
	private EditText mUsernameEdit;
	private EditText mPasswordEdit;
	private EditText mServerEdit;
	private TextView mRegisterAccountLink;
	private Button mLoginButton;
	private TextView mVersionText;
	private UserPreferencesHelper mPreferencesHelper;

	private boolean mUpdateCredentials;

	private ConfigHelper mAppPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.login);

		// first see if they are already logged in
        mPreferencesHelper = new UserPreferencesHelper(this);
		mAppPrefs = new ConfigHelper(LoginActivity.this);
		
		if (mPreferencesHelper.isUserDisabled()) {
        	((OhmageApplication) getApplication()).resetAll();
        }
		
		mUpdateCredentials = getIntent().getBooleanExtra(EXTRA_UPDATE_CREDENTIALS, false);
		
		// if they are, redirect them to the dashboard
		if (mPreferencesHelper.isAuthenticated() && !mUpdateCredentials) {
			startActivityForResult(new Intent(this, DashboardActivity.class), LOGIN_FINISHED);
			return;
		}
		
		mLoginButton = (Button) findViewById(R.id.login);
        mUsernameEdit = (EditText) findViewById(R.id.login_username); 
        mPasswordEdit = (EditText) findViewById(R.id.login_password);
		mServerEdit = (EditText) findViewById(R.id.login_server_edit);
		mRegisterAccountLink = (TextView) findViewById(R.id.login_register_new_account);

        mVersionText = (TextView) findViewById(R.id.version);
        
        try {
			mVersionText.setText("v" + getPackageManager().getPackageInfo("org.ohmage", 0).versionName);
		} catch (Exception e) {
			Log.e(TAG, "unable to retrieve version", e);
			mVersionText.setText(" ");
		}
        
        mLoginButton.setOnClickListener(mClickListener);
        mRegisterAccountLink.setOnClickListener(mClickListener);
        

		if(getResources().getBoolean(R.bool.allow_custom_server)) {
			View serverContainer = findViewById(R.id.login_server_container);
			serverContainer.setVisibility(View.VISIBLE);
		}

		String defaultServer = ConfigHelper.serverUrl();
		if(TextUtils.isEmpty(defaultServer))
			defaultServer = getResources().getStringArray(R.array.servers)[0];
		mServerEdit.setText(defaultServer);
		ensureServerUrl();
		mServerEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus) {
					ensureServerUrl();
				}
			}
		});

		ImageButton addServer = (ImageButton) findViewById(R.id.login_add_server);
		addServer.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ensureServerUrl();
				showDialog(DIALOG_SERVER_LIST);
			}
		});

		if (savedInstanceState == null) {
        	Log.i(TAG, "creating from scratch");
        	
        	//clear login fail notification (if such notification existed) 
        	//NotificationHelper.cancel(this, NotificationHelper.NOTIFY_LOGIN_FAIL, null);
        	//move this down so notification is only cleared if login is successful???
        	
        	//the following block is commented out to support single user lock-in, implemented in the code below
        	/*String username = mPreferencesHelper.getUsername();
            if (username.length() > 0) {
            	Log.i(TAG, "saved credentials exist");
    			mUsernameEdit.setText(username);
            	mPasswordEdit.setText(DUMMY_PASSWORD);
            	//launch main activity and finish
            	//WE CAN'T DO THIS BECAUSE IF THE ACTIVITY IS LAUNCHED AFTER AN HTTP ERROR NOTIFICATION, CREDS WILL STILL BE STORED
            	//startActivity(new Intent(LoginActivity.this, MainActivity.class));
            	//finish();
            	//OR do Login?
            	doLogin();
            	
            	
            	//what if this works the other way around? make main activity the starting point, and launch login if needed? 
            } else {
            	Log.i(TAG, "saved credentials do not exist");
            }*/
            
            if (mPreferencesHelper.isUserDisabled()) {
            	((OhmageApplication) getApplication()).resetAll();
            }
        	
        	//code for single user lock-in
        	String username = mPreferencesHelper.getUsername();
        	if (username.length() > 0) {
        		Log.i(TAG, "saved username exists");
    			mUsernameEdit.setText(username);
    			mUsernameEdit.setEnabled(false);
    			mUsernameEdit.setFocusable(false);
    			
//    			String hashedPassword = mPreferencesHelper.getHashedPassword();
//    			if (hashedPassword.length() > 0) {
//    				Log.i(TAG, "saved password exists");
//    				mPasswordEdit.setText(DUMMY_PASSWORD);
//    				//doLogin();
//    			} else {
//    				Log.i(TAG, "no saved password, must have had a login problem");
//    			}
    			
        	} else {
        		Log.i(TAG, "no saved username");
        	}
        }

        mCampaignDownloadTask = (CampaignReadTask) getSupportLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<CampaignReadResponse>() {

			@Override
			public Loader<CampaignReadResponse> onCreateLoader(int id, Bundle args) {
				return new CampaignReadTask(LoginActivity.this, null, null);
			}

			@Override
			public void onLoadFinished(Loader<CampaignReadResponse> loader,
					CampaignReadResponse data) {
				String urn = Campaign.getSingleCampaign(LoginActivity.this);
				if(urn == null) {
					// Downloading the campaign failed so we should reset the username and password
					MobilityHelper.setUsername(LoginActivity.this, null);
					mPreferencesHelper.clearCredentials();
					Toast.makeText(LoginActivity.this, R.string.login_error_downloading_campaign, Toast.LENGTH_LONG).show();
				} else {
					loginFinished(((CampaignReadTask) loader).getUsername(), ((CampaignReadTask) loader).getHashedPassword());
				}
				dismissDialog(DIALOG_DOWNLOADING_CAMPAIGNS);
			}

			@Override
			public void onLoaderReset(Loader<CampaignReadResponse> loader) {
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		getSupportLoaderManager().destroyLoader(0);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Analytics.activity(this, Status.OFF);
	}

	@Override
	public void onResume() {
		super.onResume();
		Analytics.activity(this, Status.ON);

		// Hide any notifications since we started the login activity
		NotificationHelper.hideAuthNotification(this);
	}

	private final OnClickListener mClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.login:
					Log.i(TAG, "login button clicked");
					Analytics.widget(v);
					if(ensureServerUrl()) {
						ConfigHelper.setServerUrl(mServerEdit.getText().toString());
						doLogin();
					} else
						Toast.makeText(LoginActivity.this, R.string.login_invalid_server, Toast.LENGTH_SHORT).show();
					break;
					
				case R.id.login_register_new_account:
					// reads the currently selected server and fires a browser intent
					// which takes the user to the registration page for that server
					Log.i(TAG, "register new account linkbutton clicked");
					
					Analytics.widget(v);
					if (ensureServerUrl()) {
						// use the textbox to make a url and send the user on their way
						String url = mServerEdit.getText().toString() + "#register";
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
						startActivity(i);
					} else
						Toast.makeText(LoginActivity.this, R.string.login_invalid_server, Toast.LENGTH_SHORT).show();
					
					break;
			}
		}
	};

	private CampaignReadTask mCampaignDownloadTask;

	private void doLogin() {
		String username = mUsernameEdit.getText().toString();
		String password = mPasswordEdit.getText().toString();
		
		new LoginTask(LoginActivity.this).execute(username, password);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		switch (id) {
			case DIALOG_FIRST_RUN:
				dialogBuilder.setTitle(R.string.eula_title)
				.setMessage(R.string.eula_text)
				.setCancelable(false)
				.setPositiveButton(R.string.eula_accept, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mAppPrefs.setFirstRun(false);
					}
				})
				.setNegativeButton(R.string.eula_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						LoginActivity.this.finish();
					}
				});
				dialog = dialogBuilder.create();
				break;

			case DIALOG_LOGIN_ERROR:
				dialogBuilder.setTitle(R.string.login_error)
				.setMessage(R.string.login_invalid_password)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, null)
				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on login error
							}
						})*/;
				//add button for contact
				dialog = dialogBuilder.create();        	
				break;

			case DIALOG_USER_DISABLED:
				dialogBuilder.setTitle(R.string.login_error)
				.setMessage(R.string.login_account_disabled)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, null)
				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on login error
							}
						})*/;
				//add button for contact
				dialog = dialogBuilder.create();        	
				break;

			case DIALOG_NETWORK_ERROR:
				dialogBuilder.setTitle(R.string.login_error)
				.setMessage(R.string.login_network_error)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, null)
				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on http error
							}
						})*/;
				//add button for contact
				dialog = dialogBuilder.create();
				break;

			case DIALOG_INTERNAL_ERROR:
				dialogBuilder.setTitle(R.string.login_error)
				.setMessage(R.string.login_server_error)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, null)
				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on http error
							}
						})*/;
				//add button for contact
				dialog = dialogBuilder.create();
				break;

			case DIALOG_LOGIN_PROGRESS: {
				ProgressDialog pDialog = new ProgressDialog(this);
				pDialog.setMessage(getString(R.string.login_authenticating, getString(R.string.server_name)));
				pDialog.setCancelable(false);
				//pDialog.setIndeterminate(true);
				dialog = pDialog;
				break;
			}
			case DIALOG_DOWNLOADING_CAMPAIGNS: {
				ProgressDialog pDialog = new ProgressDialog(this);
				pDialog.setMessage(getString(R.string.login_download_campaign));
				pDialog.setCancelable(false);
				//pDialog.setIndeterminate(true);
				dialog = pDialog;
				break;
			}
			case DIALOG_SERVER_LIST: {
	            AlertDialog.Builder builder = new AlertDialog.Builder(this);
				CharSequence[] servers = getResources().getStringArray(R.array.servers);
				final ArrayAdapter<CharSequence> adapter =
						new ArrayAdapter<CharSequence>(this, R.layout.simple_list_item_1, servers);

	            builder.setTitle(R.string.login_choose_server);
				builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mServerEdit.setText(((AlertDialog) dialog).getListView().getAdapter().getItem(which).toString());
					}
				});

				dialog = builder.create();
				break;
			}
		}

		return dialog;
	}
	
	private void onLoginTaskDone(OhmageApi.AuthenticateResponse response, final String username) {

		try {
			dismissDialog(DIALOG_LOGIN_PROGRESS);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Attempting to dismiss dialog that had not been shown.");
			e.printStackTrace();
		}
		
		switch (response.getResult()) {
		case SUCCESS:
			Log.i(TAG, "login success");
			final String hashedPassword = response.getHashedPassword();

			if(ConfigHelper.isSingleCampaignMode()) {
				// Download the single campaign
				showDialog(DIALOG_DOWNLOADING_CAMPAIGNS);

				// Temporarily set the user credentials. If the download fails, they will be removed
				// We need to set this so the ResponseSyncService can use them once the campaign is downloaded
				mPreferencesHelper.putUsername(username);
				mPreferencesHelper.putHashedPassword(hashedPassword);

				mCampaignDownloadTask.setCredentials(username, hashedPassword);
				mCampaignDownloadTask.forceLoad();
			} else {
				loginFinished(username, hashedPassword);
			}
			break;
		case FAILURE:
			//clear password so user will re-enter it
			mPasswordEdit.setText("");

			//show error dialog
			if (response.getErrorCodes().contains(OhmageApi.ERROR_ACCOUNT_DISABLED)) {
				showDialog(DIALOG_USER_DISABLED);
			} else {
				showDialog(DIALOG_LOGIN_ERROR);
			}
			break;
		case HTTP_ERROR:
			//show error dialog
			showDialog(DIALOG_NETWORK_ERROR);
			break;
		case INTERNAL_ERROR:
			//show error dialog
			showDialog(DIALOG_INTERNAL_ERROR);
			break;
		}
	}

	private void loginFinished(String username, String hashedPassword) {

		//save creds
		mPreferencesHelper.putUsername(username);
		mPreferencesHelper.putHashedPassword(hashedPassword);

		// Upgrading the mobility data will set the username so we only need to set it if we don't upgrade
		if(MobilityHelper.upgradeMobilityData(this) != MobilityHelper.VERSION_AGGREGATE_AND_USERNAME)
			MobilityHelper.setUsername(this, username);

		//clear related notifications
		//NotificationHelper.cancel(LoginActivity.this, NotificationHelper.NOTIFY_LOGIN_FAIL);
		//makes more sense to clear notification on launch, so moved to oncreate

		//start services
		//set alarms
		//register receivers
		//BackgroundManager.initAuthComponents(this);

		if (mAppPrefs.isFirstRun()) {
			Log.i(TAG, "this is the first run");

			BackgroundManager.initComponents(this);

			//cancel get started notification. this works regardless of how we start the app (notification or launcher)
			//NotificationHelper.cancel(this, NotificationHelper.NOTIFY_GET_STARTED, null);

			//show intro dialog
			//showDialog(DIALOG_FIRST_RUN);
			mAppPrefs.setFirstRun(false);
		} else {
			Log.i(TAG, "this is not the first run");
		}

		mPreferencesHelper.putLoginTimestamp(System.currentTimeMillis());

		if(mUpdateCredentials)
			finish();
		else
			startActivityForResult(new Intent(this, DashboardActivity.class), LOGIN_FINISHED);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
			case LOGIN_FINISHED:
				finish();
			break;
			default:
				this.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private static class LoginTask extends ManagedAsyncTask<String, Void, OhmageApi.AuthenticateResponse>{

		private String mUsername;
		private String mPassword;

		public LoginTask(FragmentActivity activity) {
			super(activity);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			getActivity().showDialog(DIALOG_LOGIN_PROGRESS);
		}

		@Override
		protected OhmageApi.AuthenticateResponse doInBackground(String... params) {
			mUsername = params[0];
			mPassword = params[1];
			
//			if (mPassword.equals(DUMMY_PASSWORD)) {
//				Log.i(TAG, "using stored hashed password");
//			} else {
//				Log.i(TAG, "password field modified, attempting to hash");
//				try {
//		        	mHashedPassword = BCrypt.hashpw(mPassword, BCRYPT_KEY);
//		        	Log.i(TAG, "hash complete");
//		        } catch (Exception e) {
//		        	Log.e(TAG, "unable to hash password");
//		        	e.printStackTrace();
//		        }
//			}
			OhmageApi api = new OhmageApi(getActivity());
			return api.authenticate(ConfigHelper.serverUrl(), mUsername, mPassword, OhmageApi.CLIENT_NAME);
		}

		@Override
		protected void onPostExecute(OhmageApi.AuthenticateResponse response) {
			super.onPostExecute(response);
			response.handleError(getActivity());
			((LoginActivity) getActivity()).onLoginTaskDone(response, mUsername);
		}
	}

	/**
	 * Ensures that the server url provided is valid. Once it is made valid, it is set as the server url.
	 * @return 
	 */
	private boolean ensureServerUrl() {
		String text = mServerEdit.getText().toString();
		text = URLUtil.guessUrl(text);

		if(URLUtil.isHttpsUrl(text) || URLUtil.isHttpUrl(text)) {
			mServerEdit.setText(text);
			return true;
		}

		return false;
	}
}
