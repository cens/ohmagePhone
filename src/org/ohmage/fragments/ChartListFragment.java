package org.ohmage.fragments;

import org.achartengine.chart.XYChart;
import org.ohmage.NIHConfig;
import org.ohmage.NIHConfig.ExtraPromptData;
import org.ohmage.R;
import org.ohmage.adapters.ChartListAdapter;
import org.ohmage.adapters.SimpleChartListAdapter.ChartItem;
import org.ohmage.charts.HistogramBase.CleanRenderer;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.PromptResponse;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * <p>The {@link ChartListFragment} shows a list of charts</p>
 * 
 * @author cketcham
 *
 */
public class ChartListFragment extends ListFragment implements LoaderCallbacks<Cursor>{

	private static final String TAG = "ChartListFragment";

	private static final String PROMPTS = "prompts";

	private static final int MAX_POINTS = 30;

	private static final int UNKNOWN_TYPE = -1;
	private static final int HISTOGRAM_TYPE = 0;
	private static final int BUBBLE_TYPE = 1;

	private ChartListAdapter mAdapter;

	private String[] mPrompts;

	private String[] mSQLPrompts;

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
			mSQLPrompts = new String[mPrompts.length];
			for(int i=0;i<mPrompts.length;i++) {
				mSQLPrompts[i] = NIHConfig.getExtraPromptData(mPrompts[i]).SQL;
			}
		}

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String campaign = Campaign.getSingleCampaign(getActivity());
		if(campaign == null) {
			getActivity().finish();
			return null;
		}
		return new CursorLoader(getActivity(), PromptResponses.getPromptsByCampaign(campaign, mSQLPrompts),
				new String[] { Responses.RESPONSE_TIME, PromptResponses.PROMPT_ID, PromptResponses.PROMPT_RESPONSE_VALUE, PromptResponses.PROMPT_RESPONSE_EXTRA_VALUE },
				null, null, Responses.RESPONSE_TIME + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		data.moveToPosition(-1);
		mAdapter.clear();

		HashMap<String,double[]> histogramData = new HashMap<String,double[]>();
		HashMap<String,LinkedList<int[]>> bubblePrompts = new HashMap<String,LinkedList<int[]>>();
		for(String prompt : mPrompts) {
			if(getChartType(prompt) == HISTOGRAM_TYPE) {
				double[] tmp = new double[MAX_POINTS];
				Arrays.fill(tmp, Double.MIN_VALUE);
				histogramData.put(prompt, tmp);
			} else if(getChartType(prompt) == BUBBLE_TYPE) {
				bubblePrompts.put(prompt, new LinkedList<int[]>());
			}
		}

		HistogramRenderer exerciseRenderer = new HistogramRenderer(getActivity());
		exerciseRenderer.setYLabels(0);

		String promptId = null;

		for(int i=0;i<MAX_POINTS;i++) {

			for(String prompt : mPrompts) {
				if(getChartType(prompt) == BUBBLE_TYPE) {
					ExtraPromptData extraData = NIHConfig.getExtraPromptData(prompt);
					int[] tmp = new int[extraData.getRange()];
					Arrays.fill(tmp, 0);
					bubblePrompts.get(prompt).add(tmp);
				}
			}

			while(data.moveToNext() && DateUtils.isToday(data.getLong(0) + DateUtils.DAY_IN_MILLIS * i)) {
				promptId = data.getString(1);

				Integer dataPoint = PromptResponse.getIntegerPoint(data);
				if(dataPoint != null) {
					String prompt = NIHConfig.getPrompt(promptId);
					if(getChartType(prompt) == HISTOGRAM_TYPE && dataPoint > histogramData.get(prompt)[i]) {
						histogramData.get(prompt)[i] = dataPoint;
					} else if(getChartType(prompt) == BUBBLE_TYPE) {
						bubblePrompts.get(prompt).getLast()[dataPoint]++;
					}
				}
			}

			data.moveToPrevious();
		}

		for(String prompt : mPrompts)
			mAdapter.add(getChartItem(prompt, histogramData, bubblePrompts));

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

	private ChartItem<? extends XYChart> getChartItem(String promptId,
			HashMap<String, double[]> promptData,
			HashMap<String, LinkedList<int[]>> bubblePrompts) {
		ExtraPromptData extraData = NIHConfig.getExtraPromptData(promptId);
		if(NIHConfig.Prompt.DID_EXERCISE_ID.equals(promptId)) {
			HistogramRenderer r = new HistogramRenderer(getActivity());
			r.setYLabels(0);
			return extraData.toHistogramChartItem(promptData.get(promptId), r);
		} else if(NIHConfig.Prompt.TIME_TO_YOURSELF_ID.equals(promptId)) {
			HistogramRenderer r = new HistogramRenderer(getActivity());
			r.addYTextLabel(1, "< .5");
			r.addYTextLabel(2, "< 1");
			r.addYTextLabel(3, "> 1");
			r.addYTextLabel(4, "> 2");
			int[] margins = r.getMargins();
			margins[1] = 28;
			r.setMargins(margins);
			r.setShowLabels(true);
			return extraData.toHistogramChartItem(promptData.get(promptId), r);
		} else if(NIHConfig.Prompt.FOOD_QUALITY_ID.equals(promptId)) {
			CleanRenderer r = new CleanRenderer();
			r.addYTextLabel(-1, "");
			r.addYTextLabel(0, "Low");
			r.addYTextLabel(1, "Med");
			r.addYTextLabel(2, "High");
			int[] margins = r.getMargins();
			margins[1] = 37;
			r.setMargins(margins);
			r.setShowLabels(true);
			r.setShowGrid(true);
			return extraData.toBubbleChartItem(bubblePrompts.get(promptId), r);
		} else if(NIHConfig.Prompt.FOOD_QUANTITY_ID.equals(promptId)) {
			CleanRenderer r = new CleanRenderer();
			r.addYTextLabel(-1, "");
			r.addYTextLabel(0, "Small");
			r.addYTextLabel(1, "Healthy");
			r.addYTextLabel(2, "Large");
			int[] margins = r.getMargins();
			margins[1] = 56;
			r.setMargins(margins);
			r.setShowLabels(true);
			r.setShowGrid(true);
			return extraData.toBubbleChartItem(bubblePrompts.get(promptId), r);
		} else if(NIHConfig.Prompt.HOW_STRESSED_ID.equals(promptId)) {
			CleanRenderer r = new CleanRenderer();
			r.setShowGrid(true);
			r.setYLabels(6);
			r.addYTextLabel(-1, "");
			return extraData.toBubbleChartItem(bubblePrompts.get(promptId), r);
		}
		return null;
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
}