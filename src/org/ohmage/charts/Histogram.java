
package org.ohmage.charts;

import org.achartengine.chart.BarChart;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.ohmage.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * This is a histogram which can show days on the x-axis and a value on the y-axis.
 * @author cketcham
 *
 */
public class Histogram extends BarChart {

	transient private SimpleDateFormat mDateFormat;

	/**
	 * Construct a new Histogram object with the given values.
	 * 
	 * @param context
	 * @param renderer A renderer can be specified
	 * @param values must be an array and have an entry for each day. The last entry
	 * in the array is the value for 'today'. The second to last entry should be
	 * 'yesterday' etc.
	 */
	public Histogram(Context context, HistogramRenderer renderer,  double[] values) {
		super(buildDataSet(values), (renderer != null ? renderer : new HistogramRenderer(context)), BarChart.Type.DEFAULT);
		fitData();
		setDateFormat("MMM d");
	}

	public Histogram(Context context, double[] values) {
		this(context, null, values);
	}

	public void setDateFormat(String format) {
		mDateFormat = new SimpleDateFormat(format);
	}

	public void fitData() {
		fitData(0);
	}

	/**
	 * Sets the x axis range so all the values fit the way I expect
	 * 
	 * @param index
	 */
	public void fitData(int index) {
		XYSeries series = getDataset().getSeriesAt(index);
		double barLength = (series.getMaxX() - series.getMinX()) / series.getItemCount();
		getRenderer().setXAxisMax(series.getMaxX() + barLength / 2);
		getRenderer().setXAxisMin(series.getMinX() - barLength / 2);
	}

	/**
	 * This has the same functionality as the super class, except it calls getDateLabel
	 * instead of just getLabel which will format the label as a date
	 * {@inheritDoc}
	 */
	@Override
	protected void drawXLabels(List<Double> xLabels, Double[] xTextLabelLocations, Canvas canvas,
			Paint paint, int left, int top, int bottom, double xPixelsPerUnit, double minX,
			double maxX) {
		int length = xLabels.size();
		boolean showLabels = mRenderer.isShowLabels();
		boolean showGrid = mRenderer.isShowGrid();
		for (int i = 0; i < length; i++) {
			double label = xLabels.get(i);
			float xLabel = (float) (left + xPixelsPerUnit * (label - minX));
			if (showLabels) {
				paint.setColor(mRenderer.getLabelsColor());
				canvas.drawLine(xLabel, bottom, xLabel, bottom + mRenderer.getLabelsTextSize() / 3,
						paint);
				drawText(canvas, getDateLabel(label), xLabel,
						bottom + mRenderer.getLabelsTextSize() * 4 / 3,
						paint, mRenderer.getXLabelsAngle());
			}
			if (showGrid) {
				paint.setColor(mRenderer.getGridColor());
				canvas.drawLine(xLabel, bottom, xLabel, top, paint);
			}
		}
		drawXTextLabels(xTextLabelLocations, canvas, paint, showLabels, left, top, bottom,
				xPixelsPerUnit, minX, maxX);
	}

	/**
	 * Returns a formatted string for the given time
	 * 
	 * @param label
	 * @return
	 */
	protected String getDateLabel(double label) {
		if (mDateFormat != null)
			return mDateFormat.format(new Date().getTime() + DateUtils.DAY_IN_MILLIS * label);
		return super.getLabel(label);
	}

	/**
	 * Builds a dataset with dates as the x value and the value as the y value.
	 * It expects exactly one number for each day. values[0] will be interpreted
	 * as today. values[N] will be interpreted as N days ago.
	 * 
	 * @param values
	 * @return
	 */
	private static XYMultipleSeriesDataset buildDataSet(double[] values) {
		XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
		XYSeries series = new XYSeries("");
		for (int i = 0; i < values.length; i++)
			series.add(-i, values[i]);
		dataSet.addSeries(series);
		return dataSet;
	}

	/**
	 * Builds an XY multiple time dataset using the provided values.
	 * 
	 * @param titles the series titles
	 * @param xValues the values for the X axis
	 * @param yValues the values for the Y axis
	 * @return the XY multiple time dataset
	 */
	protected XYMultipleSeriesDataset buildDateDataset(String[] titles, List<Date[]> xValues,
			List<double[]> yValues) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		int length = titles.length;
		for (int i = 0; i < length; i++) {
			TimeSeries series = new TimeSeries(titles[i]);
			Date[] xV = xValues.get(i);
			double[] yV = yValues.get(i);
			int seriesLength = xV.length;
			for (int k = 0; k < seriesLength; k++) {
				series.add(xV[k], yV[k]);
			}
			dataset.addSeries(series);
		}
		return dataset;
	}

	public static class HistogramRenderer extends XYMultipleSeriesRenderer {

		public HistogramRenderer(Context context) {
			final XYSeriesRenderer renderer = new XYSeriesRenderer();
			renderer.setLineWidth(1.0f);
			renderer.setColor(context.getResources().getColor(R.color.light_blue));
			addSeriesRenderer(renderer);
			setMarginsColor(Color.WHITE);
			setPanEnabled(false, false);
			setZoomEnabled(false, false);
			setYLabelsAlign(Align.RIGHT);
			setShowLegend(false);
			setBarSpacing(0.1);
			setAxisTitleTextSize(16);
			setChartTitleTextSize(20);
			setLabelsTextSize(15);
			setLegendTextSize(15);
			setPointSize(5f);
			setMargins(new int[] {
					15, 15, 0, 20
			});
			setLabelsColor(Color.GRAY);
			setXLabels(6);
		}
	}
}
