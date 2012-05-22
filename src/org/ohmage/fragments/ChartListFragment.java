package org.ohmage.fragments;

import org.achartengine.chart.XYChart;
import org.ohmage.NIHConfig;
import org.ohmage.NIHConfig.ExtraPromptData;
import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.adapters.ChartListAdapter;
import org.ohmage.adapters.SimpleChartListAdapter.ChartItem;
import org.ohmage.charts.HistogramBase.CleanRenderer;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>The {@link ChartListFragment} shows a list of charts</p>
 * 
 * @author cketcham
 *
 */
public class ChartListFragment extends PromptFeedbackListFragment {

	private static final String TAG = "ChartListFragment";

	private static final String PROMPTS = "prompts";

	private ChartListAdapter mAdapter;

	private String[] mPrompts;

	public static ChartListFragment newInstance(String[] prompts) {
		ChartListFragment f = new ChartListFragment();
		Bundle b = new Bundle();
		b.putStringArray(PROMPTS, prompts);
		f.setArguments(b);
		return f;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getActivity().getString(R.string.feedback_chart_list_empty));

		mAdapter = new ChartListAdapter(getActivity());
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		if(getArguments() != null) {
			mPrompts = getArguments().getStringArray(PROMPTS);
		}

		startCampaignRead();
	}

	@Override
	public String[] getPromptList() {
		return mPrompts;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.clear();
	}

	private ChartItem<? extends XYChart> getChartItem(String promptId,
			List<FeedbackItem> promptData) {
		ExtraPromptData extraData = NIHConfig.getExtraPromptData(promptId);
		CleanRenderer r = new CleanRenderer();

		int[] margins = r.getMargins();
		margins[1] = Utilities.dpToPixels(28);
		r.setMargins(margins);
		return extraData.toBubbleChartItem(promptData, r);
	}

	@Override
	public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {
		mAdapter.clear();

		for(String k : NIHConfig.PROMPT_LIST_SQL) {
			String key = NIHConfig.getPrompt(k);
			LinkedList<FeedbackItem> list = feedbackItems.get(key);
			if(list != null && !list.isEmpty()) {
				mAdapter.add(getChartItem(key, list));
			}
		}

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}
}