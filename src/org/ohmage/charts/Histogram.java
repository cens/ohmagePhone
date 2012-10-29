
package org.ohmage.charts;

import org.achartengine.chart.BarChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.ohmage.Utilities;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.format.DateUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * This is a histogram which can show days on the x-axis and a value on the y-axis.
 * @author cketcham
 *
 */
public class Histogram extends BarChart {

	private static final int MAX_DAYS = 30;

	transient private HistogramBase mBase;

	private int mDays = MAX_DAYS;

	/**
	 * Construct a new Histogram object with the given values.
	 * 
	 * @param context
	 * @param renderer A renderer can be specified
	 * @param data must be an array and have an entry for each day. The last entry
	 * in the array is the value for 'today'. The second to last entry should be
	 * 'yesterday' etc.
	 * @param maxDays
	 */
	public Histogram(Context context, HistogramRenderer renderer, List<FeedbackItem> data, int maxDays) {
		super(buildDataSet(data, maxDays), (renderer != null ? renderer : new HistogramRenderer(context)), BarChart.Type.DEFAULT);
		mBase = new HistogramBase(this);
		mBase.fitData();
		mBase.setDateFormat("MMM d");
	}

	public Histogram(Context context, HistogramRenderer renderer, List<FeedbackItem> data) {
		this(context, renderer, data, MAX_DAYS);
	}

	public Histogram(Context context, List<FeedbackItem> data) {
		this(context, null, data);
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
	 * It will parse the points and show the maximum for each day
	 * 
	 * @param data
	 * @return dataset
	 */
	private static XYMultipleSeriesDataset buildDataSet(List<FeedbackItem> data, int days) {
		XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
		XYSeries series = new XYSeries("");

		Double[] values = new Double[days];
		Arrays.fill(values, 0.0);

		Calendar calendar = Calendar.getInstance();
		Utilities.clearTime(calendar);
		calendar.add(Calendar.DATE, 1);
		long today = calendar.getTimeInMillis();

		for(FeedbackItem item : data) {
			if(item.time > today - DateUtils.DAY_IN_MILLIS * days) {
				calendar.setTimeInMillis(item.time);
				Utilities.clearTime(calendar);
				int idx = (int) ((today - calendar.getTimeInMillis()) / DateUtils.DAY_IN_MILLIS) - 1;
				values[idx] = Math.max(values[idx], item.value);
			}
		}

		for(int i=0;i<values.length;i++) {
			series.add(-i, values[i]);
		}

		dataSet.addSeries(series);
		return dataSet;
	}

	/**
	 * Sets the number of days that will be shown for this histogram
	 * @param days
	 */
	public void setTimeRange(int days) {
		mDays = days;
	}
}
