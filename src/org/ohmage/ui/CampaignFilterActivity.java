package org.ohmage.ui;

import org.ohmage.R;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.controls.FilterControl;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.view.View;

/**
 * CampaignFilterActivity can be extended by classes which have a campaign filter
 * @author cketcham
 *
 */
public class CampaignFilterActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {


	protected static final int CAMPAIGN_LOADER = 0;

	protected FilterControl mCampaignFilter;
	protected String mDefaultCampaign;

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		((ActionBarControl)findViewById(R.id.action_bar)).setTitle(getTitle());

		mCampaignFilter = (FilterControl) findViewById(R.id.campaign_filter);
		if(mCampaignFilter == null)
			throw new RuntimeException("Your activity must have a FilterControl with the id campaign_filter");

		mCampaignFilter.setVisibility(View.INVISIBLE);
		mCampaignFilter.setOnChangeListener(new FilterControl.FilterChangeListener() {

			@Override
			public void onFilterChanged(boolean selfChange, String curValue) {
				if(!selfChange)
					onCampaignFilterChanged(curValue);
			}
		});

		mDefaultCampaign = getIntent().getStringExtra(CampaignFilter.EXTRA_CAMPAIGN_URN);
		
		if(mDefaultCampaign == null)
			mCampaignFilter.add(0, new Pair<String, String>("All Campaigns", null));

		getSupportLoaderManager().initLoader(CAMPAIGN_LOADER, null, this);
	}
	
	public String getCampaignUrn() {
		return mCampaignFilter.getValue();
	}

	protected void onCampaignFilterChanged(String filter) {
		// Do whatever when the filter changes
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, Campaigns.CONTENT_URI, new String [] { Campaigns.CAMPAIGN_URN, Campaigns.CAMPAIGN_NAME }, 
				Campaigns.CAMPAIGN_STATUS + "!=" + Campaign.STATUS_REMOTE, null, Campaigns.CAMPAIGN_NAME);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		mCampaignFilter.setVisibility(View.VISIBLE);
		
		// Populate the filter
		mCampaignFilter.populate(data, Campaigns.CAMPAIGN_NAME, Campaigns.CAMPAIGN_URN);
		mCampaignFilter.add(0, new Pair<String,String>("All Campaigns", null));

		if(mDefaultCampaign != null) {
			mCampaignFilter.setValue(mDefaultCampaign);
			mDefaultCampaign = null;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mCampaignFilter.clearAll();
	}
	
    /**
     * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
     */
    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }
}
