
package org.ohmage.activity;

import org.achartengine.chart.BarChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.TimeChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.ohmage.ChartFragment;
import org.ohmage.R;
import org.ohmage.Utilities;
import org.ohmage.ui.BaseActivity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

public class FeedbackActivity extends BaseActivity {

	public static final double[] FAKE_DATA1 = new double[] {
			1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0
	};

	public static final double[] FAKE_DATA2 = new double[] {
			1, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 5, 5, 7, 7, 7, 7, 7, 7, 5, 5, 5, 5, 5, 5
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback_layout);

		if (getSupportFragmentManager().findFragmentById(R.id.chart1) == null) {
			setChart("Exercise Frequency", FAKE_DATA1, R.id.chart1_title, R.id.chart1);
		}

		if (getSupportFragmentManager().findFragmentById(R.id.chart2) == null) {
			setChart("Healthy Diet", FAKE_DATA2, R.id.chart2_title, R.id.chart2);
		}

		if (getSupportFragmentManager().findFragmentById(R.id.chart3) == null) {
			setChart("Random Graph", Utilities.randomData(30, 9), R.id.chart3_title, R.id.chart3);
		}

		if (getSupportFragmentManager().findFragmentById(R.id.feedback_response_graph) == null) {
			ChartFragment f = ChartFragment.newInstance(buildResponseFrequencyGraph());
			getSupportFragmentManager().beginTransaction().add(R.id.feedback_response_graph, f)
					.commit();
		}

	}

	private void setChart(String title, double[] data, int chartTitleId, int chartId) {
		SparkLine chart = new SparkLine(this, title, data);
		((TextView) findViewById(chartTitleId)).setText(chart.getRenderer().getChartTitle());
		ChartFragment f = ChartFragment.newInstance(chart);
		getSupportFragmentManager().beginTransaction().add(chartId, f).commit();
	}

	private BarChart buildResponseFrequencyGraph() {

		double length = 10;
		double[] values = new double[10];

		double max = 0;
		Random r = new Random();
		for (int i = 0; i < values.length; i++) {
			values[i] = Math.abs(r.nextInt() % 6);
			if (values[i] > max)
				max = values[i];
		}
		int[] colors = new int[] {
				getResources().getColor(R.color.highlight)
		};
		PointStyle[] styles = new PointStyle[] {
				PointStyle.X
		};
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		setChartSettings(renderer, "", "Days ago", "# of Responses", length - 0.5, -0.5, 0, max,
				Color.BLACK, Color.DKGRAY);
		renderer.setMarginsColor(Color.WHITE);
		renderer.setPanEnabled(false, false);
		renderer.setZoomEnabled(false);
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setShowLegend(false);
		renderer.setBarSpacing(0.1);

		XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
		XYSeries series = new XYSeries("responses");
		for (int i = 0; i < values.length; i++)
			series.add(i, values[i]);
		dataSet.addSeries(series);

		return new BarChart(dataSet, renderer, BarChart.Type.DEFAULT);
	}

	/**
	 * Builds an XY multiple series renderer.
	 *
	 * @param colors the series rendering colors
	 * @param styles the series point styles
	 * @return the XY multiple series renderers
	 */
	protected XYMultipleSeriesRenderer buildRenderer(int[] colors, PointStyle[] styles) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		setRenderer(renderer, colors, styles);
		return renderer;
	}

	private void setRenderer(XYMultipleSeriesRenderer renderer, int[] colors, PointStyle[] styles) {
		renderer.setAxisTitleTextSize(16);
		renderer.setChartTitleTextSize(20);
		renderer.setLabelsTextSize(15);
		renderer.setLegendTextSize(15);
		renderer.setPointSize(5f);
		renderer.setMargins(new int[] {
				30, 30, 15, 30
		});
		int length = colors.length;
		for (int i = 0; i < length; i++) {
			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setColor(colors[i]);
			r.setPointStyle(styles[i]);
			renderer.addSeriesRenderer(r);
		}
	}

	/**
	 * Sets a few of the series renderer settings.
	 *
	 * @param renderer the renderer to set the properties to
	 * @param title the chart title
	 * @param xTitle the title for the X axis
	 * @param yTitle the title for the Y axis
	 * @param xMin the minimum value on the X axis
	 * @param xMax the maximum value on the X axis
	 * @param yMin the minimum value on the Y axis
	 * @param yMax the maximum value on the Y axis
	 * @param axesColor the axes color
	 * @param labelsColor the labels color
	 */
	protected void setChartSettings(XYMultipleSeriesRenderer renderer, String title, String xTitle,
			String yTitle, double xMin, double xMax, double yMin, double yMax, int axesColor,
			int labelsColor) {
		renderer.setChartTitle(title);
		renderer.setXTitle(xTitle);
		renderer.setYTitle(yTitle);
		renderer.setXAxisMin(xMin);
		renderer.setXAxisMax(xMax);
		renderer.setYAxisMin(yMin);
		renderer.setYAxisMax(yMax);
		renderer.setAxesColor(axesColor);
		renderer.setLabelsColor(labelsColor);
	}

	/**
	 * Builds an XY multiple dataset using the provided values.
	 *
	 * @param titles the series titles
	 * @param xValues the values for the X axis
	 * @param yValues the values for the Y axis
	 * @return the XY multiple dataset
	 */
	protected XYMultipleSeriesDataset buildDataset(String[] titles, List<double[]> xValues,
			List<double[]> yValues) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		addXYSeries(dataset, titles, xValues, yValues, 0);
		return dataset;
	}

	public void addXYSeries(XYMultipleSeriesDataset dataset, String[] titles,
			List<double[]> xValues,
			List<double[]> yValues, int scale) {
		int length = titles.length;
		for (int i = 0; i < length; i++) {
			XYSeries series = new XYSeries(titles[i], scale);
			double[] xV = xValues.get(i);
			double[] yV = yValues.get(i);
			int seriesLength = xV.length;
			for (int k = 0; k < seriesLength; k++) {
				series.add(xV[k], yV[k]);
			}
			dataset.addSeries(series);
		}
	}

	public static class SparkLine extends TimeChart {

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
			multipleRenderer.setZoomEnabled(false);
			return multipleRenderer;
		}
	}
}
