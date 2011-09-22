package org.ohmage.activity;

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
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class CampaignListFragment extends ListFragment implements SubActionClickListener, LoaderCallbacks<Cursor> {
	
	private static final String TAG = "CampaignListFragment";
	
	public static final int MODE_MY_CAMPAIGNS = 0;
	public static final int MODE_ADD_CAMPAIGNS = 1;
	
	private int mMode = MODE_MY_CAMPAIGNS;
	
	private CursorAdapter mAdapter;
	private OnCampaignActionListener mListener;
	
	public interface OnCampaignActionListener {
        public void onCampaignActionView(String campaignUrn);
        public void onCampaignActionDownload(String campaignUrn);
        public void onCampaignActionSurveys(String campaignUrn);
        public void onCampaignActionError(String campaignUrn);
    }

//	public void setMode(int mode) {
//		if (mode == MODE_MY_CAMPAIGNS && mode == MODE_ADD_CAMPAIGNS) {
//			this.mMode = mode;
//		} else {
//			Log.e(TAG, "Invalid mode specified. Defaulting to MODE_MY_CAMPAIGNS");
//			this.mMode = MODE_MY_CAMPAIGNS;
//		}
//	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		super.onActivityCreated(savedInstanceState);
		
		if (getActivity().getComponentName().getClassName().equals(CampaignAddActivity.class.getName())) {
			mMode = MODE_ADD_CAMPAIGNS;
			setEmptyText("Loading campaigns...");
		} else {
			mMode = MODE_MY_CAMPAIGNS;
			setEmptyText("You are not participating in any campaigns. Hit the + icon on the top right to view and download avaialable campaigns.");
		}
		
		
		
		mAdapter = new CampaignListCursorAdapter(getActivity(), null, this, 0);
		setListAdapter(mAdapter);
		
		// Start out with a progress indicator.
        setListShown(false);
		
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnCampaignActionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCampaignActionListener");
        }
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		
		Cursor c = (Cursor) getListAdapter().getItem(position);
		mListener.onCampaignActionView(c.getString(c.getColumnIndex(Campaign.URN)));
	}
	
	@Override
	public void onSubActionClicked(Uri uri) {
		Cursor cursor = null;
		
		try {
			cursor = getActivity().getContentResolver().query(uri, new String [] {Campaign._ID, Campaign.URN, Campaign.STATUS}, null, null, null);
			
			if (cursor.getCount() == 1) {
				cursor.moveToFirst();
				String campaignUrn = cursor.getString(cursor.getColumnIndexOrThrow(Campaign.URN));
				int status = cursor.getInt(cursor.getColumnIndexOrThrow(Campaign.STATUS));
				
				switch (status) {
				case Campaign.STATUS_REMOTE:
					mListener.onCampaignActionDownload(campaignUrn);
					break;
					
				case Campaign.STATUS_READY:
					mListener.onCampaignActionSurveys(campaignUrn);
					break;
					
				case Campaign.STATUS_STOPPED:
				case Campaign.STATUS_OUT_OF_DATE:
				case Campaign.STATUS_INVALID_USER_ROLE:
				case Campaign.STATUS_DELETED:
				case Campaign.STATUS_VAGUE:
					mListener.onCampaignActionError(campaignUrn);
					break;
					
				case Campaign.STATUS_DOWNLOADING:
					//do nothing while downloading
					break;
					
				default:
					//campaign is in some unknown state!
					break;
				}
			} else {
				Log.e(TAG, "onSubActionClicked: more than one campaign read from content provider!");
			}
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		
		Uri baseUri = Campaign.CONTENT_URI;

		String select; 
		
		if (mMode == MODE_ADD_CAMPAIGNS) {
			select = Campaign.STATUS + " = " + Campaign.STATUS_REMOTE + " OR " + Campaign.STATUS + " = " + Campaign.STATUS_DOWNLOADING;
		} else {
			select = Campaign.STATUS + " != " + Campaign.STATUS_REMOTE + " AND " + Campaign.STATUS + " != " + Campaign.STATUS_DOWNLOADING;
		}
		
		return new CursorLoader(getActivity(), baseUri, new String [] {Campaign._ID, Campaign.URN, Campaign.NAME, Campaign.STATUS, Campaign.ICON}, select, null, Campaign.NAME);
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
        
        setEmptyText("No campaigns were returned by the server.");
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
}
