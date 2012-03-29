package org.ohmage.fragments;

import org.ohmage.NIHConfig;
import org.ohmage.adapters.ComparisonAdapter;
import org.ohmage.adapters.ComparisonAdapter.ComparisonAdapterItem;
import org.ohmage.charts.OhmageLineChart;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.view.ViewGroup.LayoutParams;

import java.util.HashMap;
import java.util.LinkedList;

public class RecentCompareFragment extends PromptFeedbackFragment<ComparisonAdapter> {

	private static final String TAG = "RecentCompareFragment";

	/**
	 * Creates a new instance of the recent chart fragment
	 * @return the chart fragment
	 */
	public static RecentCompareFragment newInstance() {
		return new RecentCompareFragment();
	}

	@Override
	public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {

		for(String key : NIHConfig.PROMPT_LIST) {
			LinkedList<FeedbackItem> list = feedbackItems.get(NIHConfig.getPrompt(key));
			if(list != null && !list.isEmpty()) {
				mAdapter.add(new ComparisonAdapterItem(getActivity(), NIHConfig.getExtraPromptData(key), list));
			}
		}

		if(!mAdapter.isEmpty()) {
			mContainer.removeAllViews();

			OhmageLineChart chart = mAdapter.getItem(0).makeChart(getActivity());
			mContainer.addView(chart.getLegendView(getActivity()), LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

			for(int i=0; i< mAdapter.getCount(); i++) {
				mContainer.addView(mAdapter.getView(i, null, mContainer));
			}
		}
	}

	@Override
	public ComparisonAdapter createAdapter() {
		return new ComparisonAdapter(getActivity());
	}
}