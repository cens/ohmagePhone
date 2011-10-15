package org.ohmage.activity;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.ohmage.R;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.fragments.ResponseListFragment;
import org.ohmage.fragments.ResponseListFragment.OnResponseActionListener;
import org.ohmage.service.UploadService;
import org.ohmage.ui.CampaignFilterActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class UploadQueueActivity extends CampaignFilterActivity implements OnResponseActionListener {
	private static final String TAG = "UploadQueueActivity";

	private Button mUploadAll;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.upload_queue_layout);
		
		mUploadAll = (Button) findViewById(R.id.upload_button);
		
		mUploadAll.setOnClickListener(mUploadAllListener);
	}
	
	@Override
	protected void onCampaignFilterChanged(String filter) {
		if (getUploadingResponseListFragment() != null) {
			getUploadingResponseListFragment().setCampaignUrn(filter);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		super.onLoadFinished(loader, data);
		ensureButtons();
	}

	private void ensureButtons() {
		findViewById(R.id.upload_all_container).setVisibility(View.VISIBLE);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, Campaigns.CONTENT_URI, new String [] { Campaigns.CAMPAIGN_URN, Campaigns.CAMPAIGN_NAME }, 
				Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY, null, Campaigns.CAMPAIGN_NAME);
	}

	private ResponseListFragment getUploadingResponseListFragment() {
		return (UploadingResponseListFragment) getSupportFragmentManager().findFragmentById(R.id.upload_queue_response_list_fragment);
	}

	public static class UploadingResponseListFragment extends ResponseListFragment {

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			// Set the empty text
			setEmptyText(getString(R.string.upload_queue_empty));
		}
		
		@Override
		protected ResponseListCursorAdapter createAdapter() {
			return new UploadingResponseListCursorAdapter(getActivity(), null, this, 0);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			CursorLoader loader = (CursorLoader) super.onCreateLoader(id, args);

			StringBuilder selection = new StringBuilder(loader.getSelection());
			if(selection.length() != 0)
				selection.append(" AND ");
			selection.append(Tables.RESPONSES + "." + Responses.RESPONSE_STATUS + "!=" + Response.STATUS_UPLOADED + " AND " + Tables.RESPONSES + "." + Responses.RESPONSE_STATUS + "!=" + Response.STATUS_DOWNLOADED);
			loader.setSelection(selection.toString());
			return loader;
		}
	}
	
	private final OnClickListener mUploadAllListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			Intent intent = new Intent(UploadQueueActivity.this, UploadService.class);
			intent.setData(Responses.CONTENT_URI);
			WakefulIntentService.sendWakefulWork(UploadQueueActivity.this, intent);
		}
	};
	
	private void queueForUpload(Uri responseUri) {
		ContentResolver cr = getContentResolver();
		ContentValues cv = new ContentValues();
		cv.put(Responses.RESPONSE_STATUS, Response.STATUS_QUEUED);
		cr.update(responseUri, cv, null, null);
		
		Intent intent = new Intent(this, UploadService.class);
		intent.setData(responseUri);
		WakefulIntentService.sendWakefulWork(this, intent);
	}

	@Override
	public void onResponseActionView(Uri responseUri) {
		startActivity(new Intent(Intent.ACTION_VIEW, responseUri));
	}

	@Override
	public void onResponseActionUpload(Uri responseUri) {
		
		queueForUpload(responseUri);
	}

	@Override
	public void onResponseActionError(Uri responseUri, int status) {
//		Toast.makeText(this, "Showing Error Dialog", Toast.LENGTH_SHORT).show();
		Bundle bundle = new Bundle();
		bundle.putString("response_uri", responseUri.toString());
		showDialog(status, bundle);
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		String message = "An error occurred while trying attempting to upload this response.";
		
		
		switch (id) {
		case Response.STATUS_ERROR_AUTHENTICATION:
			message = "Unable to authenticate. Please update your credentials via the Profile screen.";
			break;
		case Response.STATUS_ERROR_CAMPAIGN_NO_EXIST:
			message = "The campaign this survey response belongs to no longer exists.";
			break;
		case Response.STATUS_ERROR_CAMPAIGN_OUT_OF_DATE:
			message = "The campaign this survey response belongs to is out of date.";
			break;
		case Response.STATUS_ERROR_CAMPAIGN_STOPPED:
			message = "The campaign this survey response belongs to is no longer running.";
			break;
		case Response.STATUS_ERROR_INVALID_USER_ROLE:
			message = "Your user role does not permit you to upload responses for this campaign.";
			break;
		case Response.STATUS_ERROR_HTTP:
			message = "Unable to connect to the server.";
			break;
		case Response.STATUS_WAITING_FOR_LOCATION:
			builder.setMessage(R.string.upload_queue_response_waiting_for_gps)
			.setCancelable(true)
			.setPositiveButton("Upload", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

					queueForUpload(responseUriForDialogs);
				}
			}).setNegativeButton("Wait", null);

			return builder.create();
		}
		
		builder.setMessage(message)
				.setCancelable(true)
				.setPositiveButton("Retry Now", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						queueForUpload(responseUriForDialogs);
					}
				}).setNeutralButton("Retry Later", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ContentResolver cr = getContentResolver();
						ContentValues cv = new ContentValues();
						cv.put(Responses.RESPONSE_STATUS, Response.STATUS_STANDBY);
						cr.update(responseUriForDialogs, cv, null, null);
					}
				}).setNegativeButton("Delete", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ContentResolver cr = getContentResolver();
						cr.delete(responseUriForDialogs, null, null);
					}
				});
		
		return builder.create();
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		super.onPrepareDialog(id, dialog, args);
		responseUriForDialogs = Uri.parse(args.getString("response_uri"));
	}
	
	private Uri responseUriForDialogs;
}
