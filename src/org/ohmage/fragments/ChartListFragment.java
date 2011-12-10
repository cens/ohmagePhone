package org.ohmage.fragments;

import org.ohmage.R;
import org.ohmage.adapters.ChartListAdapter;
import org.ohmage.db.DbContract.Surveys;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;

/**
 * <p>The {@link ChartListFragment} shows a list of charts</p>
 * 
 * @author cketcham
 *
 */
public class ChartListFragment extends ListFragment implements LoaderCallbacks<Cursor>{

	private static final String TAG = "ChartListFragment";

	private CursorAdapter mAdapter;

	public static ChartListFragment newInstance(int position) {
		ChartListFragment f = new ChartListFragment();

		return f;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		super.onActivityCreated(savedInstanceState);

		setEmptyText(getActivity().getString(R.string.feedback_chart_list_empty));

		mAdapter = new ChartListAdapter(getActivity(), null, 0);
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {


		return new CursorLoader(getActivity(), Surveys.CONTENT_URI, null, null, null, null);
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