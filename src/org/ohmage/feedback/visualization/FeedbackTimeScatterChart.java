package org.ohmage.feedback.visualization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;

public class FeedbackTimeScatterChart extends AbstractChart {

	static final String TAG = "FeedbackScatterChart";
	protected String mCampaignUrn;
	protected String mChartTitle;

	public FeedbackTimeScatterChart(String title){
		super(title);
	}

	/**
	 * Returns the chart name.
	 * @return the chart name
	 */
	public String getName() {
		return "Scatter chart";
	}

	/**
	 * Returns the chart description.
	 * @return the chart description
	 */
	public String getDesc() {
		return "Scatter chart";
	}

	/**
	 * Executes the chart demo.
	 * @param context the context
	 * @return the built intent
	 */
	public Intent execute(Context context) {
		String[] titles = new String[] { "Series 1", "Series 2", "Series 3", "Series 4", "Series 5" };
		List<double[]> x = new ArrayList<double[]>();
		List<double[]> values = new ArrayList<double[]>();
		int count = 20;
		int length = titles.length;
		Random r = new Random();
		for (int i = 0; i < length; i++) {
			double[] xValues = new double[count];
			double[] yValues = new double[count];
			for (int k = 0; k < count; k++) {
				xValues[k] = k + r.nextInt() % 10;
				yValues[k] = k * 2 + r.nextInt() % 10;
			}
			x.add(xValues);
			values.add(yValues);
		}
		int[] colors = new int[] { Color.BLUE, Color.CYAN, Color.MAGENTA, Color.LTGRAY, Color.GREEN };
		PointStyle[] styles = new PointStyle[] { PointStyle.X, PointStyle.DIAMOND, PointStyle.TRIANGLE,
				PointStyle.SQUARE, PointStyle.CIRCLE };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		setChartSettings(renderer, "Scatter chart", "X", "Y", -10, 30, -10, 51, Color.GRAY,
				Color.LTGRAY);
		renderer.setXLabels(10);
		renderer.setYLabels(10);
		
		//Set chart layout
		int topMargin = 0;
		int bottomMargin = 50;
		int leftMargin = 2;
		int rightMargin = 2;
		int margins[] = {topMargin, leftMargin, bottomMargin, rightMargin};
				
		renderer.setAxisTitleTextSize(23);
		renderer.setLabelsTextSize(20);
		renderer.setShowGrid(true);		
		renderer.setMargins(margins);
		renderer.setPointSize(8);
		renderer.setShowLegend(false);
		renderer.setShowAxes(true);
		renderer.setXLabelsAlign(Align.LEFT);
		renderer.setXLabelsAngle(330);
		renderer.setZoomButtonsVisible(true);
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(Color.DKGRAY);
		
		length = renderer.getSeriesRendererCount();
		
		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}

		return ChartFactory.getTimeScatterChartIntent(context, buildDataset(titles, x, values), renderer, "MM/dd hha", mChartTitle);
	}
}
