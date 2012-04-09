
package org.ohmage.fragments;

import org.ohmage.ui.OhmageFilterable.CampaignSurveyFilter;
import org.ohmage.ui.OhmageFilterable.FilterableFragmentLoader;
import org.ohmage.ui.OhmageFilterable.TimeFilter;

import android.os.Bundle;
import android.support.v4.app.ListFragment;

/**
 * Fragments can extend the {@link FilterableListFragment} if the want to easily
 * be able to filter by campaign, survey, and month/year
 * 
 * @author cketcham
 */
public abstract class FilterableListFragment extends ListFragment implements FilterableFragmentLoader {
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
		if (mCampaignSurveyFilter != null) {
			mCampaignSurveyFilter.setCampaignUrn(campaignUrn);
			getLoaderManager().restartLoader(0, null, this);
		}
	}

	@Override
	public String getCampaignUrn() {
		return (mCampaignSurveyFilter != null) ? mCampaignSurveyFilter.getCampaignUrn() : null;
	}

	@Override
	public void setSurveyId(String surveyId) {
		if (mCampaignSurveyFilter != null) {
			mCampaignSurveyFilter.setSurveyId(surveyId);
			getLoaderManager().restartLoader(0, null, this);
		}
	}

	@Override
	public String getSurveyId() {
		return (mCampaignSurveyFilter != null) ? mCampaignSurveyFilter.getSurveyId() : null;
	}

	@Override
	public void setDate(int day, int month, int year) {
		if (mTimeFilter != null) {
			mTimeFilter.setDate(day, month, year);
			getLoaderManager().restartLoader(0, null, this);
		}
	}

	@Override
	public void setMonth(int month, int year) {
		if (mTimeFilter != null) {
			mTimeFilter.setMonth(month, year);
			getLoaderManager().restartLoader(0, null, this);
		}
	}

	@Override
	public long getStartBounds() {
		return (mTimeFilter != null) ? mTimeFilter.getStartBounds() : 0;
	}

	@Override
	public long getEndBounds() {
		return (mTimeFilter != null) ? mTimeFilter.getEndBounds() : 0;
	}

	@Override
	public int getMonth() {
		return (mTimeFilter != null) ? mTimeFilter.getMonth() : 0;
	}

	@Override
	public int getYear() {
		return (mTimeFilter != null) ? mTimeFilter.getYear() : 0;
	}
}