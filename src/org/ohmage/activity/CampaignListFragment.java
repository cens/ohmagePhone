package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.db.DbContract.Campaign;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class CampaignListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
	final private String TAG = "CampaignListFragment";
	
	CursorAdapter mAdapter;
	OnCampaignClickListener mListener;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		setEmptyText("Loading campaigns!");
		
//		mAdapter = new SimpleCursorAdapter(getActivity(),
//                R.layout.campaign_list_item, null,
//                new String[] { Campaign.NAME, Campaign.URN },
//                new int[] { R.id.name_text, R.id.urn_text }, 0);

		mAdapter = new CampaignListCursorAdapter(getActivity(), null, 0);
		setListAdapter(mAdapter);
		
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnCampaignClickListener) activity;
        } catch (ClassCastException e) {
        	Log.e(TAG, "error!", e);
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "onListItemClick");
		Cursor c = (Cursor) getListAdapter().getItem(position);
		mListener.onCampaignItemClick(c.getString(c.getColumnIndex(Campaign.URN)));
	}
	
	public interface OnCampaignClickListener {
        public void onCampaignItemClick(String campaignUrn);
        public void onCampaignActionClick(String campaignUrn);
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
