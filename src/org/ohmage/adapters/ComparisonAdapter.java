package org.ohmage.adapters;

import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.ohmage.NIHConfig.ExtraPromptData;
import org.ohmage.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.adapters.ComparisonAdapter.ComparisonAdapterItem;
import org.ohmage.charts.OhmageLineChart;
import org.ohmage.charts.OhmageLineChart.OhmageLineRenderer;
import org.ohmage.charts.OhmageLineChart.OhmageLineSeriesRenderer;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class ComparisonAdapter extends SimpleChartListAdapter<ComparisonAdapterItem> {
	private static final String TAG = "ComparisionAdapter";

	public static class ComparisonAdapterItem extends SimpleChartListAdapter.ChartItem<OhmageLineChart>{

		public static final PointStyle POINT_STYLE_CURRENT = PointStyle.RECTANGLE;
		public static final PointStyle POINT_STYLE_LAST_WEEK = PointStyle.RECTANGLE;
		public static final PointStyle POINT_STYLE_BASE_LINE = PointStyle.DASHED_LINE;

		private double baseLine;
		private double lastWeek;
		private double current;
		private final ExtraPromptData mPrompt;
		private OhmageLineChart mChart;

		public ComparisonAdapterItem(Context context, ExtraPromptData prompt, LinkedList<FeedbackItem> data) {
			super(prompt.shortName, data, -1, 0, 0);
			mPrompt = prompt;
			setData(context, data);
		}

		private void setData(Context context, LinkedList<FeedbackItem> data) {
			Calendar cal = Calendar.getInstance();
			long now = cal.getTimeInMillis();
			cal.add(Calendar.DATE, -cal.get(Calendar.DAY_OF_WEEK) + 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			long oneWeek = cal.getTimeInMillis();
			cal.add(Calendar.DATE, -7);
			long twoWeeks = cal.getTimeInMillis();
			long base = UserPreferencesHelper.getBaseLineEndTime(context);
			if(base == 0) {
				//If baseline is not set, we set it to 3 months ago
				cal.add(Calendar.MONTH, -3);
				base = cal.getTimeInMillis();
			}

			ArrayList<Double> nowvalues = new ArrayList<Double>();
			ArrayList<Double> weekvalues = new ArrayList<Double>();
			ArrayList<Double> basevalues = new ArrayList<Double>();

			long firstTime = base;

			for(FeedbackItem point : data) {
				if(point.time < base) {
					firstTime = Math.min(firstTime, point.time);
					basevalues.add(point.value);
				} else if(point.time >= twoWeeks && point.time < oneWeek) {
					weekvalues.add(point.value);
				} else if(point.time >= oneWeek && point.time < now) {
					nowvalues.add(point.value);	
				}
			}

			long baseDays = (base - firstTime) / DateUtils.DAY_IN_MILLIS;
			baseLine = calcAverage(basevalues, baseDays);
			long weekDays = (oneWeek - twoWeeks) / DateUtils.DAY_IN_MILLIS;
			lastWeek = calcAverage(weekvalues, weekDays);
			long currentDays = (now - oneWeek) / DateUtils.DAY_IN_MILLIS + 1; //Plus one to include today
			current = calcAverage(nowvalues, currentDays);		
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

			renderer.addSeriesRenderer(0, addSeries(current, "This Week", dataset, POINT_STYLE_CURRENT, Utilities.darkenColor(color)));
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

		private SimpleSeriesRenderer addSeries(Double value, String title, XYMultipleSeriesDataset dataset, PointStyle style, int color) {
			XYSeries series = new XYSeries(title);
			series.add(value, 0);
			dataset.addSeries(series);
			OhmageLineSeriesRenderer sr = new OhmageLineSeriesRenderer();
			sr.setPointStyle(style);
			sr.setColor(color);
			return sr;
		}
	}


	public ComparisonAdapter(Context context) {
		super(context, R.layout.feedback_chart, R.id.chart_title, new ArrayList<ComparisonAdapterItem>());
	}

	public ComparisonAdapter(Context context, List<ComparisonAdapterItem> objects) {
		super(context, R.layout.feedback_chart, R.id.chart_title, objects);
	}
}