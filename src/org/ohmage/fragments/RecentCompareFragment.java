package org.ohmage.fragments;

import org.ohmage.NIHConfig;
import org.ohmage.R;
import org.ohmage.adapters.ComparisonAdapter;
import org.ohmage.adapters.ComparisonAdapter.ComparisonAdapterItem;
import org.ohmage.loader.AggregateLoader;
import org.ohmage.loader.CampaignLoader;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.HashMap;

public class RecentCompareFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "RecentCompareFragment";
	private static final int LOADER_CAMPAIGN_URN = 0;
	private static final int LOADER_AGGREGATES_BASELINE = 1;
	private static final int LOADER_AGGREGATES_LASTWEEK = 2;
	private static final int LOADER_AGGREGATES_THISWEEK = 3;

	private static final String EXTRA_CAMPAIGN_URN = "extra_campaign_urn";

	/**
	 * Creates a new instance of the recent chart fragment
	 * @return the chart fragment
	 */
	public static RecentCompareFragment newInstance() {
		return new RecentCompareFragment();
	}

	private ComparisonAdapter mAdapter;
	private HashMap<String, Double> baseline;
	private HashMap<String, Double> lastweek;
	private HashMap<String, Double> thisweek;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getActivity().getString(R.string.charts_no_data));

		mAdapter = new ComparisonAdapter(getActivity());
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		getLoaderManager().initLoader(LOADER_CAMPAIGN_URN, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView lv = (ListView) view.findViewById(android.R.id.list);
		View header = ComparisonAdapterItem.createLegendView(getActivity());
		LinearLayout headerContainer = new LinearLayout(getActivity());
		int padding = getResources().getDimensionPixelSize(R.dimen.gutter);
		headerContainer.setPadding(padding, padding, padding, 0);
		headerContainer.addView(header);

		lv.addHeaderView(headerContainer, null, false);

		lv.setDividerHeight(0);
		return view;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.clear();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch(id) {
			case LOADER_CAMPAIGN_URN:
				return new CampaignLoader(getActivity());
			case LOADER_AGGREGATES_BASELINE:
				return new AggregateLoader(getActivity(), args.getString(EXTRA_CAMPAIGN_URN), AggregateLoader.BASELINE);
			case LOADER_AGGREGATES_LASTWEEK:
				return new AggregateLoader(getActivity(), args.getString(EXTRA_CAMPAIGN_URN), AggregateLoader.LAST_WEEK);
			case LOADER_AGGREGATES_THISWEEK:
				return new AggregateLoader(getActivity(), args.getString(EXTRA_CAMPAIGN_URN), AggregateLoader.THIS_WEEK);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch(loader.getId()) {
			case LOADER_CAMPAIGN_URN:

				Bundle args = new Bundle();
				args.putString(EXTRA_CAMPAIGN_URN, ((CampaignLoader) loader).getCampaignUrn());
				getLoaderManager().initLoader(LOADER_AGGREGATES_BASELINE, args, this).forceLoad();
				getLoaderManager().initLoader(LOADER_AGGREGATES_THISWEEK, args, this).forceLoad();
				getLoaderManager().initLoader(LOADER_AGGREGATES_LASTWEEK, args, this).forceLoad();
				break;

			case LOADER_AGGREGATES_LASTWEEK:
				lastweek = ((AggregateLoader) loader).getAverages();
				break;
			case LOADER_AGGREGATES_BASELINE:
				baseline = ((AggregateLoader) loader).getAverages();
				break;
			case LOADER_AGGREGATES_THISWEEK:
				thisweek = ((AggregateLoader) loader).getAverages();
				break;
		}

		if(lastweek != null && baseline != null && thisweek != null) {
			mAdapter.clear();

			for(String key : NIHConfig.PROMPT_LIST) {
				mAdapter.add(new ComparisonAdapterItem(getActivity(), NIHConfig.getExtraPromptData(key), baseline.get(key), lastweek.get(key), thisweek.get(key)));
			}

			// The list should now be shown.
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}

			lastweek = baseline = thisweek = null;
		}
	}
}