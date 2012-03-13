package org.ohmage.adapters;

import org.achartengine.chart.BubbleChart;
import org.achartengine.chart.XYChart;
import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.Utilities.DataMapper;
import org.ohmage.adapters.SimpleChartListAdapter.ChartItem;
import org.ohmage.charts.Histogram;
import org.ohmage.charts.HistogramBase.CleanRenderer;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
import org.ohmage.charts.HistogramBubble;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.NumberFormat;
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
		private final int mAverageIndex;
		private final CleanRenderer mRenderer;

		public BubbleChartItem(String title, List<int[]> data, int color, Integer min, Integer max, String yLabel, int averageIndex, CleanRenderer renderer) {
			super(title, new double[0], color, min, max, yLabel);
			mData = data;
			mAverageIndex = averageIndex;
			mRenderer = renderer;
		}

		@Override
		public BubbleChart makeChart(Context context) {
			return new HistogramBubble(context, mRenderer, mData);
		}

		@Override
		public String stats() {
			double[] data = null;
			if(mData != null) {
				data= new double[mData.size()];
				for(int i=0;i<data.length;i++) {
					data[i] = mData.get(i)[mAverageIndex];
				}
			}

			return NumberFormat.getInstance().format(Utilities.stats(data)[3]) + " " + yLabel + " a week on average";
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

		TextView info = (TextView) view.findViewById(R.id.info);
		info.setText(getItem(position).stats());
		return view;
	}
}