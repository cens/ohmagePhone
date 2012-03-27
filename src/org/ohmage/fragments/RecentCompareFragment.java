package org.ohmage.fragments;

import org.ohmage.NIHConfig;
import org.ohmage.R;
import org.ohmage.adapters.ComparisonAdapter;
import org.ohmage.adapters.ComparisonAdapter.ComparisonAdapterItem;
import org.ohmage.charts.OhmageLineChart;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.LinkedList;

public class RecentCompareFragment extends PromptFeedbackFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "RecentCompareFragment";

	/**
	 * Creates a new instance of the recent chart fragment
	 * @return the chart fragment
	 */
	public static RecentCompareFragment newInstance() {
		return new RecentCompareFragment();
	}

	private org.ohmage.adapters.ComparisonAdapter mAdapter;
	private LinearLayout mContainer;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter = new ComparisonAdapter(getActivity());

		startCampaignRead();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.recent_chart_fragment_layout, container, false);
		mContainer = (LinearLayout) view.findViewById(R.id.recent_chart_container);
		return view;
	}

	@Override
	public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {
		for(String key : NIHConfig.PROMPT_LIST) {

			LinkedList<FeedbackItem> list = feedbackItems.get(NIHConfig.getPrompt(key));
			if(list != null && !list.isEmpty()) {
				mAdapter.add(new ComparisonAdapterItem(getActivity(), NIHConfig.getExtraPromptData(key), list));
			}
		}

		mContainer.removeAllViews();

		if(!mAdapter.isEmpty()) {
			// Show the legend
			OhmageLineChart chart = mAdapter.getItem(0).getChart();
			mContainer.addView(chart.getLegendView(getActivity()), LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

			for(int i=0; i< mAdapter.getCount(); i++) {
				mContainer.addView(mAdapter.getView(i, null, mContainer));
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.clear();
	}
}