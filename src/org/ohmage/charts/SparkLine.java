package org.ohmage.charts;

import org.achartengine.chart.TimeChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.content.Context;

import java.util.List;

public class SparkLine extends TimeChart {

	/**
	 * The maximum number of points shown in each {@link SparkLine}
	 */
	private static final int MAX_DATA_POINTS = 30;

	public SparkLine(Context context, List<FeedbackItem> data, int color) {
		this(context, data);
		getRenderer().getSeriesRendererAt(0).setColor(Utilities.darkenColor(color));
		((XYSeriesRenderer)getRenderer().getSeriesRendererAt(0)).setFillBelowLineColor(color);
	}

	public SparkLine(Context context, List<FeedbackItem> data) {
		super(buildDataSet(data), new SparkLineRenderer(context));
	}

	private static XYMultipleSeriesDataset buildDataSet(List<FeedbackItem> data) {
		XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
		XYSeries series = new XYSeries("");

		FeedbackItem point;
		for(int i=0; i< MAX_DATA_POINTS && i < data.size(); i++) {
			point = data.get(i);
			series.add(MAX_DATA_POINTS - i - 1, point.value);
		}
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
			setShowAverageLines(true);
		}
	}
}
