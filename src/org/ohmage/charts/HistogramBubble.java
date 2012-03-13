
package org.ohmage.charts;

import org.achartengine.chart.BubbleChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYValueSeries;
import org.ohmage.charts.HistogramBase.CleanRenderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

/**
 * This is a bubble histogram which can show days on the x-axis and a value on the y-axis.
 * @author cketcham
 *
 */
public class HistogramBubble extends BubbleChart {

	transient private HistogramBase mBase;

	/**
	 * Construct a new Histogram object with the given values.
	 * 
	 * @param context
	 * @param renderer A renderer can be specified
	 * @param values must be an array and have an entry for each day. The last entry
	 * in the array is the value for 'today'. The second to last entry should be
	 * 'yesterday' etc.
	 */
	public HistogramBubble(Context context, CleanRenderer renderer,  List<int[]> values) {
		super(buildDataSet(values), (renderer != null ? renderer : new CleanRenderer()));
		mBase = new HistogramBase(this);
		mBase.fitData();
		mBase.setDateFormat("MMM d");
	}

	public HistogramBubble(Context context, List<int[]> values) {
		this(context, null, values);
	}

	public HistogramBubble(Context context, CleanRenderer renderer, List<int[]> data, int color) {
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
	 * @param values
	 * @return
	 */
	private static XYMultipleSeriesDataset buildDataSet(List<int[]> values) {
		XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
		XYValueSeries series = new XYValueSeries("");

		if(values != null) {
			for(int i=0;i < values.size(); i++) {
				int[] dp = values.get(i);
				for(int j=0;j<dp.length;j++)
					series.add(-i, j, dp[j]);
			}
		}

		dataSet.addSeries(series);
		return dataSet;
	}

}
