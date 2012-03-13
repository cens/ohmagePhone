package org.ohmage.adapters;

import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.adapters.SparklineAdapter.SparkLineChartItem;
import org.ohmage.charts.SparkLine;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class SparklineAdapter extends SimpleChartListAdapter<SparkLineChartItem> {

	public static class SparkLineChartItem extends SimpleChartListAdapter.ChartItem<SparkLine>{

		public SparkLineChartItem(String title, double[] data, int color, Integer min, Integer max) {
			super(title, data, color, min, max);
		}

		@Override
		public int getColor(Context context) {
			return Utilities.darkenColor(super.getColor(context));
		}

		@Override
		public SparkLine makeChart(Context context) {
			return new SparkLine(context, data, super.getColor(context));
		}
	}

	public SparklineAdapter(Context context) {
		super(context, R.layout.feedback_chart, R.id.chart_title, new ArrayList<SparkLineChartItem>());
	}

	public SparklineAdapter(Context context, List<SparkLineChartItem> objects) {
		super(context, R.layout.feedback_chart, R.id.chart_title, objects);
	}
}