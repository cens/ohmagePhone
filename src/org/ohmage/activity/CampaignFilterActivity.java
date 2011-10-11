package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.controls.FilterControl;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import android.database.Cursor;
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

	/**
	 * Filters the response list by the given campaign uri
	 */
	public static final String EXTRA_CAMPAIGN_URN = "extra_campaign_urn";

	protected FilterControl mCampaignFilter;
	protected String mDefaultCampaign;

	@Override
	public void onContentChanged() {
		super.onContentChanged();

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

		mDefaultCampaign = getIntent().getStringExtra(EXTRA_CAMPAIGN_URN);

		getSupportLoaderManager().initLoader(CAMPAIGN_LOADER, null, this);
	}
	
	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		initLoading();
	}

	protected void initLoading() {
		onCampaignFilterChanged(mDefaultCampaign);
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
		// Now that the campaigns loaded, we can show the filters
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
}
