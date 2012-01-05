package org.ohmage.adapters;

import org.ohmage.R;
import org.ohmage.adapters.SparklineAdapter.SparkLineChartItem;
import org.ohmage.charts.SparkLine;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class SparklineAdapter extends SimpleChartListAdapter<SparkLineChartItem> {

	public static class SparkLineChartItem extends SimpleChartListAdapter.ChartItem<SparkLine>{

		public int fill;

		public SparkLineChartItem(String title, double[] data, int color, int fill, Integer min, Integer max) {
			super(title, data, color, min, max);
			this.fill = fill;
		}

		@Override
		public SparkLine makeChart(Context context) {
			return new SparkLine(context, data, context.getResources().getColor(color), context.getResources().getColor(fill));
		}
	}

	public SparklineAdapter(Context context) {
		super(context, R.layout.feedback_chart, R.id.chart_title, new ArrayList<SparkLineChartItem>());
	}

	public SparklineAdapter(Context context, List<SparkLineChartItem> objects) {
		super(context, R.layout.feedback_chart, R.id.chart_title, objects);
	}
}