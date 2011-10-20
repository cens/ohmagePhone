package org.ohmage;

import org.ohmage.activity.LoginActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;

/**
 * Helper class which makes it easy to show the account management dialogs
 * such as the auth pin dialog, logout dialog, etc.
 * @author cketcham
 *
 */
public class AccountHelper {
	private static final int DIALOG_CLEAR_USER_CONFIRM = 100;
	private static final int DIALOG_WIPE_PROGRESS = 101;

	private final Activity mActivity;

	public AccountHelper(Activity activity) {
		mActivity = activity;
	}

	/**
	 * Shows the confirmation dialog for the user to logout
	 */
	public void logout() {
		mActivity.showDialog(DIALOG_CLEAR_USER_CONFIRM);
	}

	/**
	 * Take the user to the login activity,
	 * but allow them to back out of it if they change their mind
	 */
	public void updatePassword() {
		Intent intent = new Intent(mActivity, LoginActivity.class);
		intent.putExtra(LoginActivity.EXTRA_UPDATE_CREDENTIALS, true);
		mActivity.startActivity(intent);
	}

	public Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mActivity);
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
				ProgressDialog pDialog = new ProgressDialog(mActivity);
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

				mActivity.showDialog(DIALOG_WIPE_PROGRESS);
			}

			@Override
			protected Void doInBackground(Void... params) {
				((OhmageApplication)mActivity.getApplication()).resetAll();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);

				mActivity.dismissDialog(DIALOG_WIPE_PROGRESS);

				// then send them on a one-way trip to the login screen
				Intent intent = new Intent(mActivity, LoginActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mActivity.startActivity(intent);
				mActivity.finish();
			}

			@Override
			protected void onCancelled() {
				super.onCancelled();

				// FIXME: we should probably indicate that the task is cancelled, but this is probably fine for now
				mActivity.dismissDialog(DIALOG_WIPE_PROGRESS);
			}
		};

		wipeTask.execute();
	}
}