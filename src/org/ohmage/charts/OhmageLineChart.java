package org.ohmage.charts;

import org.achartengine.chart.ScatterChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.ohmage.Utilities;
import org.ohmage.charts.HistogramBase.CleanRenderer;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;

/**
 * This chart has some extra point styles which work ok for a line
 */
public class OhmageLineChart extends ScatterChart {

	/**
	 * Builds a new scatter chart instance.
	 * 
	 * @param dataset the multiple series dataset
	 * @param renderer the multiple series renderer
	 */
	public OhmageLineChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
		super(dataset, renderer);
	}

	/**
	 * The graphical representation of a series.
	 * 
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param points the array of points to be used for drawing the series
	 * @param seriesRenderer the series renderer
	 * @param yAxisValue the minimum value of the y axis
	 * @param seriesIndex the index of the series currently being drawn
	 */
	@Override
	public void drawSeries(Canvas canvas, Paint paint, float[] points,
			SimpleSeriesRenderer seriesRenderer, float yAxisValue, int seriesIndex) {
		XYSeriesRenderer renderer = (XYSeriesRenderer) seriesRenderer;
		paint.setColor(renderer.getColor());
		if (renderer.isFillPoints()) {
			paint.setStyle(Style.FILL);
		} else {
			paint.setStyle(Style.STROKE);
		}
		int length = points.length;
		switch (renderer.getPointStyle()) {
			case DASHED_LINE:
				for (int i = 0; i < length; i += 2) {
					drawDashedLine(canvas, paint, points[i], points[i + 1]);
				}
				break;
			case RECTANGLE:
				for (int i = 0; i < length; i += 2) {
					drawRectangle(canvas, paint, points[i], points[i + 1]);
				}
				break;
			default:
				super.drawSeries(canvas, paint, points, seriesRenderer, yAxisValue, seriesIndex);
		}
	}

	/**
	 * The graphical representation of the legend shape.
	 * 
	 * @param canvas the canvas to paint to
	 * @param renderer the series renderer
	 * @param x the x value of the point the shape should be drawn at
	 * @param y the y value of the point the shape should be drawn at
	 * @param seriesIndex the series index
	 * @param paint the paint to be used for drawing
	 */
	@Override
	public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y,
			int seriesIndex, Paint paint) {
		if (((XYSeriesRenderer) renderer).isFillPoints()) {
			paint.setStyle(Style.FILL);
		} else {
			paint.setStyle(Style.STROKE);
		}
		switch (((XYSeriesRenderer) renderer).getPointStyle()) {
			case DASHED_LINE:
				drawDashedLine(canvas, paint, x+1, y);
				break;
			case RECTANGLE:
				drawRectangle(canvas, paint, x + size/2, y);
				break;
			default:
				super.drawLegendShape(canvas, renderer, x, y, seriesIndex, paint);
		}
	}

	/**
	 * The graphical representation of a dashed line
	 * 
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param x the x value of the point the shape should be drawn at
	 * @param y the y value of the point the shape should be drawn at
	 */
	private void drawDashedLine(Canvas canvas, Paint paint, float x, float y) {
		paint.setPathEffect(new DashPathEffect(new float[] {3, 3}, 0));
		float oldwidth = paint.getStrokeWidth();
		paint.setStrokeWidth(2);
		canvas.drawLine(x, y - size, x, y + size, paint);
		paint.setStrokeWidth(oldwidth);
		paint.setPathEffect(null);
	}

	@Override
	public int getLegendShapeWidth(int seriesIndex) {
		switch(((XYSeriesRenderer) getRenderer().getSeriesRendererAt(seriesIndex)).getPointStyle()) {
			case DASHED_LINE:
				return 2;
			case RECTANGLE:
				return (int) (size/2.0);
			default:
				return super.getLegendShapeWidth(seriesIndex);
		}
	}

	/**
	 * The graphical representation of a rectangle point shape.
	 * 
	 * @param canvas the canvas to paint to
	 * @param paint the paint to be used for drawing
	 * @param x the x value of the point the shape should be drawn at
	 * @param y the y value of the point the shape should be drawn at
	 */
	private void drawRectangle(Canvas canvas, Paint paint, float x, float y) {
		canvas.drawRect(x - size/2, y - size, x + size/2, y + size, paint);
	}

	public static class OhmageLineRenderer extends CleanRenderer {
		public OhmageLineRenderer() {
			clearSeriesRenderers();

			setLabelsTextSize(Utilities.dpToPixels(11));
			setLegendTextSize(Utilities.dpToPixels(11));
			setPointSize(Utilities.dpToPixels(10));
			setMargins(new int[] {
					0, Utilities.dpToPixels(14), 0, Utilities.dpToPixels(14)
			});

			setDrawAxesBelowSeries(true);
			setShowYAxis(false);
			setLegendHeight(Utilities.dpToPixels(20));
			setLegendGrayscale(true);
		}
	}

	public static class OhmageLineSeriesRenderer extends XYSeriesRenderer {
		public OhmageLineSeriesRenderer() {
			setFillPoints(true);
		}
	}
}