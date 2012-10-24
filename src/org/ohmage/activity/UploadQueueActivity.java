package org.ohmage.activity;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.systemlog.Analytics;

import org.ohmage.ConfigHelper;
import org.ohmage.R;
import org.ohmage.adapters.ResponseListCursorAdapter;
import org.ohmage.adapters.UploadingResponseListCursorAdapter;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.fragments.ResponseListFragment;
import org.ohmage.fragments.ResponseListFragment.OnResponseActionListener;
import org.ohmage.service.UploadService;
import org.ohmage.ui.CampaignFilterActivity;
import org.ohmage.ui.ResponseActivityHelper;

import android.app.Dialog;
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

	private ResponseActivityHelper mResponseHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.upload_queue_layout);
		
		mResponseHelper = new ResponseActivityHelper(this);

		mUploadAll = (Button) findViewById(R.id.upload_button);
		
		mUploadAll.setOnClickListener(mUploadAllListener);

		// Show the upload button immediately in single campaign mode since we don't query for the campaign
		if(ConfigHelper.isSingleCampaignMode())
			ensureButtons();
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

		@Override
		protected boolean ignoreTimeBounds() {
			return true;
		}
	}
	
	private final OnClickListener mUploadAllListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Analytics.widget(v);
			Intent intent = new Intent(UploadQueueActivity.this, UploadService.class);
			intent.setData(Responses.CONTENT_URI);
			intent.putExtra(UploadService.EXTRA_UPLOAD_SURVEYS, true);
			WakefulIntentService.sendWakefulWork(UploadQueueActivity.this, intent);
		}
	};


	@Override
	public void onResponseActionView(Uri responseUri) {
		startActivity(new Intent(Intent.ACTION_VIEW, responseUri));
	}

	@Override
	public void onResponseActionUpload(Uri responseUri) {
		mResponseHelper.queueForUpload(responseUri);
	}

	@Override
	public void onResponseActionError(Uri responseUri, int status) {
		Bundle bundle = new Bundle();
		bundle.putParcelable(ResponseActivityHelper.KEY_URI, responseUri);
		showDialog(status, bundle);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		mResponseHelper.onPrepareDialog(id, dialog, args);
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		return mResponseHelper.onCreateDialog(id, args);
	}
}
