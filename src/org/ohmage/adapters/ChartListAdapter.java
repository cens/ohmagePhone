package org.ohmage.adapters;

import org.achartengine.chart.BubbleChart;
import org.achartengine.chart.XYChart;
import org.ohmage.R;
import org.ohmage.Utilities.DataMapper;
import org.ohmage.adapters.SimpleChartListAdapter.ChartItem;
import org.ohmage.charts.Histogram;
import org.ohmage.charts.HistogramBase.CleanRenderer;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
import org.ohmage.charts.HistogramBubble;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class ChartListAdapter extends SimpleChartListAdapter<ChartItem<? extends XYChart>> {

	public static class HistogramChartItem extends SimpleChartListAdapter.ChartItem<Histogram>{

		HistogramRenderer mRenderer;

		public HistogramChartItem(String title, double[] data, int color, Integer min, Integer max, String yLabel, HistogramRenderer renderer) {
			super(title, data, color, min, max, yLabel);
			mRenderer = renderer;
		}

		public HistogramChartItem(String title, double[] data, int color, Integer min, Integer max, String yLabel, HistogramRenderer renderer, DataMapper mapper) {
			super(title, data, color, min, max, yLabel, mapper);
			mRenderer = renderer;
		}

		@Override
		public Histogram makeChart(Context context) {
			return new Histogram(context, mRenderer, data);
		}
	}

	public static class BubbleChartItem extends SimpleChartListAdapter.ChartItem<BubbleChart>{

		private final List<int[]> mData;
		private final CleanRenderer mRenderer;

		public BubbleChartItem(String title, List<int[]> data, int color, Integer min, Integer max, String yLabel, int averageIndex, CleanRenderer renderer) {
			super(title, new double[0], color, min, max, yLabel);
			mData = data;
			mRenderer = renderer;
		}

		@Override
		public BubbleChart makeChart(Context context) {
			return new HistogramBubble(context, mRenderer, mData);
		}
	}

	public ChartListAdapter(Context context) {
		super(context, R.layout.ohmage_chart_list_item, R.id.title, new ArrayList<ChartItem<? extends XYChart>>());
	}

	public ChartListAdapter(Context context, List<ChartItem<? extends XYChart>> objects) {
		super(context, R.layout.ohmage_chart_list_item, R.id.title, objects);
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