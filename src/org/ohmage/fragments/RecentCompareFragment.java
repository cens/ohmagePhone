package org.ohmage.fragments;

import org.ohmage.NIHConfig;
import org.ohmage.NIHConfig.ExtraPromptData;
import org.ohmage.R;
import org.ohmage.adapters.ComparisonAdapter;
import org.ohmage.adapters.ComparisonAdapter.ComparisonAdapterItem;
import org.ohmage.adapters.ComparisonAdapter.ComparisonAdapterSubItem;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import java.util.HashMap;
import java.util.LinkedList;

public class RecentCompareFragment extends PromptFeedbackFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "RecentCompareFragment";

	/**
	 * Creates a new instance of the recent chart fragment
	 * @return the chart fragment
	 */
	public static RecentCompareFragment newInstance() {
		return new RecentCompareFragment();
	}

	private org.ohmage.adapters.ComparisonAdapter mAdapter;
	private TableLayout mContainer;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter = new ComparisonAdapter(getActivity());

		startCampaignRead();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.feedback_comparison_table_layout, container, false);
		mContainer = (TableLayout) view.findViewById(R.id.feedback_comparison_table);
		return view;
	}

	@Override
	public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {
		LinkedList<ComparisonAdapter> subData = new LinkedList<ComparisonAdapter>();

		for(String k : feedbackItems.keySet()) {
			ExtraPromptData extra = NIHConfig.getExtraPromptData(k);

			ComparisonAdapterItem item = new ComparisonAdapterItem(NIHConfig.getExtraPromptData(k).shortName);
			item.setData(getActivity(), feedbackItems.get(k), extra.getMapper(), ComparisonAdapterItem.AVG);
			mAdapter.add(item);

			ComparisonAdapter subAdapter = new ComparisonAdapter(getActivity());
			subData.add(subAdapter);

			for(int i=0;i<extra.valueLabels.length;i++){
				ComparisonAdapterSubItem item2 = new ComparisonAdapterSubItem(extra.valueLabels[i], i, getResources().getColor(extra.getColor()));
				item2.setData(getActivity(), feedbackItems.get(k), extra.getMapper(), ComparisonAdapterItem.AVG);
				subAdapter.add(item2);
			}
		}

		mContainer.removeAllViews();

		if(!mAdapter.isEmpty()) {
			// Inflate the header
			LayoutInflater.from(getActivity()).inflate(R.layout.feedback_comparison_title_row, mContainer);
			for(int i=0; i< mAdapter.getCount(); i++) {
				View v = mAdapter.getView(i, null, mContainer);
				mContainer.addView(v);
				final LinkedList<View> subViews = new LinkedList<View>();
				for(int j=0; j < subData.get(i).getCount(); j++) {
					subViews.add(subData.get(i).getView(j, null, mContainer));
					mContainer.addView(subViews.getLast());
				}
				v.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						for(View view : subViews)
							view.setVisibility((view.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE);
					}
				});
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.clear();
	}
}