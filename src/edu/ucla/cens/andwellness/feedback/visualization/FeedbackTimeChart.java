/**
 * Copyright (C) 2009, 2010 SC 4ViewSoft SRL
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.ucla.cens.andwellness.feedback.visualization;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import edu.ucla.cens.andwellness.feedback.FeedbackContract;
import edu.ucla.cens.andwellness.feedback.FeedbackContract.FeedbackPromptResponses;
import edu.ucla.cens.andwellness.feedback.FeedbackContract.FeedbackResponses;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;

/**
 * Project status demo chart.
 */
public class FeedbackTimeChart extends AbstractChart {
	protected String mPromptID;
	protected String mCampaignUrn;
	protected String mSurveyID;

	public FeedbackTimeChart(String title, String campaignUrn, String surveyID, String promptID) {
		super(title);
		mCampaignUrn = campaignUrn;
		mSurveyID = surveyID;
		mPromptID = promptID;
	}

	/**
	 * Returns the chart name.
	 * 
	 * @return the chart name
	 */
	public String getName() {
		return "FeedbackTimeChart";
	}

	/**
	 * Returns the chart description.
	 * 
	 * @return the chart description
	 */
	public String getDesc() {
		return "Feedback Time Chart Description";
	}

	/**
	 * Executes the chart demo.
	 * 
	 * @param context
	 *            the context
	 * @return the built intent
	 */
	public Intent execute(Context context) {
		/*
	    //String[] titles = new String[] { "New tickets", "Fixed tickets"};
	    String[] titles = new String[] { ""};
	    List<Date[]> dates = new ArrayList<Date[]>();
	    List<double[]> values = new ArrayList<double[]>();
	    int length = titles.length;
	    for (int i = 0; i < length; i++) {
	      dates.add(new Date[12]);
	      dates.get(i)[0] = new Date(111, 5, 30);
	      dates.get(i)[1] = new Date(111, 6, 1);
	      dates.get(i)[2] = new Date(111, 6, 2);
	      dates.get(i)[3] = new Date(111, 6, 3);
	      dates.get(i)[4] = new Date(111, 6, 4);
	      dates.get(i)[5] = new Date(111, 6, 5);
	      dates.get(i)[6] = new Date(111, 6, 6);
	      dates.get(i)[7] = new Date(111, 6, 7);
	      dates.get(i)[8] = new Date(111, 6, 8);
	      dates.get(i)[9] = new Date(111, 6, 9);
	      dates.get(i)[10] = new Date(111, 6, 10);
	      dates.get(i)[11] = new Date(111, 6, 11);
	    }
	    values.add(new double[] { 3, 4, 4, 6, 5, 5, 4, 2, 1, 5, 3, 5});
	    //values.add(new double[] { 102, 90, 112, 105, 125, 112, 125, 112, 105, 115, 116, 135 });
	    length = values.get(0).length;
	    
	    
	    //int[] colors = new int[] { Color.BLUE, Color.GREEN };
	    int[] colors = new int[] { Color.RED };
	    PointStyle[] styles = new PointStyle[] { PointStyle.X};
	    XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
	    //renderer.setXLabels(5);
	    //renderer.setYLabels(10);
	    renderer.setShowGrid(true);

	    setChartSettings(renderer, "", "Date", "# of participation", dates.get(0)[0].getTime(),
	        dates.get(0)[11].getTime(), 0, 8, Color.GRAY, Color.LTGRAY);

	    length = renderer.getSeriesRendererCount();
	    for (int i = 0; i < length; i++) {
	      SimpleSeriesRenderer seriesRenderer = renderer.getSeriesRendererAt(i);
	      seriesRenderer.setDisplayChartValues(true);
	    }
	    return ChartFactory.getTimeChartIntent(context, buildDateDataset(titles, dates, values),
	        renderer, "MM/dd/yyyy",mChartTitle);
	        */
		
		// titles for each of the series (in this case we have only one)
		String[] titles = new String[] { "" };
		// list of labels for each series (only one, since we only have one series)
		List<Date[]> dates = new ArrayList<Date[]>();
		// list of values for each series (only one, since we only have one series)
		List<double[]> values = new ArrayList<double[]>();
		
		// call up the contentprovider and feed it the campaign, survey, and prompt ID
		// we should get a series of prompt values for the survey that we can plot
		ContentResolver cr = context.getContentResolver();
		// URI to match "<campaignUrn>/<surveyID>/responses/prompts/<promptID>"
		Uri queryUri = FeedbackPromptResponses.getPromptsByCampaignAndSurvey(mCampaignUrn, mSurveyID, mPromptID);
		// columns to return; in this case, we just need the date and the value at that date point
		String[] projection = new String[] { FeedbackResponses.TIME, FeedbackPromptResponses.PROMPT_VALUE };
		
		// nab that data!
		Cursor cursor = cr.query(queryUri, projection, null, null, null);
		
		// now we iterate through the cursor and insert each column of each row
		// into the appropriate list
		ArrayList<Date> singleDates = new ArrayList<Date>();
		ArrayList<Double> singleValues = new ArrayList<Double>();
		
		double maxValue = 0;

		while (cursor.moveToNext()) {
			// extract date/value from each row and put it in our series
			// 0: time field, as a long
			// 1: prompt value, as text
			singleDates.add(new Date(cursor.getLong(0)));
			singleValues.add(cursor.getDouble(1));
			
			if (cursor.getDouble(1) > maxValue)
				maxValue = cursor.getDouble(1);
		}
		
		cursor.close();
		
		// convert ArrayList<Double> to double[], because java is silly
		double[] singleValuesArray = new double[singleValues.size()];
		for (int i = 0; i < singleValues.size(); ++i)
			singleValuesArray[i] = singleValues.get(i);
		
		// and add our date/value series to the respective containers
		dates.add(singleDates.toArray(new Date[singleDates.size()]));
		values.add(singleValuesArray);

		// int[] colors = new int[] { Color.BLUE, Color.GREEN };
		int[] colors = new int[] { Color.RED };
		PointStyle[] styles = new PointStyle[] { PointStyle.X };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		renderer.setShowGrid(true);

		setChartSettings(renderer,
				"", "Date", mChartTitle,
				dates.get(0)[0].getTime(), dates.get(0)[dates.get(0).length-1].getTime(), 0, maxValue,
				Color.GRAY, Color.LTGRAY);

		int length = renderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++) {
			SimpleSeriesRenderer seriesRenderer = renderer.getSeriesRendererAt(i);
			seriesRenderer.setDisplayChartValues(true);
		}
		return ChartFactory.getTimeChartIntent(context, buildDateDataset(
				titles, dates, values), renderer, "MM/dd/yy hh:mma", mChartTitle);
	}

}
