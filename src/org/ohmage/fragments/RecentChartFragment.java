
package org.ohmage.fragments;

import org.ohmage.NIHConfig;
import org.ohmage.R;
import org.ohmage.activity.ChartFeedbackActivity;
import org.ohmage.adapters.SparklineAdapter;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.LinkedList;

public class RecentChartFragment extends PromptFeedbackListFragment {

	private SparklineAdapter mAdapter;

	/**
	 * Creates a new instance of the recent chart fragment
	 * @return the chart fragment
	 */
	public static RecentChartFragment newInstance() {
		return new RecentChartFragment();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getActivity().getString(R.string.charts_no_data));

		mAdapter = new SparklineAdapter(getActivity());
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		startCampaignRead();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView lv = (ListView) view.findViewById(android.R.id.list);

		TextView header = new TextView(getActivity());
		header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		header.setText("*Last 30 responses shown");
		int padding = getResources().getDimensionPixelSize(R.dimen.gutter);
		header.setPadding(padding, padding, padding, padding);
		lv.addHeaderView(header, null, false);

		LinearLayout moreLayout = new LinearLayout(getActivity());
		Button moreButton = (Button) inflater.inflate(R.layout.button_charts_more, moreLayout, false);
		moreButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ChartFeedbackActivity.class));
			}
		});
		moreLayout.addView(moreButton);
		lv.addFooterView(moreLayout, null, false);

		lv.setDividerHeight(0);
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

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.clear();
	}
}