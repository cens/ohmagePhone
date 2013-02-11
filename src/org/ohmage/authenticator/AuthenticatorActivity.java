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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.ohmage.AccountHelper;
import org.ohmage.BackgroundManager;
import org.ohmage.ConfigHelper;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi.AuthenticateResponse;
import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.async.CampaignReadTask;
import org.ohmage.db.DbContract;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.utils.Lists;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.LogProbe.Status;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorFragmentActivity {
    private static final String TAG = "AuthenticatorActivity";

    private static final int LOGIN_FINISHED = 0;
    private static final int DIALOG_FIRST_RUN = 1;
    private static final int DIALOG_LOGIN_ERROR = 2;
    private static final int DIALOG_NETWORK_ERROR = 3;
    private static final int DIALOG_LOGIN_PROGRESS = 4;
    private static final int DIALOG_INTERNAL_ERROR = 5;
    private static final int DIALOG_USER_DISABLED = 6;
    private static final int DIALOG_DOWNLOADING_CAMPAIGNS = 7;
    private static final int DIALOG_SERVER_LIST = 8;

    public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";
    public static final String PARAM_PASSWORD = "password";

    /**
     * The {@link AuthenticatorActivity} looks for this extra to determine if it
     * should update the credentials for the user
     */
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

    private static final String KEY_OHMAGE_SERVER = "key_ohmage_server";

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

    private EditText mServerEdit;
    private UserPreferencesHelper mPreferencesHelper;
    private ConfigHelper mAppPrefs;
    private CampaignReadTask mCampaignDownloadTask;
    private String mHashedPassword;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mAccountManager = AccountManager.get(this);

        final Intent intent = getIntent();
        mUsername = intent.getStringExtra(PARAM_USERNAME);
        mAuthtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);

        // If we are just logging in regularly, we need to set the authtoken
        // type
        if (mAuthtokenType == null) {
            mAuthtokenType = OhmageApplication.AUTHTOKEN_TYPE;
        }

        mRequestNewAccount = mUsername == null;
        mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRMCREDENTIALS, false);

        mPreferencesHelper = new UserPreferencesHelper(this);
        mAppPrefs = new ConfigHelper(this);

        if (mPreferencesHelper.isUserDisabled()) {
            ((OhmageApplication) getApplication()).resetAll();
        }

        // if they are, redirect them to the dashboard
        if (AccountHelper.accountExists() && !mConfirmCredentials) {
            startActivityForResult(new Intent(this, DashboardActivity.class), LOGIN_FINISHED);
            return;
        }

        setContentView(R.layout.login);

        mMessage = (TextView) findViewById(R.id.version);
        mUsernameEdit = (EditText) findViewById(R.id.login_username);
        mPasswordEdit = (EditText) findViewById(R.id.login_password);
        mServerEdit = (EditText) findViewById(R.id.login_server_edit);

        if (mConfirmCredentials) {
            mUsernameEdit.setEnabled(false);
            mPasswordEdit.requestFocus();
        }

        mUsernameEdit.setText(mUsername);
        mMessage.setText(getVersion());

        TextView registerAccountLink = (TextView) findViewById(R.id.login_register_new_account);
        registerAccountLink.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // reads the currently selected server and fires a browser
                // intent which takes the user to the registration page for that
                // server
                if (ensureServerUrl()) {
                    // use the textbox to make a url
                    String url = mServerEdit.getText().toString().split(" ")[0] + "#register";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } else
                    Toast.makeText(v.getContext(), R.string.login_invalid_server,
                            Toast.LENGTH_SHORT).show();
            }
        });

        if (getResources().getBoolean(R.bool.allow_custom_server)) {
            View serverContainer = findViewById(R.id.login_server_container);
            serverContainer.setVisibility(View.VISIBLE);
        }

        String defaultServer = ConfigHelper.serverUrl();
        if (TextUtils.isEmpty(defaultServer))
            defaultServer = getResources().getStringArray(R.array.servers)[0];
        mServerEdit.setText(defaultServer);
        ensureServerUrl();
        mServerEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
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

        mCampaignDownloadTask = (CampaignReadTask) getSupportLoaderManager().initLoader(0, null,
                new LoaderManager.LoaderCallbacks<CampaignReadResponse>() {

                    @Override
                    public Loader<CampaignReadResponse> onCreateLoader(int id, Bundle args) {
                        return new CampaignReadTask(AuthenticatorActivity.this, null, null);
                    }

                    @Override
                    public void onLoadFinished(Loader<CampaignReadResponse> loader,
                            CampaignReadResponse data) {
                        String urn = Campaign.getSingleCampaign(AuthenticatorActivity.this);
                        if (urn == null) {
                            Toast.makeText(AuthenticatorActivity.this,
                                    R.string.login_error_downloading_campaign, Toast.LENGTH_LONG)
                                    .show();
                        } else {
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

    private CharSequence getVersion() {
        try {
            return "v" + getPackageManager().getPackageInfo("org.ohmage", 0).versionName;
        } catch (Exception e) {
            Log.e(TAG, "unable to retrieve version", e);
            return null;
        }
    }

    /**
     * The easiest way to make sure the progress dialog is hidden when it is
     * supposed to be is to have a static reference to it...
     */
    private static ProgressDialog pDialog;

    @Override
    public void onDestroy() {
        super.onDestroy();
        pDialog = null;

        getSupportLoaderManager().destroyLoader(0);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = super.onCreateDialog(id);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        switch (id) {
            case DIALOG_FIRST_RUN:
                dialogBuilder
                        .setTitle(R.string.eula_title)
                        .setMessage(R.string.eula_text)
                        .setCancelable(false)
                        .setPositiveButton(R.string.eula_accept,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mAppPrefs.setFirstRun(false);
                                    }
                                })
                        .setNegativeButton(R.string.eula_cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        AuthenticatorActivity.this.finish();
                                    }
                                });
                dialog = dialogBuilder.create();
                break;

            case DIALOG_LOGIN_ERROR:
                dialogBuilder.setTitle(R.string.login_error)
                        .setMessage(R.string.login_invalid_password).setCancelable(true)
                        .setPositiveButton(R.string.ok, null)
                /*
                 * .setNeutralButton("Help", new
                 * DialogInterface.OnClickListener() {
                 * @Override public void onClick(DialogInterface dialog, int
                 * which) { startActivity(new Intent(LoginActivity.this,
                 * HelpActivity.class)); //put extras for specific help on login
                 * error } })
                 */;
                // add button for contact
                dialog = dialogBuilder.create();
                break;

            case DIALOG_USER_DISABLED:
                dialogBuilder.setTitle(R.string.login_error)
                        .setMessage(R.string.login_account_disabled).setCancelable(true)
                        .setPositiveButton(R.string.ok, null)
                /*
                 * .setNeutralButton("Help", new
                 * DialogInterface.OnClickListener() {
                 * @Override public void onClick(DialogInterface dialog, int
                 * which) { startActivity(new Intent(LoginActivity.this,
                 * HelpActivity.class)); //put extras for specific help on login
                 * error } })
                 */;
                // add button for contact
                dialog = dialogBuilder.create();
                break;

            case DIALOG_NETWORK_ERROR:
                dialogBuilder.setTitle(R.string.login_error)
                        .setMessage(R.string.login_network_error).setCancelable(true)
                        .setPositiveButton(R.string.ok, null)
                /*
                 * .setNeutralButton("Help", new
                 * DialogInterface.OnClickListener() {
                 * @Override public void onClick(DialogInterface dialog, int
                 * which) { startActivity(new Intent(LoginActivity.this,
                 * HelpActivity.class)); //put extras for specific help on http
                 * error } })
                 */;
                // add button for contact
                dialog = dialogBuilder.create();
                break;

            case DIALOG_INTERNAL_ERROR:
                dialogBuilder.setTitle(R.string.login_error)
                        .setMessage(R.string.login_server_error).setCancelable(true)
                        .setPositiveButton(R.string.ok, null)
                /*
                 * .setNeutralButton("Help", new
                 * DialogInterface.OnClickListener() {
                 * @Override public void onClick(DialogInterface dialog, int
                 * which) { startActivity(new Intent(LoginActivity.this,
                 * HelpActivity.class)); //put extras for specific help on http
                 * error } })
                 */;
                // add button for contact
                dialog = dialogBuilder.create();
                break;

            case DIALOG_LOGIN_PROGRESS: {
                pDialog = new ProgressDialog(this);
                pDialog.setMessage(getString(R.string.login_authenticating,
                        getString(R.string.server_name)));
                pDialog.setIndeterminate(true);
                pDialog.setCancelable(true);
                pDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
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
                // pDialog.setIndeterminate(true);
                dialog = pDialog;
                break;
            }
            case DIALOG_SERVER_LIST: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                ArrayList<String> servers = Lists.newArrayList(getResources().getStringArray(
                        R.array.servers));

                if (OhmageApplication.isDebugBuild()) {

                    servers.add("https://test.ohmage.org/");
                    servers.add("https://dev.ohmage.org/");
                }

                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        R.layout.simple_list_item_1, servers);

                builder.setTitle(R.string.login_choose_server);
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mServerEdit.setText(((AlertDialog) dialog).getListView().getAdapter()
                                .getItem(which).toString());
                    }
                });

                dialog = builder.create();
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
        Analytics.widget(view);

        if (!ensureServerUrl()) {
            Toast.makeText(this, R.string.login_invalid_server, Toast.LENGTH_SHORT).show();
            return;
        }

        String server = mServerEdit.getText().toString();
        ConfigHelper.setServerUrl(server.split("\\(")[0].trim());
        configureForDeployment(server);

        if (mRequestNewAccount) {
            mUsername = mUsernameEdit.getText().toString();
        }
        mPassword = mPasswordEdit.getText().toString();
        if (!TextUtils.isEmpty(mUsername) && !TextUtils.isEmpty(mPassword)) {
            showDialog(DIALOG_LOGIN_PROGRESS);
            // Start authenticating...
            mAuthThread = AuthenticationUtilities.attemptAuth(mUsername, mPassword, mHandler,
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
        Log.v(TAG, "finishConfirmCredentials()");
        final Account account = new Account(mUsername, OhmageApplication.ACCOUNT_TYPE);
        mAccountManager.setPassword(account, mPassword);
        if (mAuthtokenType != null && mAuthtokenType.equals(OhmageApplication.AUTHTOKEN_TYPE)) {
            mAccountManager.setAuthToken(account, mAuthtokenType, mHashedPassword);
        }
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Called when response is received from the server for authentication
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller. Also sets
     * the authToken in AccountManager for this account.
     * 
     * @param the confirmCredentials result.
     */

    protected void finishLogin() {
        Log.v(TAG, "finishLogin()");
        final Account account = new Account(mUsername, OhmageApplication.ACCOUNT_TYPE);
        Bundle userData = new Bundle();
        userData.putString(KEY_OHMAGE_SERVER, mServerEdit.getText().toString());
        mAuthtoken = mHashedPassword;

        if (mRequestNewAccount) {
            mAccountManager.addAccountExplicitly(account, mPassword, userData);
            mAccountManager.setAuthToken(account, OhmageApplication.AUTHTOKEN_TYPE, mAuthtoken);
            // Set sync for this account.
            ContentResolver.setIsSyncable(account, DbContract.CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, DbContract.CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, DbContract.CONTENT_AUTHORITY, new Bundle(),
                    3600);
        } else {
            mAccountManager.setPassword(account, mPassword);
        }
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, OhmageApplication.ACCOUNT_TYPE);
        if (mAuthtokenType != null && mAuthtokenType.equals(OhmageApplication.AUTHTOKEN_TYPE)) {
            intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
        }
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);

        if (mAppPrefs.isFirstRun()) {
            Log.v(TAG, "this is the first run");

            BackgroundManager.initComponents(this);

            // cancel get started notification. this works regardless of how we
            // start the app (notification or launcher)
            // NotificationHelper.cancel(this,
            // NotificationHelper.NOTIFY_GET_STARTED, null);

            // show intro dialog
            // showDialog(DIALOG_FIRST_RUN);
            mAppPrefs.setFirstRun(false);
        }

        mPreferencesHelper.putLoginTimestamp(System.currentTimeMillis());

        if (mConfirmCredentials)
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
            if (pDialog != null)
                pDialog.dismiss();
        }

        switch (response.getResult()) {
            case SUCCESS:
                Log.v(TAG, "login success");
                mHashedPassword = response.getHashedPassword();
                if (!mConfirmCredentials) {
                    if (ConfigHelper.isSingleCampaignMode()) {
                        final String hashedPassword = response.getHashedPassword();
                        // Download the single campaign
                        showDialog(DIALOG_DOWNLOADING_CAMPAIGNS);
                        mCampaignDownloadTask.setCredentials(mUsername, mHashedPassword);
                        mCampaignDownloadTask.forceLoad();
                    } else {
                        finishLogin();
                    }
                } else {
                    finishConfirmCredentials(true);
                }
                break;
            case FAILURE:
                Log.e(TAG, "login failure: " + response.getErrorCodes());

                // show error dialog
                if (Arrays.asList(response.getErrorCodes()).contains("0201")) {
                    mPreferencesHelper.setUserDisabled(true);
                    showDialog(DIALOG_USER_DISABLED);
                } else {
                    showDialog(DIALOG_LOGIN_ERROR);
                }
                break;
            case HTTP_ERROR:
                Log.w(TAG, "login http error");

                // show error dialog
                showDialog(DIALOG_NETWORK_ERROR);
                break;
            case INTERNAL_ERROR:
                Log.e(TAG, "login internal error");

                // show error dialog
                showDialog(DIALOG_INTERNAL_ERROR);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOGIN_FINISHED:
                finish();
                break;
            default:
                this.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Ensures that the server url provided is valid. Once it is made valid, it
     * is set as the server url.
     * 
     * @return
     */
    private boolean ensureServerUrl() {
        String text = mServerEdit.getText().toString();
        text = URLUtil.guessUrl(text);

        if (URLUtil.isHttpsUrl(text) || URLUtil.isHttpUrl(text)) {
            mServerEdit.setText(text);
            return true;
        }

        return false;
    }

    /**
     * Configures some settings based on the deployment. Looks at the server url
     * and deployment name to figure out what the settings should be
     * 
     * @param server
     */
    private void configureForDeployment(String server) {
        if (server == null)
            return;

        server = server.split(" ")[0];

        ConfigHelper config = new ConfigHelper(this);

        if ("https://lausd.mobilizingcs.org/".equals(server)) {
            mPreferencesHelper.setShowFeedback(true);
            mPreferencesHelper.setShowMobility(false);
            mPreferencesHelper.setUploadResponsesWifiOnly(false);
            mPreferencesHelper.setUploadProbesWifiOnly(true);
            config.setAdminMode(false);
            config.setLogLevel("verbose");
            config.setLogAnalytics(true);
            ((OhmageApplication) getApplication()).updateLogLevel();
        } else if ("https://pilots.mobilizelabs.org/".equals(server)) {
            mPreferencesHelper.setShowFeedback(true);
            mPreferencesHelper.setShowMobility(false);
            mPreferencesHelper.setUploadResponsesWifiOnly(false);
            mPreferencesHelper.setUploadProbesWifiOnly(true);
            config.setAdminMode(false);
            config.setLogLevel("error");
            config.setLogAnalytics(false);
            ((OhmageApplication) getApplication()).updateLogLevel();
        } else if ("https://dev.ohmage.org/".equals(server)
                || "https://test.ohmage.org/".equals(server)) {
            mPreferencesHelper.setShowFeedback(true);
            mPreferencesHelper.setShowMobility(true);
            mPreferencesHelper.setUploadResponsesWifiOnly(false);
            mPreferencesHelper.setUploadProbesWifiOnly(false);
            config.setAdminMode(true);
            config.setLogLevel("verbose");
            config.setLogAnalytics(true);
            ((OhmageApplication) getApplication()).updateLogLevel();
        } else if ("https://play.ohmage.org/".equals(server)) {
            mPreferencesHelper.setShowFeedback(true);
            mPreferencesHelper.setShowMobility(true);
            mPreferencesHelper.setUploadResponsesWifiOnly(false);
            mPreferencesHelper.setUploadProbesWifiOnly(true);
            config.setAdminMode(true);
            config.setLogLevel("error");
            config.setLogAnalytics(false);
            ((OhmageApplication) getApplication()).updateLogLevel();
        }
    }
}
