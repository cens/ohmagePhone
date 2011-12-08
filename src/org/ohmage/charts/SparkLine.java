package org.ohmage.charts;

import org.achartengine.chart.TimeChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.ohmage.R;

import android.content.Context;

public class SparkLine extends TimeChart {

	public SparkLine(Context context, String title, double[] values) {
		super(buildDataSet(values), buildRenderer(context, title));
	}

	private static XYMultipleSeriesDataset buildDataSet(double[] values) {
		XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
		XYSeries series = new XYSeries("");
		for (int i = 0; i < values.length; i++)
			series.add(i, values[i]);
		dataSet.addSeries(series);
		return dataSet;
	}

	private static XYMultipleSeriesRenderer buildRenderer(Context context, String title) {
		XYMultipleSeriesRenderer multipleRenderer = new XYMultipleSeriesRenderer();
		final XYSeriesRenderer renderer = new XYSeriesRenderer();
		renderer.setFillBelowLine(true);
		renderer.setFillBelowLineColor(context.getResources().getColor(R.color.highlight));
		renderer.setLineWidth(2.0f);
		renderer.setColor(context.getResources().getColor(R.color.powderkegblue));
		multipleRenderer.addSeriesRenderer(renderer);
		multipleRenderer.setChartTitle(title);
		multipleRenderer.setShowAxes(false);
		multipleRenderer.setShowLabels(false);
		multipleRenderer.setShowLegend(false);
		multipleRenderer.setShowGrid(false);
		multipleRenderer.setShowLegend(false);
		multipleRenderer.setMargins(new int[] {
				0, 0, 0, 0
		});
		multipleRenderer.setPanEnabled(false, false);
		multipleRenderer.setZoomEnabled(false, false);
		return multipleRenderer;
	}
}
