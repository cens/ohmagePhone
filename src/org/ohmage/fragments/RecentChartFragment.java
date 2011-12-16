
package org.ohmage.fragments;

import org.ohmage.R;
import org.ohmage.adapters.SparklineAdapter;
import org.ohmage.adapters.SparklineAdapter.ChartItem;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;

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

import java.util.ArrayList;

public class RecentChartFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

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

			while("foodButton".equals(surveyId) || "stressButton".equals(surveyId)) {
				data.moveToNext();
				if(data.isAfterLast())
					return;
				surveyId = data.getString(ResponseQuery.SURVEY_ID);
			}

			if("Morning".equals(surveyId) || "Mid Day".equals(surveyId) || "Late Afternoon".equals(surveyId)) {
				double[] stressData = getData(data.getString(ResponseQuery.CAMPAIGN_URN), "howStressed");
				if(stressData.length > 1)
					mAdapter.add(new ChartItem("Stress Amount", stressData, R.color.dark_red, R.color.light_red));

				double[] foodData = getData(data.getString(ResponseQuery.CAMPAIGN_URN), "foodQuality");
				if(foodData.length > 1)
					mAdapter.add(new ChartItem("Food Quality", foodData, R.color.powderkegblue, R.color.light_blue));

				double[] foodAmountData = getData(data.getString(ResponseQuery.CAMPAIGN_URN), "foodHowMuch");
				if(foodAmountData.length > 1)
					mAdapter.add(new ChartItem("Food Amount", foodAmountData, R.color.powderkegblue, R.color.light_blue));

			} else if("Bedtime".equals(surveyId)) {
				double[] exerciseData = getData(data.getString(ResponseQuery.CAMPAIGN_URN), "didYouExercise");
				if(exerciseData.length > 1)
					mAdapter.add(new ChartItem("Exercise", exerciseData, R.color.dark_green, R.color.light_green));

				double[] aloneData = getData(data.getString(ResponseQuery.CAMPAIGN_URN), "timeForYourself");
				if(aloneData.length > 1)
					mAdapter.add(new ChartItem("Time For Yourself", aloneData, R.color.dark_purple, R.color.light_purple));
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

	private double[] toArray(ArrayList<? extends Number> data) {
		double[] ret = new double[data.size()];
		for(int i=0;i<ret.length;i++)
			ret[i] = data.get(i).doubleValue();
		return ret;
	}

	private double[] getData(String campaignUrn, String promptId) {
		Cursor promptResponses = getActivity().managedQuery(PromptResponses.getPromptsByCampaign(campaignUrn, promptId), new String[] { PromptResponses.PROMPT_RESPONSE_VALUE, PromptResponses.PROMPT_RESPONSE_EXTRA_VALUE }, null, null, null);
		ArrayList<Integer> data = new ArrayList<Integer>();
		for(int j=0;j<promptResponses.getCount();j++) {
			if(promptResponses.moveToNext()) {
				try {
					data.add(Integer.parseInt(promptResponses.getString(0)));
				} catch(NumberFormatException e)	{
					try {
						data.add(Integer.parseInt(promptResponses.getString(1)));
					} catch(NumberFormatException e2)	{
						// Don't add anything
					}
				}
			}
		}

		return toArray(data);
	}
}