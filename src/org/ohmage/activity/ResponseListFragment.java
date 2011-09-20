package org.ohmage.activity;

import org.ohmage.db.DbContract.Response;
import org.ohmage.db.DbHelper;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

public class ResponseListFragment extends ListFragment implements LoaderCallbacks<Cursor>{
	
	private ResponseListCursorAdapter mAdapter;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);

		Uri queryUri = Response.getResponses();
		
		mAdapter = new ResponseListCursorAdapter(getActivity(), null, 0);
		setListAdapter(mAdapter);
		
		setEmptyText("Loaing response list...");
		setListShown(false);
		
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {

		Uri queryUri = Response.getResponses();

//		String select = null; 
//		
//		if (!mCampaignFilter.equals(FILTER_ALL_CAMPAIGNS)) {
//			select = Survey.CAMPAIGN_URN + "= '" + mCampaignFilter + "'";
//		}
		
		return new CursorLoader(
				getActivity(), 
				queryUri, 
				new String [] {DbHelper.Tables.RESPONSES+"."+Response._ID, Response.DATE}, 
				null, 
				null, 
				Response.DATE);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		mAdapter.swapCursor(c);
		setListShownNoAnimation(true);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
	}
}
