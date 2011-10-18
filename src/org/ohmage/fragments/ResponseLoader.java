package org.ohmage.fragments;

import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbProvider;
import org.ohmage.ui.OhmageFilterable.FilterableFragmentLoader;

import android.os.Bundle;
import android.support.v4.content.CursorLoader;

/**
 * The {@link ResponseLoader} makes it easy for a {@link FilterableFragment} to get the CursorLoader it
 * needs to query for a set of responses
 * @author cketcham
 *
 */
public class ResponseLoader {

	private final FilterableFragmentLoader mFragment;
	private final String[] mProjection;
	private final String mSelection;

	public ResponseLoader(FilterableFragmentLoader fragment, String[] projection) {
		this(fragment, projection, null);
	}

	public ResponseLoader(FilterableFragmentLoader fragment, String[] projection, String selection) {
		mFragment = fragment;
		mProjection = projection;
		mSelection = selection;
	}

	public CursorLoader onCreateLoader(int arg0, Bundle arg1) {

		// Set the campaign filter selection
		StringBuilder selection = new StringBuilder();
		if(mFragment.getCampaignUrn() != null)
			selection.append(DbProvider.Qualified.RESPONSES_CAMPAIGN_URN + "='" + mFragment.getCampaignUrn() + "' AND ");

		// Set the survey filter selection
		if(mFragment.getSurveyId() != null)
			selection.append(DbProvider.Qualified.RESPONSES_SURVEY_ID + "='" + mFragment.getSurveyId() + "' AND ");

		// Set the date filter selection
		selection.append(Responses.RESPONSE_TIME + " >= " + mFragment.getStartBounds() + " AND ");
		selection.append(Responses.RESPONSE_TIME + " <= " + mFragment.getEndBounds());

		if(mSelection != null)
			selection.append(" AND " + mSelection);

		return new CursorLoader(mFragment.getActivity(), Responses.CONTENT_URI, mProjection, selection.toString(), null, Responses.RESPONSE_TIME + " DESC");
	}
}