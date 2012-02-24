package org.ohmage.fragments;

import edu.ucla.cens.systemlog.Analytics;

import org.ohmage.R;
import org.ohmage.activity.SubActionClickListener;
import org.ohmage.adapters.CampaignListCursorAdapter;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class CampaignListFragment extends ListFragment implements SubActionClickListener, LoaderCallbacks<Cursor> {
	
	private static final String TAG = "CampaignListFragment";
	
	public static final int MODE_MY_CAMPAIGNS = 0;
	public static final int MODE_ADD_CAMPAIGNS = 1;

	private static final String EXTRA_MODE = "extra_mode";
	
	private int mMode = MODE_MY_CAMPAIGNS;
	
	private CursorAdapter mAdapter;
	private OnCampaignActionListener mListener;
	
	public interface OnCampaignActionListener {
        public void onCampaignActionView(String campaignUrn);
        public void onCampaignActionDownload(String campaignUrn);
        public void onCampaignActionSurveys(String campaignUrn);
        public void onCampaignActionError(String campaignUrn, int status);
    }

	public static Fragment newInstance(int mode) {
		CampaignListFragment f = new CampaignListFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_MODE, mode);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if(getArguments() != null && getArguments().containsKey(EXTRA_MODE))
			mMode = getArguments().getInt(EXTRA_MODE);

		switch(mMode) {
			case MODE_ADD_CAMPAIGNS:
				setEmptyText(getActivity().getString(R.string.campaign_add_list_empty));
				break;
			case MODE_MY_CAMPAIGNS:
				setEmptyText(getActivity().getString(R.string.campaign_list_empty));
				break;
		}

		// style the empty text, too
		TextView emptyView = (TextView)getListView().getEmptyView();
		emptyView.setGravity(Gravity.LEFT);
		emptyView.setPadding(25, 25, 25, 0);
		
		mAdapter = new CampaignListCursorAdapter(getActivity(), null, this, 0);
		setListAdapter(mAdapter);
		
		// Start out with a progress indicator.
        setListShown(false);
		
		getLoaderManager().initLoader(0, null, this);
	}
	
	// TODO: we may need to override onCreateView here to load up a layout with a customized empty text view, too
	// consult FilterableListFragment for an example

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
		String campaignUrn = c.getString(c.getColumnIndex(Campaigns.CAMPAIGN_URN));
		Analytics.widget(v, null, campaignUrn);
		mListener.onCampaignActionView(campaignUrn);
	}
	
	@Override
	public void onSubActionClicked(Uri uri) {
		Cursor cursor = null;
		
		try {
			cursor = getActivity().getContentResolver().query(uri, new String [] {Campaigns._ID, Campaigns.CAMPAIGN_URN, Campaigns.CAMPAIGN_STATUS}, null, null, null);
			
			if (cursor.getCount() == 1) {
				cursor.moveToFirst();
				String campaignUrn = cursor.getString(cursor.getColumnIndexOrThrow(Campaigns.CAMPAIGN_URN));
				int status = cursor.getInt(cursor.getColumnIndexOrThrow(Campaigns.CAMPAIGN_STATUS));
				
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
				case Campaign.STATUS_NO_EXIST:
				case Campaign.STATUS_VAGUE:
					mListener.onCampaignActionError(campaignUrn, status);
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
		
		Uri baseUri = Campaigns.CONTENT_URI;

		String select; 
		
		if (mMode == MODE_ADD_CAMPAIGNS) {
			select = Campaigns.CAMPAIGN_STATUS + " = " + Campaign.STATUS_REMOTE + " OR " + Campaigns.CAMPAIGN_STATUS + " = " + Campaign.STATUS_DOWNLOADING;
		} else {
			select = Campaigns.CAMPAIGN_STATUS + " != " + Campaign.STATUS_REMOTE + " AND " + Campaigns.CAMPAIGN_STATUS + " != " + Campaign.STATUS_DOWNLOADING;
		}
		
		return new CursorLoader(getActivity(), baseUri, new String [] {Campaigns._ID, Campaigns.CAMPAIGN_URN, Campaigns.CAMPAIGN_NAME, Campaigns.CAMPAIGN_STATUS, Campaigns.CAMPAIGN_ICON}, select, null, Campaigns.CAMPAIGN_NAME);
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
