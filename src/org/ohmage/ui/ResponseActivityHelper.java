package org.ohmage.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.ohmage.mobilizingcs.R;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Response;
import org.ohmage.service.UploadService;

/**
 * A helper for activities which have responses. This helper provides access to a dialog which
 * can be shown for the upload status of the response. It is invoked by {@link Activity#showDialog(int, Bundle)}
 * with the id of the status and the bundle containing {@link #KEY_URI} with the uri to the response. The
 * parent activity should make sure to call {@link #onCreateDialog(int, Bundle)} on this helper in their own
 * {@link Activity#onCreateDialog(int, Bundle)}
 * 
 * @author Cameron Ketcham
 *
 */
public class ResponseActivityHelper {

	public static final String KEY_URI = "key_uri";
	private final Context mContext;

	private Uri responseUriForDialogs;

	public ResponseActivityHelper(Context context) {
		mContext = context;
	}

	public void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		responseUriForDialogs = (Uri) args.get(KEY_URI);
	}

	public Dialog onCreateDialog(int id, Bundle args) {

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

		int message = R.string.upload_queue_response_error;

		switch (id) {
			case Response.STATUS_ERROR_AUTHENTICATION:
				message = R.string.upload_queue_auth_error;
				break;
			case Response.STATUS_ERROR_CAMPAIGN_NO_EXIST:
				message = R.string.upload_queue_campaign_no_exist;
				break;
			case Response.STATUS_ERROR_CAMPAIGN_OUT_OF_DATE:
				message = R.string.upload_queue_campaign_out_of_date;
				break;
			case Response.STATUS_ERROR_CAMPAIGN_STOPPED:
				message = R.string.upload_queue_campaign_stopped;
				break;
			case Response.STATUS_ERROR_INVALID_USER_ROLE:
				message = R.string.upload_queue_invalid_user_role;
				break;
			case Response.STATUS_ERROR_HTTP:
				message = R.string.upload_queue_network_error;
				break;
			case Response.STATUS_WAITING_FOR_LOCATION:
				builder.setMessage(R.string.upload_queue_response_waiting_for_gps)
				.setCancelable(true)
				.setPositiveButton(R.string.upload, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						queueForUpload(responseUriForDialogs);
					}
				}).setNegativeButton(R.string.wait, null);

				return builder.create();
		}

		builder.setMessage(message)
		.setCancelable(true)
		.setPositiveButton(R.string.retry_now, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				queueForUpload(responseUriForDialogs);
			}
		}).setNeutralButton(R.string.retry_later, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				ContentResolver cr = mContext.getContentResolver();
				ContentValues cv = new ContentValues();
				cv.put(Responses.RESPONSE_STATUS, Response.STATUS_STANDBY);
				cr.update(responseUriForDialogs, cv, null, null);
			}
		}).setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				ContentResolver cr = mContext.getContentResolver();
				cr.delete(responseUriForDialogs, null, null);
			}
		});

		return builder.create();
	}

	public void queueForUpload(Uri responseUri) {
		ContentResolver cr = mContext.getContentResolver();
		ContentValues cv = new ContentValues();
		cv.put(Responses.RESPONSE_STATUS, Response.STATUS_QUEUED);
		cr.update(responseUri, cv, null, null);

		Intent intent = new Intent(mContext, UploadService.class);
		intent.setData(responseUri);
		WakefulIntentService.sendWakefulWork(mContext, intent);
	}


}
