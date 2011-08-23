package org.ohmage.feedback.visualization;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.ohmage.Utilities.KVLTriplet;
import org.ohmage.db.DbContract.PromptResponse;
import org.ohmage.db.DbContract.Response;
import org.ohmage.prompt.Prompt;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.net.Uri;

public class FeedbackTimeScatterChart extends AbstractChart {

	static final String TAG = "FeedbackTimeScatterChart";
	static final int aDayInMilliseconds = 84600000;

	public FeedbackTimeScatterChart(String title, String campaignUrn, String surveyID, String promptID, List<Prompt> prompts) {
		super(title);
		mCampaignUrn = campaignUrn;
		mSurveyID = surveyID;
		mPromptID = promptID;
		mPrompts = prompts;
	}
	
	@Override
	public String getName() {
		return "FeedbackTimeScatterChart";
	}

	@Override
	public String getDesc() {
		return "Feedback Time Scatter Chart Description";
	}

	@Override
	public Intent execute(Context context) {
		
		String[] titles = new String[] { "" };
		List<Date[]> dates = new ArrayList<Date[]>();
		List<double[]> values = new ArrayList<double[]>();
		
		ContentResolver cr = context.getContentResolver();
		Uri queryUri = PromptResponse.getPromptsByCampaignAndSurvey(mCampaignUrn, mSurveyID, mPromptID);
		//String[] projection = new String[] { Response.TIME, PromptResponse.PROMPT_VALUE, PromptResponse.PROMPT_CUSTOM_CHOICES};
		String[] projection = new String[] { Response.TIME, PromptResponse.PROMPT_VALUE};
		
		Cursor cursor = cr.query(queryUri, projection, null, null, Response.TIME);
		if(cursor.getCount() == 0){
			cursor.close();
			return null;
		}
		
		ArrayList<Date> singleDates = new ArrayList<Date>();
		ArrayList<Double> singleValues = new ArrayList<Double>();
		
		double maxResponseValue = 0;
		double maxYLableValue = 0;
		double minYLableValue = 0;
		while (cursor.moveToNext()) {
			// extract date/value from each row and put it in our series
			// 0: time field, as a long
			// 1: prompt value, as text
			singleDates.add(new Date(cursor.getLong(0)));
			singleValues.add(cursor.getDouble(1));
			
			if (cursor.getDouble(1) > maxResponseValue)
				maxResponseValue = cursor.getDouble(1);
		}
		
		cursor.close();
		
		// convert ArrayList<Double> to double[], because java is silly
		double[] singleValuesArray = new double[singleValues.size()];
		for (int i = 0; i < singleValues.size(); ++i)
			singleValuesArray[i] = singleValues.get(i);
		
		// and add our date/value series to the respective containers
		dates.add(singleDates.toArray(new Date[singleDates.size()]));
		values.add(singleValuesArray);
		
		//Get startdate and enddate
		long startDate = dates.get(0)[0].getTime()  - (100 * 60 * 60 * 24 * 3);
		long endDate = dates.get(0)[dates.get(0).length-1].getTime()  + (100 * 60 * 60 * 24 * 3);

		int[] colors = new int[] { Color.rgb(198, 226, 255) };
		PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		((XYSeriesRenderer) renderer.getSeriesRendererAt(0)).setFillPoints(true);
		
		//Set Chart
		setChartSettings(
				renderer,
				"", 
				"Date", 
				"",
				startDate, 
				endDate, 
				-1, 
				maxResponseValue+1,
				Color.GRAY, 
				Color.LTGRAY
		);

		//Set chart layout
		int topMargin = 0;
		int bottomMargin = 50;
		int leftMargin = 6;
		int rightMargin = 2;
		int margins[] = {topMargin, leftMargin, bottomMargin, rightMargin};
				
		renderer.setAxisTitleTextSize(23);
		renderer.setLabelsTextSize(25);
		renderer.setShowGrid(true);		
		renderer.setMargins(margins);
		renderer.setPointSize(12);
		renderer.setShowLegend(false);
		renderer.setShowAxes(true);
		renderer.setXLabelsAlign(Align.LEFT);
		renderer.setYLabelsAngle(330);
		renderer.setXLabelsAngle(330);
		renderer.setZoomButtonsVisible(true);
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(Color.DKGRAY);
					
		//Set Y Label
		//TODO Need to organize all prompt types instead of handling this with NULL 
		renderer.setYLabelsAlign(Align.LEFT);
		List<KVLTriplet> propertiesList = getPropertiesList(mPromptID);
		if(propertiesList != null){
			//Prompts that have properties (SingleChoice, MultiChoice, etc)
			for(KVLTriplet i : propertiesList){
				double iValue = Double.valueOf(i.key).doubleValue();
				renderer.addYTextLabel(iValue, i.label);
				if(maxYLableValue < iValue){
					maxYLableValue = iValue;
				}
				if(minYLableValue > iValue){
					minYLableValue = iValue;
				}
			}
		}

		// disables interpolated values from being added between text labels
		// i assume we want this if we're providing specific labels for enumerations
		renderer.setYLabels(0);
		renderer.setLabelsTextSize(25);

		//Set pan limit from startData-3days to endDate+3days
		renderer.setPanEnabled(true, true);
		renderer.setPanLimits(new double[]{startDate-(aDayInMilliseconds*4), endDate+(aDayInMilliseconds*3), minYLableValue-1, maxYLableValue+1});
		
		//Set zoom
		renderer.setZoomEnabled(true, false);
		renderer.setZoomLimits(new double[]{startDate-(aDayInMilliseconds*4), endDate+(aDayInMilliseconds*3), minYLableValue-1, maxYLableValue+1});
		
		renderer.setZoomRate(2);
		
		int length = renderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++) {
			SimpleSeriesRenderer seriesRenderer = renderer.getSeriesRendererAt(i);
			seriesRenderer.setDisplayChartValues(false);
		}
		
		return ChartFactory.getTimeLineChartIntent(
				context, 
				buildDateDataset(titles, dates, values), 
				renderer, 
				"MM/dd hha", 
				mChartTitle
				);
	}
}
