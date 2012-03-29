
package org.ohmage.fragments;

import org.achartengine.chart.XYChart;
import org.ohmage.R;
import org.ohmage.adapters.SimpleChartListAdapter;
import org.ohmage.loader.PromptFeedbackLoaderCallbacks;
import org.ohmage.loader.PromptFeedbackLoaderCallbacks.PromptFeedbackLoaderCallbacksListener;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public abstract class PromptFeedbackFragment<T extends SimpleChartListAdapter<? extends SimpleChartListAdapter.ChartItem<? extends XYChart>>> extends Fragment implements
LoaderManager.LoaderCallbacks<Cursor>, PromptFeedbackLoaderCallbacksListener {

	private PromptFeedbackLoaderCallbacks mPromptFeedbackHelper;
	protected LinearLayout mContainer;

	protected T mAdapter;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter = createAdapter();

		startCampaignRead();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mPromptFeedbackHelper = new PromptFeedbackLoaderCallbacks(this);
		mPromptFeedbackHelper.setOnPromptReadFinishedListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.recent_chart_fragment_layout, container, false);
		mContainer = (LinearLayout) view.findViewById(R.id.recent_chart_container);
		return view;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return mPromptFeedbackHelper.onCreateLoader(id, args);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mPromptFeedbackHelper.onLoadFinished(loader, cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.clear();
	}

	protected void startCampaignRead() {
		mPromptFeedbackHelper.start();
	}

	@Override
	public String[] getPromptList() {
		return null;
	}

	public abstract T createAdapter();
}
