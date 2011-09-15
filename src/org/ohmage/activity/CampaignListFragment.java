package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.db.DbContract.Campaign;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ListView;

public class CampaignListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	
	SimpleCursorAdapter mAdapter;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		setEmptyText("No campaigns here!");
		
		mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.campaign_list_item, null,
                new String[] { Campaign.NAME, Campaign.URN },
                new int[] { R.id.name_text, R.id.urn_text }, 0);
		setListAdapter(mAdapter);
		
		getLoaderManager().initLoader(0, null, this);		
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		
		Uri baseUri = Campaign.CONTENT_URI;
		
		String select = Campaign.STATUS + " = " + Campaign.STATUS_REMOTE;
		return new CursorLoader(getActivity(), baseUri, new String [] {Campaign._ID, Campaign.URN, Campaign.NAME, Campaign.STATUS, Campaign.ICON}, select, null, Campaign.NAME);
		
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		
		 mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

		mAdapter.swapCursor(null);
	}

}
