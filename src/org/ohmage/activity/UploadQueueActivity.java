package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.activity.ResponseListFragment.OnResponseActionListener;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.service.UploadService;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class UploadQueueActivity extends FragmentActivity implements OnResponseActionListener, LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = "UploadQueueActivity";
	private ContentResolver mCR;
	private FilterControl mCampaignFilter;
	private Button mUploadAll;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.upload_queue_layout);
		
		mUploadAll = (Button) findViewById(R.id.upload_button);
		
		mUploadAll.setOnClickListener(mUploadAllListener);

		// Add the surveys to the list fragment
		final FragmentManager fm = getSupportFragmentManager();

		// instantiate a filter just for fun and populate it from the campaigns list
		// also create a filter that will be populated by the survey list
		mCampaignFilter = (FilterControl) findViewById(R.id.campaign_filter);
		mCampaignFilter.setVisibility(View.GONE);

		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(boolean selfChange, String curValue) {
				if (getUploadingResponseListFragment() != null) {
					getUploadingResponseListFragment().setFilters(curValue, null);
				}
			}
		});

		if (fm.findFragmentById(R.id.root_container) == null) {

			UploadingResponseListFragment list = new UploadingResponseListFragment();

			fm.beginTransaction().add(R.id.root_container, list, "list").commit();
		}

		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, Campaigns.CONTENT_URI, new String [] { Campaigns.CAMPAIGN_URN, Campaigns.CAMPAIGN_NAME }, 
				Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY, null, Campaigns.CAMPAIGN_NAME);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Now that the campaigns loaded, we can show the filters
		mCampaignFilter.setVisibility(View.VISIBLE);

		// Populate the filter
		mCampaignFilter.populate(data, Campaigns.CAMPAIGN_NAME, Campaigns.CAMPAIGN_URN);
		mCampaignFilter.add(0, new Pair<String,String>("All Campaigns", null));
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mCampaignFilter.clearAll();
	}

	private ResponseListFragment getUploadingResponseListFragment() {
		return (UploadingResponseListFragment) getSupportFragmentManager().findFragmentByTag("list");
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
			intent.putExtra("select", Tables.RESPONSES + "." + Responses.RESPONSE_STATUS + "=" + Response.STATUS_STANDBY);
			WakefulIntentService.sendWakefulWork(UploadQueueActivity.this, intent);
		}
	};

	@Override
	public void onResponseActionView(Uri responseUri) {
		startActivity(new Intent(Intent.ACTION_VIEW, responseUri));
	}

	@Override
	public void onResponseActionUpload(Uri responseUri) {
		
//		ContentResolver cr = getContentResolver();
//		ContentValues cv = new ContentValues();
//		cv.put(Tables.RESPONSES + "." + Response.STATUS, Response.STATUS_QUEUED);
//		cr.update(responseUri, cv, null, null);
		
		Intent intent = new Intent(this, UploadService.class);
		intent.setData(responseUri);
		WakefulIntentService.sendWakefulWork(this, intent);
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
		final Uri responseUri = Uri.parse(args.getString("response_uri"));
		
		switch (id) {
		case Response.STATUS_ERROR_AUTHENTICATION:
			message = "An authentication error occurred while trying attempting to upload this response.";
			break;
		case Response.STATUS_ERROR_CAMPAIGN_NO_EXIST:
			message = "The campaign this response belongs to no longer exists.";
			break;
		case Response.STATUS_ERROR_INVALID_USER_ROLE:
			message = "Invalid user role.";
			break;
		}
		
		builder.setMessage(message)
				.setCancelable(true)
				.setPositiveButton("Retry Now", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						Intent intent = new Intent(UploadQueueActivity.this, UploadService.class);
						intent.setData(responseUri);
						WakefulIntentService.sendWakefulWork(UploadQueueActivity.this, intent);
					}
				}).setNeutralButton("Retry Later", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ContentResolver cr = getContentResolver();
						Cursor c = cr.query(responseUri, new String [] {Tables.RESPONSES + "." + Responses._ID}, null, null, null);
						if (c.getCount() == 1) {
							c.moveToFirst();
							long responseId = c.getLong(0);
							ContentValues values = new ContentValues();
							values.put(Responses.RESPONSE_STATUS, Response.STATUS_STANDBY);
							cr.update(Responses.CONTENT_URI, values, Responses._ID + "=" + responseId, null);
						} else {
							Log.e(TAG, "Unexpected number of rows returned for + " + responseUri.toString() + ": " + c.getCount());
						}
					}
				}).setNegativeButton("Delete", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ContentResolver cr = getContentResolver();
						Cursor c = cr.query(responseUri, new String [] {Tables.RESPONSES + "." + Responses._ID}, null, null, null);
						if (c.getCount() == 1) {
							c.moveToFirst();
							long responseId = c.getLong(0);
							cr.delete(Responses.CONTENT_URI, Responses._ID + "=" + responseId, null);
						} else {
							Log.e(TAG, "Unexpected number of rows returned for + " + responseUri.toString() + ": " + c.getCount());
						}
					}
				});
		
		return builder.create();
	}
}
