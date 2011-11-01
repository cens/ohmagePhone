package org.ohmage.fragments;

import org.ohmage.R;
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
import org.ohmage.fragments.FilterableListFragment;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

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
				Tables.RESPONSES + "." + Responses.RESPONSE_STATUS
		};
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {

		// Set the campaign filter selection
		StringBuilder selection = new StringBuilder();
		if(getCampaignUrn() != null)
			selection.append(DbProvider.Qualified.RESPONSES_CAMPAIGN_URN + "='" + getCampaignUrn() + "' AND ");

		// Set the survey filter selection
		if(getSurveyId() != null)
			selection.append(DbProvider.Qualified.RESPONSES_SURVEY_ID + "='" + getSurveyId() + "' AND ");
		
		// Set the date filter selection
		selection.append(Responses.RESPONSE_TIME + " >= " + getStartBounds() + " AND ");
		selection.append(Responses.RESPONSE_TIME + " <= " + getEndBounds());

		return new CursorLoader(getActivity(), Responses.CONTENT_URI, ResponseQuery.PROJECTION, selection.toString(), null, Responses.RESPONSE_TIME + " DESC");
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
}
