package org.ohmage.adapters;

import org.achartengine.GraphicalView;
import org.achartengine.chart.AbstractChart;
import org.ohmage.R;
import org.ohmage.adapters.SparklineAdapter.ChartItem;
import org.ohmage.charts.SparkLine;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class SparklineAdapter extends ArrayAdapter<ChartItem> {

	public static class ChartItem {
		public String title;
		public double[] data;
		public int color;
		public int fill;

		public ChartItem(String t, double[] d, int c, int f) {
			title = t;
			data = d;
			color = c;
			fill = f;
		}

		@Override
		public String toString() {
			return title;
		}

		public AbstractChart getChart(Context context) {
			SparkLine chart = new SparkLine(context, data, context.getResources().getColor(color), context.getResources().getColor(fill));
			chart.getRenderer().setInScroll(true);
			return chart;
		}

		public GraphicalView getGraph(Context context) {
			return new GraphicalView(context, getChart(context));
		}
	}

	public SparklineAdapter(Context context) {
		super(context, R.layout.feedback_chart, R.id.chart_title, new ArrayList<ChartItem>());
	}

	public SparklineAdapter(Context context, List<ChartItem> objects) {
		super(context, R.layout.feedback_chart, R.id.chart_title, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);

		ViewGroup chartContainer = (ViewGroup) view.findViewById(R.id.chart);
		chartContainer.removeAllViews();
		chartContainer.addView(getItem(position).getGraph(getContext()));

		return view;
	}
}