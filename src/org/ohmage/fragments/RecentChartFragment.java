
package org.ohmage.fragments;

import org.ohmage.NIHConfig;
import org.ohmage.R;
import org.ohmage.activity.ChartFeedbackActivity;
import org.ohmage.adapters.SparklineAdapter;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.LinkedList;

public class RecentChartFragment extends PromptFeedbackFragment<SparklineAdapter> {

	private Button moreButton;


	/**
	 * Creates a new instance of the recent chart fragment
	 * @return the chart fragment
	 */
	public static RecentChartFragment newInstance() {
		return new RecentChartFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		moreButton = (Button) view.findViewById(R.id.recent_chart_more);
		moreButton.setVisibility(View.VISIBLE);
		moreButton.setText(R.string.recent_charts_more);
		moreButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ChartFeedbackActivity.class));
			}
		});
		return view;
	}

	@Override
	public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {
		mAdapter.clear();

		for(String key : NIHConfig.PROMPT_LIST) {
			LinkedList<FeedbackItem> list = feedbackItems.get(NIHConfig.getPrompt(key));
			if(list != null && !list.isEmpty()) {
				mAdapter.add(NIHConfig.getExtraPromptData(key).toSparkLineChartItem(list));
			}
		}

		if(!mAdapter.isEmpty()) {
			moreButton.setVisibility(View.VISIBLE);
			mContainer.removeAllViews();
			TextView header = new TextView(getActivity());
			header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			header.setText("*Last 30 responses shown");
			mContainer.addView(header);
			for(int i=0; i< mAdapter.getCount(); i++) {
				mContainer.addView(mAdapter.getView(i, null, mContainer));
			}
		} else {
			moreButton.setVisibility(View.GONE);
		}
	}


	@Override
	public SparklineAdapter createAdapter() {
		return new SparklineAdapter(getActivity());
	}
}