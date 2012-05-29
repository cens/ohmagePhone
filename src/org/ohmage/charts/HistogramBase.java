
package org.ohmage.charts;

import org.achartengine.chart.XYChart;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.Utilities;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * This is a histogram helper class which makes it easier to implement charts which
 * can show days on the x-axis and a value on the y-axis.
 * @author cketcham
 *
 */
public class HistogramBase {

	private SimpleDateFormat mDateFormat;
	private final XYChart mChart;

	public HistogramBase(XYChart chart) {
		mChart = chart;
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
		XYSeries series = mChart.getDataset().getSeriesAt(index);
		double barLength = (series.getMaxX() - series.getMinX()) / series.getItemCount();
		mChart.getRenderer().setXAxisMax(series.getMaxX() + barLength / 2);
		mChart.getRenderer().setXAxisMin(series.getMinX() - barLength / 2);
	}

	/**
	 * Returns a formatted string for the given time
	 * 
	 * @param label
	 * @return
	 */
	public String getDateLabel(double label) {
		if (mDateFormat != null)
			return mDateFormat.format(new Date().getTime() + DateUtils.DAY_IN_MILLIS * label);
		return mChart.getLabel(label);
	}

	/**
	 * This has the same functionality as the super class of {@link Histogram}, except it calls getDateLabel
	 * instead of just getLabel which will format the label as a date
	 */
	public void drawXLabels(List<Double> xLabels, Double[] xTextLabelLocations, Canvas canvas,
			Paint paint, int left, int top, int bottom, double xPixelsPerUnit, double minX,
			double maxX) {
		XYMultipleSeriesRenderer renderer = mChart.getRenderer();
		int length = xLabels.size();
		boolean showLabels = renderer.isShowLabels();
		boolean showGrid = renderer.isShowGrid();
		for (int i = 0; i < length; i++) {
			double label = xLabels.get(i);
			float xLabel = (float) (left + xPixelsPerUnit * (label - minX));
			if (showLabels) {
				paint.setColor(renderer.getLabelsColor());
				canvas.drawLine(xLabel, bottom, xLabel, bottom + renderer.getLabelsTextSize() / 3,
						paint);
				mChart.drawText(canvas, getDateLabel(label), xLabel,
						bottom + renderer.getLabelsTextSize() * 4 / 3,
						paint, renderer.getXLabelsAngle());
			}
			if (showGrid) {
				paint.setColor(renderer.getGridColor());
				canvas.drawLine(xLabel, bottom, xLabel, top, paint);
			}
		}
		mChart.drawXTextLabels(xTextLabelLocations, canvas, paint, showLabels, left, top, bottom,
				xPixelsPerUnit, minX, maxX);
	}

	public static class HistogramRenderer extends CleanRenderer {

		public HistogramRenderer(Context context) {
			super();
			getSeriesRendererAt(0).setColor(context.getResources().getColor(R.color.light_blue));
		}
	}

	public static class CleanRenderer extends XYMultipleSeriesRenderer {
		public CleanRenderer() {
			super();
			final XYSeriesRenderer renderer = new XYSeriesRenderer();
			renderer.setLineWidth(1.0f);
			addSeriesRenderer(renderer);

			setPanEnabled(false, false);
			setZoomEnabled(false, false);
			setYLabelsAlign(Align.RIGHT);
			setShowLegend(false);
			setBarSpacing(0.1);
			setAxisTitleTextSize(Utilities.dpToPixels(10));
			setChartTitleTextSize(Utilities.dpToPixels(12));
			setLabelsTextSize(Utilities.dpToPixels(10));
			setLegendTextSize(Utilities.dpToPixels(10));
			setPointSize(Utilities.dpToPixels(10));
			int darkgray = OhmageApplication.getContext().getResources().getColor(R.color.darkgray);
			setLabelsColor(darkgray);
			setXLabels(6);
		}
	}
}
