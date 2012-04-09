
package org.ohmage.charts;

import org.achartengine.chart.BubbleChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYValueSeries;
import org.ohmage.Utilities;
import org.ohmage.charts.HistogramBase.CleanRenderer;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * This is a bubble histogram which can show days on the x-axis and a value on the y-axis.
 * @author cketcham
 *
 */
public class HistogramBubble extends BubbleChart {

	private static final long MAX_DAYS = 10;

	transient private HistogramBase mBase;

	/**
	 * Construct a new Histogram object with the given values.
	 * 
	 * @param context
	 * @param renderer A renderer can be specified
	 * @param data is a list of FeedbackItems
	 */
	public HistogramBubble(Context context, CleanRenderer renderer, List<FeedbackItem> data) {
		super(buildDataSet(data), (renderer != null ? renderer : new CleanRenderer()));
		mBase = new HistogramBase(this);
		mBase.fitData();
		mBase.setDateFormat("MMM d");
	}

	public HistogramBubble(Context context, List<FeedbackItem> values) {
		this(context, null, values);
	}

	public HistogramBubble(Context context, CleanRenderer renderer, List<FeedbackItem> data, int color) {
		this(context, renderer, data);
		getRenderer().getSeriesRendererAt(0).setColor(context.getResources().getColor(color));
	}

	/**
	 * This has the same functionality as the super class, except it calls getDateLabel
	 * instead of just getLabel which will format the label as a date
	 * {@inheritDoc}
	 */
	@Override
	protected void drawXLabels(List<Double> xLabels, Double[] xTextLabelLocations, Canvas canvas,
			Paint paint, int left, int top, int bottom, double xPixelsPerUnit, double minX,double maxX) {
		mBase.drawXLabels(xLabels, xTextLabelLocations, canvas, paint, left, top, bottom, xPixelsPerUnit, minX, maxX);
	}

	/**
	 * Builds a dataset with dates as the x value and the value as the y value.
	 * It expects exactly one number for each day. values[0] will be interpreted
	 * as today. values[N] will be interpreted as N days ago.
	 * 
	 * @param data
	 * @return
	 */
	private static XYMultipleSeriesDataset buildDataSet(List<FeedbackItem> data) {
		XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
		XYValueSeries series = new XYValueSeries("");

		HashMap<Integer, HashMap<Integer, Integer>> distribution = new HashMap<Integer, HashMap<Integer, Integer>>();

		Calendar calendar = Calendar.getInstance();
		Utilities.clearTime(calendar);
		calendar.add(Calendar.DATE, 1);
		long today = calendar.getTimeInMillis();


		for(FeedbackItem item : data) {
			if(item.time > today - DateUtils.DAY_IN_MILLIS * MAX_DAYS) {
				calendar.setTimeInMillis(item.time);
				Utilities.clearTime(calendar);
				int idx = (int) ((today - calendar.getTimeInMillis()) / DateUtils.DAY_IN_MILLIS) - 1;

				HashMap<Integer,Integer> dayData = distribution.get(-idx);
				if(dayData == null) {
					dayData = new HashMap<Integer,Integer>();
					distribution.put(-idx, dayData);
				}

				Integer i = dayData.get(item.value.intValue());
				if(i == null)
					i = 0;
				dayData.put(item.value.intValue(), i+1);
			}
		}

		for(Integer day : distribution.keySet()) {
			HashMap<Integer, Integer> dayDist = distribution.get(day);
			for(Integer key : dayDist.keySet()) {
				series.add(day, key, dayDist.get(key));
			}
		}

		series.add(0, 0, 0);
		series.add(-MAX_DAYS + 1, 0, 0);

		dataSet.addSeries(series);
		return dataSet;
	}
}
