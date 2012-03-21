
package org.ohmage.charts;

import org.achartengine.chart.BarChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.format.DateUtils;

import java.util.List;

/**
 * This is a histogram which can show days on the x-axis and a value on the y-axis.
 * @author cketcham
 *
 */
public class Histogram extends BarChart {

	private static final int MAX_DAYS = 30;

	transient private HistogramBase mBase;

	/**
	 * Construct a new Histogram object with the given values.
	 * 
	 * @param context
	 * @param renderer A renderer can be specified
	 * @param data must be an array and have an entry for each day. The last entry
	 * in the array is the value for 'today'. The second to last entry should be
	 * 'yesterday' etc.
	 */
	public Histogram(Context context, HistogramRenderer renderer, List<FeedbackItem> data) {
		super(buildDataSet(data), (renderer != null ? renderer : new HistogramRenderer(context)), BarChart.Type.DEFAULT);
		mBase = new HistogramBase(this);
		mBase.fitData();
		mBase.setDateFormat("MMM d");
	}

	public Histogram(Context context, List<FeedbackItem> data) {
		this(context, null, data);
	}

	public Histogram(Context context, HistogramRenderer renderer, List<FeedbackItem> data, int color) {
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
		XYSeries series = new XYSeries("");

		for(FeedbackItem item : data) {
			if(item.time > System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * MAX_DAYS)
				series.add((item.time - System.currentTimeMillis()) / DateUtils.DAY_IN_MILLIS, item.value);
		}
		
		dataSet.addSeries(series);
		return dataSet;
	}
}
