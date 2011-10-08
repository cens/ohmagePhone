package org.ohmage.activity;

import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.db.Models.Response;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class ResponseListFragment extends ListFragment implements SubActionClickListener, LoaderCallbacks<Cursor>{
	
	private static final String TAG = "ResponseListFragment";

	private ResponseListCursorAdapter mAdapter;
	private OnResponseActionListener mListener;

	private String mCampaignUrnFilter;
	private String mSurveyIdFilter;

	private Long mStartDateFilter;
	private Long mEndDateFilter;
	
	public interface OnResponseActionListener {
        public void onResponseActionView(Uri responseUri);
        public void onResponseActionUpload(Uri responseUri);
        public void onResponseActionError(Uri responseUri, int status);
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Set the empty text
		setEmptyText("No responses");

		// We have no menu items to show in action bar.
		setHasOptionsMenu(false);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = createAdapter();
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);
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
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri queryUri;

		// Filter the uri
		if(mCampaignUrnFilter == null) {
			queryUri = Responses.CONTENT_URI;
		} else {
			if(mSurveyIdFilter == null) 
				queryUri = Campaigns.buildResponsesUri(mCampaignUrnFilter);
			else
				queryUri = Campaigns.buildResponsesUri(mCampaignUrnFilter, mSurveyIdFilter);
		}

		// Set the date filter selection
		StringBuilder selection = new StringBuilder();
		if(mStartDateFilter != null)
			selection.append(Responses.RESPONSE_TIME + " > " + mStartDateFilter);
		if(mEndDateFilter != null) {
			if(selection.length() != 0)
				selection.append(" AND ");
			selection.append(Responses.RESPONSE_TIME + " < " + mEndDateFilter);
		}

		return new CursorLoader(getActivity(), queryUri, ResponseQuery.PROJECTION, selection.toString(), null, Responses.RESPONSE_TIME + " DESC");
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
	 * Specify that this list should only show responses for a certain campaign or survey.
	 * If campaignUrn is null, the surveyId will be ignored
	 * If the surveyId is null, it will show responses from any survey from the specified campaign
	 * @param campaignUrn
	 * @param surveyId
	 */
	public void setFilters(String campaignUrn, String surveyId) {
		mCampaignUrnFilter = campaignUrn;
		mSurveyIdFilter = surveyId;
		getLoaderManager().restartLoader(0, null, this);
	}

	/**
	 * Specify date bounds for the responses that will be shown. If either startDateFilter
	 * or endDateFilter is null, that bound will be ignored. No date bound will be set if both are null
	 * @param startDateFilter
	 * @param endDateFilter
	 */
	public void setDateBounds(Long startDateFilter, Long endDateFilter) {
		mStartDateFilter = startDateFilter;
		mEndDateFilter = endDateFilter;
		getLoaderManager().restartLoader(0, null, this);
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
