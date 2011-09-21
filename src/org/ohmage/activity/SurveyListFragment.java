package org.ohmage.activity;


import java.util.List;

import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Survey;

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
	
	public static final int MODE_ALL_SURVEYS = 0;
	public static final int MODE_PENDING_SURVEYS = 1;
	
	public static final String FILTER_ALL_CAMPAIGNS = "all";
	
	private int mMode = MODE_ALL_SURVEYS;
	private String mCampaignFilter = FILTER_ALL_CAMPAIGNS;
	
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
		
		Uri uri = Survey.getSurveyByID(cursor.getString(cursor.getColumnIndex(Survey.CAMPAIGN_URN)), cursor.getString(cursor.getColumnIndex(Survey.SURVEY_ID)));
		mListener.onSurveyActionView(uri);
		
//		if (surveys.size() == 1) {
//			
//		} else {
//			Log.e(TAG, "onListItemClick: more than one campaign read from content provider!");
//		}	
	}
	
	@Override
	public void onSubActionClicked(Uri uri) {
		
		mListener.onSurveyActionStart(uri);
		
//		Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
//		List<Survey> surveys = Survey.fromCursor(cursor);
//		if (surveys.size() == 1) {
//			mListener.onSurveyActionStart(surveys.get(0));
//		} else {
//			Log.e(TAG, "onSubActionClicked: more than one campaign read from content provider!");
//		}
		
//		mListener.onSurveyActionUnavailable();
	}
	
	public void setCampaignFilter(String filter) {
		mCampaignFilter = filter;
		getLoaderManager().restartLoader(0, null, this);
		Log.i(TAG, "restarted loader for filter: " + filter);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "Creating loader - filter: " + mCampaignFilter);
		Uri baseUri = Survey.CONTENT_URI;

		String select = null; 
		
		if (mMode == MODE_PENDING_SURVEYS) {
//			select = Campaign.STATUS + " = " + Campaign.STATUS_REMOTE + " OR " + Campaign.STATUS + " = " + Campaign.STATUS_DOWNLOADING;
		} else {
//			select = Campaign.STATUS + " != " + Campaign.STATUS_REMOTE + " AND " + Campaign.STATUS + " != " + Campaign.STATUS_DOWNLOADING;
		}
		
		if (!mCampaignFilter.equals(FILTER_ALL_CAMPAIGNS)) {
			select = Survey.CAMPAIGN_URN + "= '" + mCampaignFilter + "'";
		}
		
		return new CursorLoader(getActivity(), baseUri, null, select, null, Survey.TITLE);
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
