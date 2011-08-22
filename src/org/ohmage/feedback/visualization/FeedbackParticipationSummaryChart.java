package org.ohmage.feedback.visualization;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.ohmage.db.DbContract;
import org.ohmage.db.DbContract.Response;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.net.Uri;

public class FeedbackParticipationSummaryChart extends AbstractChart {

	static final String TAG = "FeedbackScatterChart";
	static final int aDayInMilliseconds = 84600000;
	protected Context mContext;

	public FeedbackParticipationSummaryChart( String title, String campaignUrn, String surveyID, Context context){
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
		String[] projection = new String[] { Response.TIME, Response.CAMPAIGN_URN, Response.SURVEY_ID };
		
		// nab that data! data is sorted by FeedbackResponses.TIME
		Cursor cursor = cr.query(Uri.parse("content://" + DbContract.CONTENT_AUTHORITY + "/" + "responses"), projection, null, null, Response.TIME);
		
		if(cursor.getCount() == 0){
			cursor.close();
			return null;
		}
		
		//Key: SurveyID 
		//Value: Date
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
			if(endDate < time){
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
			colorsArrayList.add(new Integer(Color.rgb((198+k*30)%256, 226, (255+k*30)%256)));
		}
		
		//Set color array
		int[] colors = new int[colorsArrayList.size()];
		for(int i = 0; i<colorsArrayList.size(); i++){
			colors[i] = colorsArrayList.get(i).intValue();
		}
		
		//Set styles
		List<PointStyle> stylesArrayList = new ArrayList<PointStyle>();
		for(int i=0; i<colorsArrayList.size(); i++){
			stylesArrayList.add(PointStyle.DIAMOND);
		}
		
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, stylesArrayList.toArray(new PointStyle[stylesArrayList.size()]));
		
		//Set Chart
		setChartSettings(
				renderer,
				"", 
				"Date", 
				"",
				startDate.doubleValue()-(aDayInMilliseconds*6),
				endDate.doubleValue()+(aDayInMilliseconds*6), 
				-1,
				titles.length,
				Color.GRAY, 
				Color.LTGRAY
		);
		
		renderer.setXLabels(10);
		renderer.setYLabels(0);
		renderer.setLabelsTextSize(25);

		//Set Y labels
		for(int k=0; k<titles.length ; k++){
			renderer.addYTextLabel(k, titles[k]);
		}
		
		//Set pan limit from startData-3days to endDate+3days
		renderer.setPanEnabled(true, true);
		renderer.setPanLimits(new double[]{startDate-(aDayInMilliseconds*6), endDate+(aDayInMilliseconds*6), -1, titles.length});
		
		//Set zoom
		renderer.setZoomEnabled(true, false);
		renderer.setZoomLimits(new double[]{startDate-(aDayInMilliseconds*6), endDate+(aDayInMilliseconds*6), -1, titles.length});
		
		renderer.setZoomRate(2);
		
		//Set chart layout
		int topMargin = 0;
		int bottomMargin = 50;
		int leftMargin = 4;
		int rightMargin = 2;
		int margins[] = {topMargin, leftMargin, bottomMargin, rightMargin};

		renderer.setAxisTitleTextSize(23);
		renderer.setLabelsTextSize(20);
		renderer.setShowGrid(true);
		renderer.setMargins(margins);
		renderer.setPointSize(14);
		renderer.setShowLegend(false);
		renderer.setShowAxes(true);
		renderer.setXLabelsAlign(Align.LEFT);
		renderer.setYLabelsAlign(Align.LEFT);
		renderer.setXLabelsAngle(330);
		renderer.setYLabelsAngle(330);
		renderer.setZoomButtonsVisible(true);
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(Color.DKGRAY);

		int length = renderer.getSeriesRendererCount();
		
		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}

		return ChartFactory.getTimeScatterChartIntent(context,buildDateDataset(titles, xValues, yValues), renderer, "MM/dd hh aa", mChartTitle);
	}
}
