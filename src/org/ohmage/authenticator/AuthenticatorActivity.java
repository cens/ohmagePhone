/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ohmage.authenticator;

import org.ohmage.AccountHelper;
import org.ohmage.BackgroundManager;
import org.ohmage.Config;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi.AuthenticateResponse;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.async.CampaignReadTask;
import org.ohmage.db.Models.Campaign;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorFragmentActivity {
	private static final int DIALOG_FIRST_RUN = 1;
	private static final int DIALOG_LOGIN_ERROR = 2;
	private static final int DIALOG_NETWORK_ERROR = 3;
	private static final int DIALOG_LOGIN_PROGRESS = 4;
	private static final int DIALOG_INTERNAL_ERROR = 5;
	private static final int DIALOG_USER_DISABLED = 6;
	private static final int DIALOG_DOWNLOADING_CAMPAIGNS = 7;
	private static final int LOGIN_FINISHED = 0;

	public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";
	public static final String PARAM_PASSWORD = "password";
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

	private static final String TAG = "AuthenticatorActivity";

	private AccountManager mAccountManager;
	private Thread mAuthThread;
	private String mAuthtoken;
	private String mAuthtokenType;

	/**
	 * If set we are just checking that the user knows their credentials; this
	 * doesn't cause the user's password to be changed on the device.
	 */
	private Boolean mConfirmCredentials = false;

	/** for posting authentication attempts back to UI thread */
	private final Handler mHandler = new Handler();
	private TextView mMessage;
	private String mPassword;
	private EditText mPasswordEdit;

	/** Was the original caller asking for an entirely new account? */
	protected boolean mRequestNewAccount = false;

	private String mUsername;
	private EditText mUsernameEdit;
	private SharedPreferencesHelper mPreferencesHelper;
	private CampaignReadTask mCampaignDownloadTask;
	private String mHashedPassword;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle icicle) {
		Log.i(TAG, "onCreate(" + icicle + ")");
		super.onCreate(icicle);
		mAccountManager = AccountManager.get(this);
		Log.i(TAG, "loading data from Intent");
		final Intent intent = getIntent();
		mUsername = intent.getStringExtra(PARAM_USERNAME);
		mAuthtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);

		// If we are just logging in regularly, we need to set the authtoken type
		if(mAuthtokenType == null) {
			mAuthtokenType = OhmageApplication.AUTHTOKEN_TYPE;
		}

		mRequestNewAccount = mUsername == null;
		mConfirmCredentials =
				intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);

		mPreferencesHelper = new SharedPreferencesHelper(this);

		// if they are, redirect them to the dashboard
		if (AccountHelper.accountExists(this) && !mConfirmCredentials) {
			startActivityForResult(new Intent(this, DashboardActivity.class), LOGIN_FINISHED);
			return;
		}

		setContentView(R.layout.login);

		if (mPreferencesHelper.isUserDisabled()) {
			((OhmageApplication) getApplication()).resetAll();
		}

		mMessage = (TextView) findViewById(R.id.version);
		mUsernameEdit = (EditText) findViewById(R.id.user_input);
		mPasswordEdit = (EditText) findViewById(R.id.password);

		if(mConfirmCredentials) {
			mUsernameEdit.setEnabled(false);
			mPasswordEdit.requestFocus();
		}

		mUsernameEdit.setText(mUsername);
		mMessage.setText(getVersion());

		mCampaignDownloadTask = (CampaignReadTask) getSupportLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<CampaignReadResponse>() {

			@Override
			public Loader<CampaignReadResponse> onCreateLoader(int id, Bundle args) {
				return new CampaignReadTask(AuthenticatorActivity.this, null);
			}

			@Override
			public void onLoadFinished(Loader<CampaignReadResponse> loader,
					CampaignReadResponse data) {
				String urn = Campaign.getSingleCampaign(AuthenticatorActivity.this);
				if(urn == null)
					Toast.makeText(AuthenticatorActivity.this, R.string.login_error_downloading_campaign, Toast.LENGTH_LONG).show();
				else {
					finishLogin();
				}
				dismissDialog(DIALOG_DOWNLOADING_CAMPAIGNS);
			}

			@Override
			public void onLoaderReset(Loader<CampaignReadResponse> loader) {
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		// Hide any notifications since we started the login activity
		NotificationHelper.hideAuthNotification(this);
	}

	private CharSequence getVersion() {
		try {
			return "v" + getPackageManager().getPackageInfo("org.ohmage", 0).versionName;
		} catch (Exception e) {
			Log.e(TAG, "unable to retrieve version", e);
			return null;
		}
	}

	/**
	 * The easiest way to make sure the progress dialog is hidden when it is supposed to be
	 * is to have a static reference to it...
	 */
	private static ProgressDialog pDialog;

	@Override
	public void onDestroy() {
		super.onDestroy();
		pDialog = null;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(TAG, "creating dialog" + id);

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
						mPreferencesHelper.setFirstRun(false);
					}
				})
				.setNegativeButton(R.string.eula_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						AuthenticatorActivity.this.finish();
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
				pDialog = new ProgressDialog(this);
				pDialog.setMessage(getString(R.string.login_authenticating, getString(R.string.server_name)));
				pDialog.setIndeterminate(true);
				pDialog.setCancelable(true);
				pDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						Log.i(TAG, "dialog cancel has been invoked");
						if (mAuthThread != null) {
							mAuthThread.interrupt();
							finish();
						}
					}
				});
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
		}

		return dialog;
	}

	/**
	 * Handles onClick event on the Submit button. Sends username/password to
	 * the server for authentication.
	 * 
	 * @param view The Submit button for which this method is invoked
	 */
	public void handleLogin(View view) {
		if (mRequestNewAccount) {
			mUsername = mUsernameEdit.getText().toString();
		}
		mPassword = mPasswordEdit.getText().toString();
		if (!TextUtils.isEmpty(mUsername) && !TextUtils.isEmpty(mPassword)) {
			showDialog(DIALOG_LOGIN_PROGRESS);
			// Start authenticating...
			mAuthThread =
					AuthenticationUtilities.attemptAuth(mUsername, mPassword, mHandler,
							AuthenticatorActivity.this);
		}
	}

	/**
	 * Called when response is received from the server for confirm credentials
	 * request. See onAuthenticationResult(). Sets the
	 * AccountAuthenticatorResult which is sent back to the caller.
	 * 
	 * @param the confirmCredentials result.
	 */
	protected void finishConfirmCredentials(boolean result) {
		Log.i(TAG, "finishConfirmCredentials()");
		final Account account = new Account(mUsername, OhmageApplication.ACCOUNT_TYPE);
		mAccountManager.setPassword(account, mPassword);
		if (mAuthtokenType != null
				&& mAuthtokenType.equals(OhmageApplication.AUTHTOKEN_TYPE)) {
			mAccountManager.setAuthToken(account, mAuthtokenType, mHashedPassword);
		}
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * 
	 * Called when response is received from the server for authentication
	 * request. See onAuthenticationResult(). Sets the
	 * AccountAuthenticatorResult which is sent back to the caller. Also sets
	 * the authToken in AccountManager for this account.
	 * 
	 * @param the confirmCredentials result.
	 */

	protected void finishLogin() {
		Log.i(TAG, "finishLogin()");
		final Account account = new Account(mUsername, OhmageApplication.ACCOUNT_TYPE);

		if (mRequestNewAccount) {
			mAccountManager.addAccountExplicitly(account, mPassword, null);
			// Set contacts sync for this account.
			ContentResolver.setSyncAutomatically(account,
					ContactsContract.AUTHORITY, true);
		} else {
			mAccountManager.setPassword(account, mPassword);
		}
		final Intent intent = new Intent();
		mAuthtoken = mHashedPassword;
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, OhmageApplication.ACCOUNT_TYPE);
		if (mAuthtokenType != null
				&& mAuthtokenType.equals(OhmageApplication.AUTHTOKEN_TYPE)) {
			intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
		}
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);


		boolean isFirstRun = mPreferencesHelper.isFirstRun();

		if (isFirstRun) {
			Log.i(TAG, "this is the first run");

			BackgroundManager.initComponents(this);

			//cancel get started notification. this works regardless of how we start the app (notification or launcher)
			//NotificationHelper.cancel(this, NotificationHelper.NOTIFY_GET_STARTED, null);

			//show intro dialog
			//showDialog(DIALOG_FIRST_RUN);
			mPreferencesHelper.setFirstRun(false);
			mPreferencesHelper.putLoginTimestamp(System.currentTimeMillis());
		}

		if(mConfirmCredentials)
			finish();
		else
			startActivityForResult(new Intent(this, DashboardActivity.class), LOGIN_FINISHED);
	}

	/**
	 * Called when the authentication process completes (see attemptLogin()).
	 */
	public void onAuthenticationResult(AuthenticateResponse response) {

		try {
			dismissDialog(DIALOG_LOGIN_PROGRESS);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Attempting to dismiss dialog that had not been shown.");
			e.printStackTrace();
			if(pDialog != null)
				pDialog.dismiss();
		}

		switch (response.getResult()) {
			case SUCCESS:
				Log.i(TAG, "login success");		
				mHashedPassword = response.getHashedPassword();
				if (!mConfirmCredentials) {
					if(Config.IS_SINGLE_CAMPAIGN) {
						final String hashedPassword = response.getHashedPassword();
						// Download the single campaign
						showDialog(DIALOG_DOWNLOADING_CAMPAIGNS);
						mCampaignDownloadTask.setCredentials(new AccountHelper(this));
						mCampaignDownloadTask.forceLoad();
					} else {
						finishLogin();
					}
				} else {
					finishConfirmCredentials(true);
				}
				break;
			case FAILURE:
				Log.e(TAG, "login failure");
				for (String s : response.getErrorCodes()) {
					Log.e(TAG, "error code: " + s);
				}

				//show error dialog
				if (Arrays.asList(response.getErrorCodes()).contains("0201")) {
					mPreferencesHelper.setUserDisabled(true);
					showDialog(DIALOG_USER_DISABLED);
				} else {
					showDialog(DIALOG_LOGIN_ERROR);
				}
				break;
			case HTTP_ERROR:
				Log.e(TAG, "login http error");

				//show error dialog
				showDialog(DIALOG_NETWORK_ERROR);
				break;
			case INTERNAL_ERROR:
				Log.e(TAG, "login internal error");

				//show error dialog
				showDialog(DIALOG_INTERNAL_ERROR);
				break;
		}
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
}
