package org.ohmage.adapters;

import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.ohmage.NIHConfig.ExtraPromptData;
import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.adapters.ComparisonAdapter.ComparisonAdapterItem;
import org.ohmage.charts.OhmageLineChart;
import org.ohmage.charts.OhmageLineChart.OhmageLineRenderer;
import org.ohmage.charts.OhmageLineChart.OhmageLineSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ComparisonAdapter extends SimpleChartListAdapter<ComparisonAdapterItem> {
	private static final String TAG = "ComparisionAdapter";

	public static class ComparisonAdapterItem extends SimpleChartListAdapter.ChartItem<OhmageLineChart>{

		public static final PointStyle POINT_STYLE_CURRENT = PointStyle.RECTANGLE;
		public static final PointStyle POINT_STYLE_LAST_WEEK = PointStyle.RECTANGLE;
		public static final PointStyle POINT_STYLE_BASE_LINE = PointStyle.DASHED_LINE;

		private final Double baseLine;
		private final Double lastWeek;
		private final Double current;
		private final ExtraPromptData mPrompt;
		private OhmageLineChart mChart;

		public ComparisonAdapterItem(FragmentActivity context, ExtraPromptData prompt, Double b, Double l, Double t) {
			super(prompt.shortName, null, -1, 0, 0);
			mPrompt = prompt;
			baseLine = b;
			lastWeek = l;
			current = t;
		}

		protected double calcAverage(ArrayList<Double> values, long days) {
			Double count = 0.0;
			for(Double i : values) {
				count += mPrompt.getMapper().translate(i);
			}
			return Double.valueOf(count) / values.size();
		}

		@Override
		public OhmageLineChart makeChart(Context context) {
			if(mChart != null)
				return mChart;

			XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
			XYMultipleSeriesRenderer renderer = new OhmageLineRenderer();

			int color = context.getResources().getColor(mPrompt.getColor());

			renderer.addSeriesRenderer(0, addSeries(current, "Week So Far", dataset, POINT_STYLE_CURRENT, Utilities.darkenColor(color)));
			renderer.addSeriesRenderer(1, addSeries(lastWeek, "Last Week", dataset, POINT_STYLE_LAST_WEEK, color));
			renderer.addSeriesRenderer(2, addSeries(baseLine, "Base Line", dataset, POINT_STYLE_BASE_LINE, Color.BLACK));

			renderer.setXAxisMin(mPrompt.getMin());
			renderer.setXAxisMax(mPrompt.getMax());

			renderer.addYTextLabel(0, "");

			renderer.setXLabels(mPrompt.getRange());
			for(Double i=mPrompt.getMin(); i<mPrompt.getMax() + 1; i++) {
				renderer.addXTextLabel(i, mPrompt.valueLabels[i.intValue()]);
			}

			mChart = new OhmageLineChart(dataset, renderer);

			return mChart;
		}

		private static SimpleSeriesRenderer addSeries(Double value, String title, XYMultipleSeriesDataset dataset, PointStyle style, int color) {
			XYSeries series = new XYSeries(title);
			if(value != null)
				series.add(value, 0);
			dataset.addSeries(series);
			OhmageLineSeriesRenderer sr = new OhmageLineSeriesRenderer();
			sr.setPointStyle(style);
			sr.setColor(color);
			return sr;
		}

		public static View createLegendView(Context context) {
			XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
			XYMultipleSeriesRenderer renderer = new OhmageLineRenderer();

			renderer.addSeriesRenderer(0, addSeries(0.0, "Week So Far", dataset, POINT_STYLE_CURRENT, Color.DKGRAY));
			renderer.addSeriesRenderer(1, addSeries(0.0, "Last Week", dataset, POINT_STYLE_LAST_WEEK, Color.LTGRAY));
			renderer.addSeriesRenderer(2, addSeries(0.0, "Base Line", dataset, POINT_STYLE_BASE_LINE, Color.BLACK));

			OhmageLineChart chart = new OhmageLineChart(dataset, renderer);
			return chart.getLegendView(context);
		}
	}


	public ComparisonAdapter(Context context) {
		super(context, R.layout.feedback_chart, R.id.chart_title, new ArrayList<ComparisonAdapterItem>());
	}

	public ComparisonAdapter(Context context, List<ComparisonAdapterItem> objects) {
		super(context, R.layout.feedback_chart, R.id.chart_title, objects);
	}
}