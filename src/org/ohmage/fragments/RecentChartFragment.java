
package org.ohmage.fragments;

import org.ohmage.NIHConfig;
import org.ohmage.R;
import org.ohmage.adapters.SparklineAdapter;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.LinkedList;

public class RecentChartFragment extends PromptFeedbackFragment {

	/**
	 * Maximum number of sparklines to show
	 */
	private static final int MAX_CHARTS = 3;

	/**
	 * Creates a new instance of the recent chart fragment
	 * @return the chart fragment
	 */
	public static RecentChartFragment newInstance() {
		return new RecentChartFragment();
	}

	private SparklineAdapter mAdapter;
	private LinearLayout mContainer;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter = new SparklineAdapter(getActivity());

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
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.clear();
	}

	@Override
	public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {

		for(String key : feedbackItems.keySet()) {
			if(mAdapter.getCount() < MAX_CHARTS) {
				LinkedList<FeedbackItem> list = feedbackItems.get(key);
				if(list != null && !list.isEmpty()) {
					mAdapter.add(NIHConfig.getExtraPromptData(key).toSparkLineChartItem(list));
				}
			}
		}

		if(!mAdapter.isEmpty()) {
			mContainer.removeAllViews();
			for(int i=0; i< mAdapter.getCount(); i++) {
				mContainer.addView(mAdapter.getView(i, null, mContainer));
			}
		}
	}
}