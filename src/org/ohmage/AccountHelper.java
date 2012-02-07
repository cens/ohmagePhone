package org.ohmage;


import org.ohmage.authenticator.AuthenticatorActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;

import java.io.IOException;

/**
 * Helper class which makes it easy to show the account management dialogs
 * such as the auth pin dialog, logout dialog, etc.
 * @author cketcham
 *
 */
public class AccountHelper {
	private static final int DIALOG_CLEAR_USER_CONFIRM = 100;
	private static final int DIALOG_WIPE_PROGRESS = 101;

	private final Context mContext;
	private final AccountManager mAccountManager;

	public AccountHelper(Context context) {
		mContext = context;
		mAccountManager = AccountManager.get(mContext);
	}

	/**
	 * Gets the Ohmage Account
	 * @return
	 */
	public Account getAccount() {
		Account[] accounts = mAccountManager.getAccountsByType(OhmageApplication.ACCOUNT_TYPE);
		if(accounts.length == 0)
			return null;
		return accounts[0];
	}

	/**
	 * Take the user to the login activity,
	 * but allow them to back out of it if they change their mind
	 * @return 
	 */
	public AccountManagerFuture<Bundle> updatePassword() {
		Account account = getAccount();
		if(account != null) {
			if(mContext instanceof Activity)
				return mAccountManager.confirmCredentials(account, null, (Activity) mContext, null, null);
			else
				return mAccountManager.confirmCredentials(account, null, null, null, null);
		}
		return null;
	}

	/**
	 * Returns the intent to update the password. Should not be run on UI thread
	 * @param context
	 * @return intent to update the password
	 */
	public static Intent updatePasswordIntent(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccountsByType(OhmageApplication.ACCOUNT_TYPE);
		AccountManagerFuture<Bundle> future = accountManager.confirmCredentials(accounts[0], null, null, null, null);
		try {
			return (Intent) future.getResult().get(AccountManager.KEY_INTENT);
		} catch (OperationCanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the current account username
	 * @return
	 */
	public String getUsername() {
		Account account = getAccount();
		if(account != null)
			return account.name;
		return null;
	}

	/**
	 * Retrieve the auth token. Either from our cached token or from the account manager
	 * @return the authtoken or null if we don't have it yet
	 */
	public String getAuthToken() {
		if(Thread.currentThread() == Looper.getMainLooper().getThread())
			return mAccountManager.peekAuthToken(getAccount(), OhmageApplication.AUTHTOKEN_TYPE);

		AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(getAccount(), OhmageApplication.AUTHTOKEN_TYPE, false, null, null);
		try {
			Bundle result = future.getResult();
			if(result != null)
				return result.getString(AccountManager.KEY_AUTHTOKEN);
		} catch (OperationCanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Checks to see if there is an account to determine if the user is already authenticated
	 * @param context
	 * @return true if there is an account, and false if there isn't
	 */
	public static boolean accountExists(Context context) {
		Account[] accounts = AccountManager.get(context).getAccountsByType(OhmageApplication.ACCOUNT_TYPE);
		return accounts != null && accounts.length > 0;
	}

	public boolean accountExists() {
		if(!accountExists(mContext)) {
			if(mContext instanceof Activity) {
				mContext.startActivity(getLoginIntent(mContext));
				((Activity) mContext).finish();
			}
			return false;
		}
		return true;
	}

	/**
	 * Shows the confirmation dialog for the user to logout
	 */
	public void logout() {
		if(mContext instanceof Activity)
			((Activity) mContext).showDialog(DIALOG_CLEAR_USER_CONFIRM);
	}

	/**
	 * Creates the dialogs for the activity
	 * @param id
	 * @return
	 */
	public Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
		switch (id) {
			case DIALOG_CLEAR_USER_CONFIRM:			
				dialogBuilder.setTitle("Confirm")
				.setMessage("Are you sure you wish to clear all user data? Any data that has not been uploaded will be lost, and the app will be restored to its initial state.")
				.setNegativeButton("No", null)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						clearAndGotoLogin();
					}

				});
				dialog = dialogBuilder.create();        	
				break;
			case DIALOG_WIPE_PROGRESS:
				ProgressDialog pDialog = new ProgressDialog(mContext);
				pDialog.setMessage("Clearing local user data...");
				pDialog.setCancelable(false);
				//pDialog.setIndeterminate(true);
				dialog = pDialog;
				break;
		}

		return dialog;
	}

	/**
	 * 1) Clears the user's data,
	 * 2) redirects the user to the login page, and
	 * 3) clears the backstack + makes it a new task, so they can't get back into the app
	 */
	private void clearAndGotoLogin()  {
		// create a task that asynchronously clears their data and displays a "waiting" dialog in the meantime
		AsyncTask<Void, Void, Void> wipeTask = new AsyncTask<Void,Void,Void>() {
			@Override
			protected void onPreExecute() {
				super.onPreExecute();

				if(mContext instanceof Activity)
					((Activity) mContext).showDialog(DIALOG_WIPE_PROGRESS);
			}

			@Override
			protected Void doInBackground(Void... params) {
				((OhmageApplication)mContext.getApplicationContext()).resetAll();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);

				if(mContext instanceof Activity) {
					((Activity) mContext).dismissDialog(DIALOG_WIPE_PROGRESS);

					// then send them on a one-way trip to the login screen
					mContext.startActivity(getLoginIntent(mContext));
					((Activity) mContext).finish();
				}
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();

				// FIXME: we should probably indicate that the task is cancelled, but this is probably fine for now
				if(mContext instanceof Activity)
					((Activity) mContext).dismissDialog(DIALOG_WIPE_PROGRESS);
			}
		};

		wipeTask.execute();
	}

	public static Intent getLoginIntent(Context context) {
		Intent intent = new Intent(context, AuthenticatorActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}
}