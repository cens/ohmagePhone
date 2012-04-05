package org.ohmage.fragments;

import org.achartengine.chart.XYChart;
import org.ohmage.NIHConfig;
import org.ohmage.NIHConfig.ExtraPromptData;
import org.ohmage.R;
import org.ohmage.adapters.ChartListAdapter;
import org.ohmage.adapters.SimpleChartListAdapter.ChartItem;
import org.ohmage.charts.HistogramBase.CleanRenderer;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
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

	private static final int UNKNOWN_TYPE = -1;
	private static final int HISTOGRAM_TYPE = 0;
	private static final int BUBBLE_TYPE = 1;

	private static final int MAX_POINTS = 10;

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
		
		if(NIHConfig.Prompt.DID_EXERCISE_ID.equals(promptId)) {
			int[] margins = r.getMargins();
			margins[1] = 28;
			r.setMargins(margins);
		} else if(NIHConfig.Prompt.TIME_TO_YOURSELF_ID.equals(promptId)) {
			HistogramRenderer r2 = new HistogramRenderer(getActivity());
			int[] margins = r2.getMargins();
			margins[1] = 28;
			r2.setMargins(margins);
			return extraData.toHistogramChartItem(promptData, r2);
		} else if(NIHConfig.Prompt.FOOD_QUALITY_ID.equals(promptId)) {
			int[] margins = r.getMargins();
			margins[1] = 37;
			r.setMargins(margins);
		} else if(NIHConfig.Prompt.FOOD_QUANTITY_ID.equals(promptId)) {
			int[] margins = r.getMargins();
			margins[1] = 71;
			r.setMargins(margins);
		} else if(NIHConfig.Prompt.HOW_STRESSED_ID.equals(promptId)) {
			int[] margins = r.getMargins();
			margins[1] = 41;
			r.setMargins(margins);
		}
		return extraData.toBubbleChartItem(promptData, r);
	}

	private int getChartType(String promptId) {
		if(NIHConfig.Prompt.DID_EXERCISE_ID.equals(promptId) || NIHConfig.Prompt.TIME_TO_YOURSELF_ID.equals(promptId)) {
			return HISTOGRAM_TYPE;
		} else if(NIHConfig.Prompt.FOOD_QUALITY_ID.equals(promptId) || NIHConfig.Prompt.FOOD_QUANTITY_ID.equals(promptId) || NIHConfig.Prompt.HOW_STRESSED_ID.equals(promptId)) {
			return BUBBLE_TYPE;
		} else {
			return UNKNOWN_TYPE;
		}
	}

	@Override
	public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {
		mAdapter.clear();

		for(String k : NIHConfig.PROMPT_LIST) {
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