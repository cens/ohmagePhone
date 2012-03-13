package org.ohmage.charts;

import org.achartengine.chart.TimeChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.ohmage.R;
import org.ohmage.Utilities;

import android.content.Context;

public class SparkLine extends TimeChart {

	public SparkLine(Context context, double[] values, int color) {
		this(context, values);
		getRenderer().getSeriesRendererAt(0).setColor(Utilities.darkenColor(color));
		((XYSeriesRenderer)getRenderer().getSeriesRendererAt(0)).setFillBelowLineColor(color);
	}

	public SparkLine(Context context, double[] values) {
		super(buildDataSet(values), new SparkLineRenderer(context));
	}

	private static XYMultipleSeriesDataset buildDataSet(double[] values) {
		XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
		XYSeries series = new XYSeries("");
		for (int i = 0; i < values.length; i++)
			series.add(i, values[i]);
		dataSet.addSeries(series);
		return dataSet;
	}

	public static class SparkLineRenderer extends XYMultipleSeriesRenderer {
		public SparkLineRenderer(Context context) {
			final XYSeriesRenderer renderer = new XYSeriesRenderer();
			renderer.setColor(context.getResources().getColor(R.color.powderkegblue));
			renderer.setFillBelowLine(true);
			renderer.setFillBelowLineColor(context.getResources().getColor(R.color.highlight));
			renderer.setLineWidth(2.0f);
			addSeriesRenderer(renderer);
			setShowAxes(false);
			setShowLabels(false);
			setShowLegend(false);
			setShowGrid(false);
			setShowLegend(false);
			setMargins(new int[] {
					0, 0, 0, 0
			});
			setPanEnabled(false, false);
			setZoomEnabled(false, false);
		}
	}
}
