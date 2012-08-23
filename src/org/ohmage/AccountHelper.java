package org.ohmage;

import org.ohmage.activity.LoginActivity;
import org.ohmage.activity.UploadQueueActivity;
import org.ohmage.db.Models.Campaign;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.Html;

/**
 * Helper class which makes it easy to show the account management dialogs
 * such as the auth pin dialog, logout dialog, etc.
 * @author cketcham
 *
 */
public class AccountHelper {
	private static final int DIALOG_CLEAR_USER_CONFIRM = 100;
	private static final int DIALOG_CLEAR_USER_CONFIRM_RESPONSES = 101;
	private static final int DIALOG_WIPE_PROGRESS = 102;

	private final Activity mActivity;
	private int responseCount;

	public AccountHelper(Activity activity) {
		mActivity = activity;
	}

	/**
	 * Shows the confirmation dialog for the user to logout
	 */
	public void logout() {
		responseCount = Campaign.localResponseCount(mActivity);

		if(responseCount == 0)
			mActivity.showDialog(DIALOG_CLEAR_USER_CONFIRM);
		else
			mActivity.showDialog(DIALOG_CLEAR_USER_CONFIRM_RESPONSES);
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

	public void onPrepareDialog(int id, Dialog d) {
		if(id == DIALOG_CLEAR_USER_CONFIRM_RESPONSES) {
			((AlertDialog) d).setMessage(Html.fromHtml(mActivity.getResources().getQuantityString(R.plurals.logout_message_responses, responseCount, responseCount)));
		}
	}

	public Dialog onCreateDialog(int id) {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mActivity);
		switch (id) {
			case DIALOG_CLEAR_USER_CONFIRM_RESPONSES:
				dialogBuilder.setNeutralButton(R.string.upload, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mActivity.startActivity(new Intent(mActivity, UploadQueueActivity.class));
					}
				});
				// Fall through to the next case statement. The correct message will be applied in onPrepareDialog
			case DIALOG_CLEAR_USER_CONFIRM:			
				dialogBuilder.setTitle(R.string.confirm)
				.setMessage(R.string.logout_message)
				.setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.logout, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						clearAndGotoLogin();
					}
				});

				break;
			case DIALOG_WIPE_PROGRESS:
				ProgressDialog pDialog = new ProgressDialog(mActivity);
				pDialog.setMessage(mActivity.getString(R.string.logging_out_message));
				pDialog.setCancelable(false);
				return pDialog;
		}

		return dialogBuilder.create();
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
				mActivity.startActivity(getLoginIntent(mActivity));
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

	public static Intent getLoginIntent(Context context) {
		Intent intent = new Intent(context, LoginActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}
}