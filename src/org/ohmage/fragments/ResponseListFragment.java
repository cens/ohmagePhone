package org.ohmage.fragments;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.Gravity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.mobilizingcs.R;
import org.ohmage.activity.SubActionClickListener;
import org.ohmage.activity.UploadQueueActivity;
import org.ohmage.adapters.ResponseListCursorAdapter;
import org.ohmage.adapters.UploadingResponseListCursorAdapter;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.db.DbProvider;
import org.ohmage.db.Models.Response;
import org.ohmage.db.utils.SelectionBuilder;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.Log;

public class ResponseListFragment extends FilterableListFragment implements SubActionClickListener {
	
	private static final String TAG = "ResponseListFragment";

	private ResponseListCursorAdapter mAdapter;
	private OnResponseActionListener mListener;

	public interface OnResponseActionListener {
        public void onResponseActionView(Uri responseUri);
        public void onResponseActionUpload(Uri responseUri);
        public void onResponseActionError(Uri responseUri, int status);
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Set the empty text
		setEmptyText(getActivity().getString(R.string.response_list_empty));
		
		// style the empty text, too
		TextView emptyView = (TextView)getListView().getEmptyView();
		emptyView.setGravity(Gravity.LEFT);
		emptyView.setPadding(25, 25, 25, 0);
		

		// We have no menu items to show in action bar.
		setHasOptionsMenu(false);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = createAdapter();
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);
		
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnResponseActionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnResponseActionListener");
        }
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor cursor = (Cursor) getListAdapter().getItem(position);
		String uuid = cursor.getString(cursor.getColumnIndex(Responses.RESPONSE_UUID));
		Analytics.widget(v, null, uuid);
//		startActivity(new Intent(Intent.ACTION_VIEW, DbContract.Response.getResponseByID(id)));
		mListener.onResponseActionView(Responses.buildResponseUri(id));
	}
	
	@Override
	public void onSubActionClicked(Uri uri) {
		Cursor cursor = null;
		
		try {
			cursor = getActivity().getContentResolver().query(uri, new String [] {Tables.RESPONSES + "." + Responses.RESPONSE_STATUS}, null, null, null);
			
			if (cursor.getCount() == 1) {
				cursor.moveToFirst();
				int status = cursor.getInt(cursor.getColumnIndexOrThrow(Responses.RESPONSE_STATUS));
				
				switch (status) {
				case Response.STATUS_STANDBY:
					mListener.onResponseActionUpload(uri);
					break;
				
				case Response.STATUS_UPLOADED:
				case Response.STATUS_DOWNLOADED:
					break;
					
				case Response.STATUS_QUEUED:
				case Response.STATUS_UPLOADING:
					break;

				case Response.STATUS_WAITING_FOR_LOCATION:
				case Response.STATUS_ERROR_AUTHENTICATION:
				case Response.STATUS_ERROR_CAMPAIGN_NO_EXIST:
				case Response.STATUS_ERROR_CAMPAIGN_OUT_OF_DATE:
				case Response.STATUS_ERROR_CAMPAIGN_STOPPED:
				case Response.STATUS_ERROR_INVALID_USER_ROLE:
				case Response.STATUS_ERROR_HTTP:
				case Response.STATUS_ERROR_OTHER:
					mListener.onResponseActionError(uri, status);
					break;
					
				default:
					//campaign is in some unknown state!
					break;
				}
			} else {
				Log.e(TAG, "onSubActionClicked: more than one response read from content provider!");
			}
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public interface ResponseQuery {
		String[] PROJECTION = { 
				Tables.RESPONSES + "." + Responses._ID,
				Campaigns.CAMPAIGN_NAME,
				Surveys.SURVEY_TITLE,
				Responses.RESPONSE_TIME,
				Responses.RESPONSE_TIMEZONE,
				Tables.RESPONSES + "." + Responses.RESPONSE_STATUS,
				Responses.RESPONSE_UUID
		};
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {

		SelectionBuilder selection = new SelectionBuilder();

		// Set the campaign filter selection
		if(getCampaignUrn() != null)
			selection.where(DbProvider.Qualified.RESPONSES_CAMPAIGN_URN + "=?", getCampaignUrn());
		// Set the survey filter selection
		if(getSurveyId() != null)
			selection.where(DbProvider.Qualified.RESPONSES_SURVEY_ID + "=?", getSurveyId());
		
		// Set the date filter selection
		if(!ignoreTimeBounds()) {
			selection.where(Responses.RESPONSE_TIME + " >= " + getStartBounds());
			selection.where(Responses.RESPONSE_TIME + " <= " + getEndBounds());
		}

		return new CursorLoader(getActivity(), Responses.CONTENT_URI, ResponseQuery.PROJECTION, selection.getSelection(), selection.getSelectionArgs(), Responses.RESPONSE_TIME + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
	
	/**
	 * Extending fragments can override this method to change the list adapter used. For example, the {@link UploadQueueActivity}
	 * uses this fragment with the {@link UploadingResponseListCursorAdapter} so it can have the uploading action for the responses
	 * @return the adapter used in this fragment
	 */
	protected ResponseListCursorAdapter createAdapter() {
		return new ResponseListCursorAdapter(getActivity(), null, 0);
	}

	/**
	 * @return true if there should be no time filtering for this type of response fragment
	 */
	protected boolean ignoreTimeBounds() {
		return false;
	}
}
