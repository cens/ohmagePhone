package org.ohmage.fragments;

import org.achartengine.chart.XYChart;
import org.ohmage.R;
import org.ohmage.Utilities.DataMapper;
import org.ohmage.adapters.ChartListAdapter;
import org.ohmage.adapters.ChartListAdapter.BubbleChartItem;
import org.ohmage.adapters.ChartListAdapter.HistogramChartItem;
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

		if(getArguments() != null)
			mPrompts = getArguments().getStringArray(PROMPTS);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), PromptResponses.getPromptsByCampaign(Campaign.getFirstAvaliableCampaign(getActivity()), mPrompts),
				new String[] { Responses.RESPONSE_TIME, PromptResponses.PROMPT_ID, PromptResponses.PROMPT_RESPONSE_VALUE, PromptResponses.PROMPT_RESPONSE_EXTRA_VALUE },
				null, null, Responses.RESPONSE_TIME + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		data.moveToPosition(-1);
		mAdapter.clear();

		HashMap<String,double[]> histogramData = new HashMap<String,double[]>();
		for(String prompt : mPrompts) {
			if(getChartType(prompt) == HISTOGRAM_TYPE) {
				double[] tmp = new double[MAX_POINTS];
				Arrays.fill(tmp, Double.MIN_VALUE);
				histogramData.put(prompt, tmp);
			}
		}

		HashMap<String,LinkedList<int[]>> bubblePrompts = new HashMap<String,LinkedList<int[]>>();
		bubblePrompts.put("foodQuality", new LinkedList<int[]>());
		bubblePrompts.put("foodHowMuch", new LinkedList<int[]>());
		bubblePrompts.put("howStressed", new LinkedList<int[]>());

		HistogramRenderer exerciseRenderer = new HistogramRenderer(getActivity());
		exerciseRenderer.setYLabels(0);

		String promptId = null;

		for(int i=0;i<MAX_POINTS;i++) {

			int[] fq = new int[3];
			Arrays.fill(fq, 0);
			bubblePrompts.get("foodQuality").add(fq);
			int[] fa = new int[3];
			Arrays.fill(fa, 0);
			bubblePrompts.get("foodHowMuch").add(fa);
			int[] hs = new int[6];
			Arrays.fill(hs, 0);
			bubblePrompts.get("howStressed").add(hs);

			while(data.moveToNext() && DateUtils.isToday(data.getLong(0) + DateUtils.DAY_IN_MILLIS * i)) {
				promptId = data.getString(1);

				Integer dataPoint = PromptResponse.getIntegerPoint(data);
				if(dataPoint != null) {
					if(getChartType(promptId) == HISTOGRAM_TYPE && dataPoint > histogramData.get(promptId)[i]) {
						histogramData.get(promptId)[i] = dataPoint;
					} else if("foodQuality".equals(promptId)) {
						fq[dataPoint]++;
					} else if("foodHowMuch".equals(promptId)) {
						fa[dataPoint]++;
					} else if("howStressed".equals(promptId)) {
						hs[dataPoint]++;
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
		if("didYouExercise".equals(promptId)) {
			HistogramRenderer r = new HistogramRenderer(getActivity());
			r.setYLabels(0);
			return new HistogramChartItem("Did Exercise", promptData.get(promptId), R.color.light_green, 0, 1, "times", r);
		} else if("timeForYourself".equals(promptId)) {
			HistogramRenderer r = new HistogramRenderer(getActivity());
			r.addYTextLabel(1, "< .5");
			r.addYTextLabel(2, "< 1");
			r.addYTextLabel(3, "> 1");
			r.addYTextLabel(4, "> 2");
			int[] margins = r.getMargins();
			margins[1] = 28;
			r.setMargins(margins);
			r.setShowLabels(true);
			return new HistogramChartItem("Time For Yourself", promptData.get(promptId), R.color.light_purple, 0, 4, "hours", r, new DataMapper() {

				@Override
				public double translate(double d) {
					switch(Double.valueOf(d).intValue()) {
						case 0: return 0.0;
						case 1: return 0.5;
						case 2: return 1;
						case 3: return 2;
						case 4: return 4;
						default: return d;
					}
				}
			});
		} else if("foodQuality".equals(promptId)) {
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
			return new BubbleChartItem("Food Quality", bubblePrompts.get(promptId), R.color.light_blue, -1, 2, "high quality meals eaten", 2, r);
		} else if("foodHowMuch".equals(promptId)) {
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
			return new BubbleChartItem("Food Quantity", bubblePrompts.get(promptId), R.color.light_blue, -1, 2, "healthy size meals eaten", 1, r);
		} else if("howStressed".equals(promptId)) {
			CleanRenderer r = new CleanRenderer();
			r.setShowGrid(true);
			r.setYLabels(6);
			r.addYTextLabel(-1, "");
			return new BubbleChartItem("Stress Amount", bubblePrompts.get(promptId), R.color.light_red, -1, 5, "times with low stress", 0, r);
		}
		return null;
	}

	private int getChartType(String promptId) {
		if("didYouExercise".equals(promptId) || "timeForYourself".equals(promptId)) {
			return HISTOGRAM_TYPE;
		} else if("foodQuality".equals(promptId) || "foodHowMuch".equals(promptId) || "howStressed".equals(promptId)) {
			return BUBBLE_TYPE;
		} else {
			return UNKNOWN_TYPE;
		}
	}
}