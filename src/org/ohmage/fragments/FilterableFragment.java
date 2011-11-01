package org.ohmage.fragments;

import org.ohmage.ui.OhmageFilterable.CampaignSurveyFilter;
import org.ohmage.ui.OhmageFilterable.FilterableFragmentLoader;
import org.ohmage.ui.OhmageFilterable.TimeFilter;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Fragments can extend the {@link FilterableFragment} if the want to easily be able to filter by campaign, survey, and month/year
 * @author cketcham
 *
 */
public abstract class FilterableFragment extends Fragment implements FilterableFragmentLoader {
	CampaignSurveyFilter mCampaignSurveyFilter;
	TimeFilter mTimeFilter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCampaignSurveyFilter = new CampaignSurveyFilter(getArguments());
		mTimeFilter = new TimeFilter(getArguments());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void setCampaignUrn(String campaignUrn) {
		mCampaignSurveyFilter.setCampaignUrn(campaignUrn);
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public String getCampaignUrn() {
		return mCampaignSurveyFilter.getCampaignUrn();
	}

	@Override
	public void setSurveyId(String surveyId) {
		mCampaignSurveyFilter.setSurveyId(surveyId);
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public String getSurveyId() {
		return mCampaignSurveyFilter.getSurveyId();
	}

	@Override
	public void setDate(int day, int month, int year) {
		mTimeFilter.setDate(day, month, year);
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void setMonth(int month, int year) {
		mTimeFilter.setMonth(month, year);
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public long getStartBounds() {
		return mTimeFilter.getStartBounds();
	}

	@Override
	public long getEndBounds() {
		return mTimeFilter.getEndBounds();
	}

	@Override
	public int getMonth() {
		return mTimeFilter.getMonth();
	}

	@Override
	public int getYear() {
		return mTimeFilter.getYear();
	}
}