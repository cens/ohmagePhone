package org.ohmage.activity;


import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Survey;
import org.ohmage.db.utils.SelectionBuilder;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class SurveyListFragment extends ListFragment implements SubActionClickListener, LoaderCallbacks<Cursor> {

	private static final String TAG = "SurveyListFragment";
	
	public static final String FILTER_ALL_CAMPAIGNS = "all";
	
	private String mCampaignFilter = FILTER_ALL_CAMPAIGNS;
	private boolean mShowPending = false;
	
	private CursorAdapter mAdapter;
	private OnSurveyActionListener mListener;
	
	public interface OnSurveyActionListener {
		public void onSurveyActionView(Uri surveyUri);
        public void onSurveyActionStart(Uri surveyUri);
        public void onSurveyActionUnavailable(Uri surveyUri);
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		super.onActivityCreated(savedInstanceState);
		
		setEmptyText("Loading surveys...");
		
		mAdapter = new SurveyListCursorAdapter(getActivity(), null, this, 0);
		setListAdapter(mAdapter);
		
		// Start out with a progress indicator.
        setListShown(false);
		
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSurveyActionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSurveyActionListener");
        }
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		
		Cursor cursor = (Cursor) getListAdapter().getItem(position);
		
		Uri uri = Campaigns.buildSurveysUri(cursor.getString(cursor.getColumnIndex(Surveys.CAMPAIGN_URN)), cursor.getString(cursor.getColumnIndex(Surveys.SURVEY_ID)));
		mListener.onSurveyActionView(uri);
	}
	
	@Override
	public void onSubActionClicked(Uri uri) {
		
		mListener.onSurveyActionStart(uri);
		
//		mListener.onSurveyActionUnavailable();
	}
	
	public void setCampaignFilter(String filter) {
		mCampaignFilter = filter;
		getLoaderManager().restartLoader(0, null, this);
		Log.i(TAG, "restarted loader for filter: " + filter);
	}
	
	public void setShowPending(boolean showPending) {
		mShowPending = showPending;
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "Creating loader - filter: " + mCampaignFilter);
		Uri baseUri = Surveys.CONTENT_URI;
		
		SelectionBuilder builder = new SelectionBuilder();
		
		if (!mCampaignFilter.equals(FILTER_ALL_CAMPAIGNS)) {
			builder.where(Surveys.CAMPAIGN_URN + "= ?", mCampaignFilter);
		}
		
		if (mShowPending) {
			builder.where(Surveys.SURVEY_STATUS + "=" + Survey.STATUS_TRIGGERED);
		} 
		
		return new CursorLoader(getActivity(), baseUri, null, builder.getSelection(), builder.getSelectionArgs(), Surveys.SURVEY_TITLE);
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
}
