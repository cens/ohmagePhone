package org.ohmage.feedback.visualization;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.ohmage.feedback.FeedbackContract;
import org.ohmage.feedback.FeedbackContract.FeedbackResponses;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.net.Uri;

public class FeedbackTimeScatterChart extends AbstractChart {

	static final String TAG = "FeedbackScatterChart";
	protected String mCampaignUrn;
	protected String mChartTitle;
	protected String mSurveyID;
	protected Context mContext;

	public FeedbackTimeScatterChart( String title, String campaignUrn, String surveyID, Context context){
		super(title);
		mContext = context;
		mCampaignUrn = campaignUrn;
		mSurveyID = surveyID;
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
		
		ContentResolver cr = context.getContentResolver();

		// columns to return; in this case, we just need the date and the value at that date point
		String[] projection = new String[] { FeedbackResponses.TIME, FeedbackResponses.CAMPAIGN_URN, FeedbackResponses.SURVEY_ID };
		
		// nab that data! data is sorted by FeedbackResponses.TIME
		Cursor cursor = cr.query(Uri.parse("content://" + FeedbackContract.CONTENT_AUTHORITY + "/" + "responses"), projection, null, null, FeedbackResponses.TIME);
		
		if(cursor.getCount() == 0){
			cursor.close();
			return null;
		}
		
		
		//Key: SurveyID 
		//Value:Date
		Map<String, ArrayList<Date>> dataMap = new HashMap<String, ArrayList<Date>>();
		Long startDate = new Long(Long.MAX_VALUE); 
		Long endDate = new Long(Long.MIN_VALUE);

		while (cursor.moveToNext()) {
			Long time = cursor.getLong(0);
			String campaignUrn = cursor.getString(1);
			String surveyId = cursor.getString(2);
			//Toast.makeText(mContext, String.valueOf(time) + surveyId, Toast.LENGTH_SHORT).show();
			
			//Build dataMap to draw charts
			if(dataMap.containsKey(surveyId) == false){
				ArrayList<Date> dates = new ArrayList<Date>();
				dates.add(new Date(time));
				dataMap.put(surveyId, dates);
			}
			else{
				ArrayList<Date> dates = dataMap.get(surveyId);
				dates.add(new Date(time));
				dataMap.put(surveyId, dates);
			}
			if(startDate > time){
				startDate = time;
			}
			if(endDate > time){
				endDate = time;
			}
		}
		String[] titles = dataMap.keySet().toArray(new String[dataMap.keySet().size()]);
		
		List<Date[]> xValues = new ArrayList<Date[]>();
		List<double[]> yValues = new ArrayList<double[]>();
		
		for (int i = 0; i < titles.length; i++) {
					
			List<Date> xValuesOfASeries = dataMap.get(titles[i]);
			double[] yValuesOfASeries = new double[xValuesOfASeries.size()];
			
			for(int k=0; k<xValuesOfASeries.size(); k++){
				yValuesOfASeries[k] = i;
			}
			
			xValues.add(xValuesOfASeries.toArray(new Date[xValuesOfASeries.size()]));
			yValues.add(yValuesOfASeries);
		}
		
		ArrayList<Integer> colorsArrayList = new ArrayList<Integer>();
		for(int k=0; k<titles.length; k++){
			colorsArrayList.add(new Integer(Color.rgb(198, 226, 255)));
		}
		
		
		//Set color array
		int[] colors = new int[colorsArrayList.size()];
		for(int i = 0; i<colorsArrayList.size(); i++){
			colors[i] = colorsArrayList.get(i).intValue();
		}
		
		//int[] colors = new int[] { Color.rgb(198, 226, 255), Color.rgb(198, 226, 255), Color.rgb(198, 226, 255)};
		
		//Set styles
		List<PointStyle> stylesArrayList = new ArrayList<PointStyle>();
		for(int i=0; i<colorsArrayList.size(); i++){
			stylesArrayList.add(PointStyle.SQUARE);
		}
		
		//PointStyle[] styles = new PointStyle[] { PointStyle.SQUARE, PointStyle.SQUARE, PointStyle.SQUARE};
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, stylesArrayList.toArray(new PointStyle[stylesArrayList.size()]));
		
		//Set Chart
		setChartSettings(
				renderer,
				"", 
				"Date", 
				"",
				startDate.doubleValue(), 
				endDate.doubleValue(), 
				0, 
				titles.length+1,
				Color.GRAY, 
				Color.LTGRAY
		);
		
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
		renderer.setPointSize(10);
		renderer.setShowLegend(false);
		renderer.setShowAxes(true);
		renderer.setXLabelsAlign(Align.LEFT);
		renderer.setYLabelsAlign(Align.LEFT);
		renderer.setXLabelsAngle(330);
		renderer.setZoomButtonsVisible(true);
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(Color.DKGRAY);

		int length = renderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}

		return ChartFactory.getTimeScatterChartIntent(context,buildDateDataset(titles, xValues, yValues), renderer, "MM/dd hha", mChartTitle);
	}


}
