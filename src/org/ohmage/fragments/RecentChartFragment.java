
package org.ohmage.fragments;

import org.ohmage.NIHConfig;
import org.ohmage.adapters.SparklineAdapter;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.util.TypedValue;
import android.widget.TextView;

import java.util.HashMap;
import java.util.LinkedList;

public class RecentChartFragment extends PromptFeedbackFragment<SparklineAdapter> {

	/**
	 * Creates a new instance of the recent chart fragment
	 * @return the chart fragment
	 */
	public static RecentChartFragment newInstance() {
		return new RecentChartFragment();
	}


	@Override
	public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {

		for(String key : NIHConfig.PROMPT_LIST) {
			LinkedList<FeedbackItem> list = feedbackItems.get(NIHConfig.getPrompt(key));
			if(list != null && !list.isEmpty()) {
				mAdapter.add(NIHConfig.getExtraPromptData(key).toSparkLineChartItem(list));
			}
		}

		if(!mAdapter.isEmpty()) {
			mContainer.removeAllViews();
			TextView header = new TextView(getActivity());
			header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			header.setText("*Last 30 responses shown");
			mContainer.addView(header);
			for(int i=0; i< mAdapter.getCount(); i++) {
				mContainer.addView(mAdapter.getView(i, null, mContainer));
			}
		}
	}


	@Override
	public SparklineAdapter createAdapter() {
		return new SparklineAdapter(getActivity());
	}
}