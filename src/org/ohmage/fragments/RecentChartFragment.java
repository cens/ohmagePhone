
package org.ohmage.fragments;

import org.ohmage.NIHConfig;
import org.ohmage.R;
import org.ohmage.adapters.SparklineAdapter;
import org.ohmage.charts.SparkLine;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class RecentChartFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	/**
	 * The maximum number of points shown in each {@link SparkLine}
	 */
	private static final int MAX_DATA_POINTS = 20;

	/**
	 * Creates a new instance of the recent chart fragment
	 * @return the chart fragment
	 */
	public static RecentChartFragment newInstance() {
		return new RecentChartFragment();
	}

	private SparklineAdapter mAdapter;
	private LinearLayout mContainer;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter = new SparklineAdapter(getActivity());

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.recent_chart_fragment_layout, container, false);
		mContainer = (LinearLayout) view.findViewById(R.id.recent_chart_container);
		return view;
	}

	private static class ResponseQuery {
		static final String[] projection = new String[] {
			Responses.CAMPAIGN_URN,
			Responses.SURVEY_ID
		};

		static final int CAMPAIGN_URN = 0;
		static final int SURVEY_ID = 1;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), Responses.CONTENT_URI, ResponseQuery.projection, 
				null, null, Responses.RESPONSE_TIME + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.clear();
		
		while(mAdapter.isEmpty() && data.moveToNext()) {

			String surveyId = data.getString(ResponseQuery.SURVEY_ID);

			while(NIHConfig.Surveys.FOOD_BUTTON.equals(surveyId) || NIHConfig.Surveys.STRESS_BUTTON.equals(surveyId)) {
				data.moveToNext();
				if(data.isAfterLast())
					return;
				surveyId = data.getString(ResponseQuery.SURVEY_ID);
			}

			if(NIHConfig.Surveys.MORNING.equals(surveyId) || NIHConfig.Surveys.MIDDAY.equals(surveyId) || NIHConfig.Surveys.LATEAFTERNOON.equals(surveyId)) {
				double[] stressData = getData(data.getString(ResponseQuery.CAMPAIGN_URN), NIHConfig.SQL.HOW_STRESSED_ID);
				if(stressData.length > 1)
					mAdapter.add(NIHConfig.getExtraPromptData(NIHConfig.Prompt.HOW_STRESSED_ID).toSparkLineChartItem(stressData));

				double[] foodData = getData(data.getString(ResponseQuery.CAMPAIGN_URN), NIHConfig.SQL.FOOD_QUALITY_ID);
				if(foodData.length > 1)
					mAdapter.add(NIHConfig.getExtraPromptData(NIHConfig.Prompt.FOOD_QUALITY_ID).toSparkLineChartItem(foodData));

				double[] foodAmountData = getData(data.getString(ResponseQuery.CAMPAIGN_URN), NIHConfig.SQL.FOOD_QUANTITY_ID);
				if(foodAmountData.length > 1)
					mAdapter.add(NIHConfig.getExtraPromptData(NIHConfig.Prompt.FOOD_QUANTITY_ID).toSparkLineChartItem(foodAmountData));

			} else if(NIHConfig.Surveys.BEDTIME.equals(surveyId)) {
				double[] exerciseData = getData(data.getString(ResponseQuery.CAMPAIGN_URN), NIHConfig.SQL.DID_EXERCISE_ID);
				if(exerciseData.length > 1)
					mAdapter.add(NIHConfig.getExtraPromptData(NIHConfig.Prompt.DID_EXERCISE_ID).toSparkLineChartItem(exerciseData));

				double[] aloneData = getData(data.getString(ResponseQuery.CAMPAIGN_URN), NIHConfig.SQL.TIME_TO_YOURSELF_ID);
				if(aloneData.length > 1)
					mAdapter.add(NIHConfig.getExtraPromptData(NIHConfig.Prompt.TIME_TO_YOURSELF_ID).toSparkLineChartItem(aloneData));
			}

			if(!mAdapter.isEmpty()) {
				mContainer.removeAllViews();
				for(int i=0; i< mAdapter.getCount(); i++) {
					mContainer.addView(mAdapter.getView(i, null, mContainer));
				}
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.clear();
	}

	private double[] getData(String campaignUrn, String promptId) {
		return Models.PromptResponse.getData(getActivity(), campaignUrn, promptId, MAX_DATA_POINTS);
	}
}