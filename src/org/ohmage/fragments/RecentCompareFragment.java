package org.ohmage.fragments;

import org.ohmage.NIHConfig;
import org.ohmage.R;
import org.ohmage.adapters.ComparisonAdapter;
import org.ohmage.adapters.ComparisonAdapter.ComparisonAdapterItem;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.HashMap;
import java.util.LinkedList;

public class RecentCompareFragment extends PromptFeedbackListFragment {

	private static final String TAG = "RecentCompareFragment";

	/**
	 * Creates a new instance of the recent chart fragment
	 * @return the chart fragment
	 */
	public static RecentCompareFragment newInstance() {
		return new RecentCompareFragment();
	}

	private ComparisonAdapter mAdapter;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setEmptyText(getActivity().getString(R.string.charts_no_data));

		mAdapter = new ComparisonAdapter(getActivity());
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		startCampaignRead();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView lv = (ListView) view.findViewById(android.R.id.list);
		View header = ComparisonAdapterItem.createLegendView(getActivity());
		LinearLayout headerContainer = new LinearLayout(getActivity());
		int padding = getResources().getDimensionPixelSize(R.dimen.gutter);
		headerContainer.setPadding(padding, padding, padding, 0);
		headerContainer.addView(header);

		lv.addHeaderView(headerContainer, null, false);

		lv.setDividerHeight(0);
		return view;
	}

	@Override
	public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {
		mAdapter.clear();

		for(String key : NIHConfig.PROMPT_LIST) {
			LinkedList<FeedbackItem> list = feedbackItems.get(NIHConfig.getPrompt(key));
			if(list != null && !list.isEmpty()) {
				mAdapter.add(new ComparisonAdapterItem(getActivity(), NIHConfig.getExtraPromptData(key), list));
			}
		}

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.clear();
	}
}