package org.ohmage.adapters;

import org.achartengine.GraphicalView;
import org.achartengine.chart.XYChart;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.ohmage.R;
import org.ohmage.Utilities.DataMapper;
import org.ohmage.adapters.SimpleChartListAdapter.ChartItem;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

public abstract class SimpleChartListAdapter<T extends ChartItem<? extends XYChart>> extends ArrayAdapter<T> {

	public static abstract class ChartItem<T extends XYChart> {
		public String title;
		public List<FeedbackItem> data;
		private int color;
		public int colorValue = -1;
		protected final String yLabel;
		private final DataMapper mapper;
		private Integer min;
		private Integer max;

		public ChartItem(String title, List<FeedbackItem> data, int color, Integer min, Integer max) {
			this(title, data, color, min, max, "hours", null);
		}

		public ChartItem(String title, List<FeedbackItem> data, int color, Integer min, Integer max, String yLabel) {
			this(title, data, color, min, max, yLabel, null);
		}

		public ChartItem(String title, List<FeedbackItem> data, int color, Integer min, Integer max, String yLabel, DataMapper mapper) {
			this.title = title;
			this.data = data;
			this.color = color;
			this.min = min;
			this.max = max;
			this.yLabel = yLabel;
			this.mapper = mapper;
		}

		@Override
		public String toString() {
			return title;
		}

		public final T getChart(Context context) {
			T chart = makeChart(context);
			XYMultipleSeriesRenderer renderer = chart.getRenderer();

			// If a color is set make all series that color
			if(color != -1) {
				for(int i=0; i < renderer.getSeriesRendererCount(); i++)
					renderer.getSeriesRendererAt(i).setColor(getColor(context));
			}

			renderer.setInScroll(true);
			renderer.setYAxisMin(min);
			if(max != null)
				renderer.setYAxisMax(max);

			renderer.setPanEnabled(false, false);
			renderer.setZoomEnabled(false, false);
			return chart;
		}

		public abstract T makeChart(Context context);

		public GraphicalView getGraph(Context context) {
			return new GraphicalView(context, getChart(context));
		}

		public int getColor(Context context) {
			if(colorValue == -1)
				colorValue = context.getResources().getColor(color);
			return colorValue;
		}
	}

	public SimpleChartListAdapter(Context context, int feedbackChart, int chartTitle, List<T> arrayList) {
		super(context, feedbackChart, chartTitle, arrayList);
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