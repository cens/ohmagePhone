
package org.ohmage.activity;

import org.achartengine.GraphicalView;
import org.ohmage.R;
import org.ohmage.charts.Histogram;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.fragments.RecentChartFragment;
import org.ohmage.fragments.RecentCompareFragment;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;
import org.ohmage.ui.BaseActivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class FeedbackActivity extends BaseActivity {

	private static final String TAG = "FeedbackActivity";

	/**
	 * true when the charts have been loaded
	 */
	private boolean mChartsLoaded;

	/**
	 * true when the responses have been loaded
	 */
	private boolean mResponsesLoaded;

	/**
	 * true when the compare fragment has been loaded
	 */
	private boolean mCompareLoaded;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback_layout);

		if (getSupportFragmentManager().findFragmentById(R.id.feedback_compare_container) == null) {
			Fragment compareFragment = new RecentCompareFragment() {
				@Override
				public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {
					super.onPromptReadFinished(feedbackItems);
					mCompareLoaded = true;
					maybeShowContent();
				}
			};
			getSupportFragmentManager().beginTransaction().add(R.id.feedback_compare_container, compareFragment).commit();
		}

		if (getSupportFragmentManager().findFragmentById(R.id.feedback_chart_container) == null) {
			Fragment chartFragment = new RecentChartFragment() {
				@Override
				public void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {
					super.onPromptReadFinished(feedbackItems);
					mChartsLoaded = true;
					maybeShowContent();
				}
			};
			getSupportFragmentManager().beginTransaction().add(R.id.feedback_chart_container, chartFragment).commit();
		}

		if (getSupportFragmentManager().findFragmentById(R.id.feedback_responses_container) == null) {
			Fragment chartFragment = new RecentResponsesFragment() {
				@Override
				public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
					super.onLoadFinished(loader, data);
					mResponsesLoaded = true;
					maybeShowContent();
				}
			};
			getSupportFragmentManager().beginTransaction().add(R.id.feedback_responses_container, chartFragment).commit();
		}
	}

	private void maybeShowContent() {
		setLoadingVisibility(!mChartsLoaded || !mResponsesLoaded || !mCompareLoaded);
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		setLoadingVisibility(true);
	}

	public static class RecentResponsesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

		private static final int MAX_DATA_COLUMNS = 10;

		private static final int LOAD_RESPONSES = 0;

		private ViewGroup mContainer;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			getLoaderManager().initLoader(LOAD_RESPONSES, null, this);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.recent_responses_fragment_layout, container, false);

			mContainer = (ViewGroup) view.findViewById(R.id.feedback_response_graph);
			Button b = (Button) view.findViewById(R.id.feedback_response_history_calendar);
			b.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					startActivity(new Intent(getActivity(), ResponseHistoryActivity.class));
				}
			});
			b = (Button) view.findViewById(R.id.feedback_response_history_map);
			b.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					startActivity(new Intent(getActivity(), ResponseHistoryActivity.class).putExtra(ResponseHistoryActivity.EXTRA_SHOW_MAP, true));
				}
			});

			return view;
		}


		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new CursorLoader(getActivity(), Responses.CONTENT_URI,
					new String[] { Responses.RESPONSE_TIME },
					Responses.RESPONSE_TIME + " < " + System.currentTimeMillis(), null, Responses.RESPONSE_TIME + " DESC");
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// Nothing to do?
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			List<FeedbackItem> values = new LinkedList<FeedbackItem>();

			// Move to the beginning of the data
			data.moveToPosition(-1);

			for (int i = 0; i < MAX_DATA_COLUMNS; i++) {
				FeedbackItem point = new FeedbackItem();
				point.time = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * i;
				point.value = 0.0;
				// Count up the data for each day
				while (data.moveToNext()
						&& DateUtils.isToday(data.getLong(0) + DateUtils.DAY_IN_MILLIS * i)) {
					point.value++;
				}
				values.add(point);
				data.moveToPrevious();
			}

			HistogramRenderer renderer = new HistogramRenderer(getActivity());
			renderer.setMargins(new int[] {
					30, 35, 15, 30
			});
			renderer.setYLabels(5);
			renderer.setYAxisMin(0);
			renderer.setYTitle("# of Responses");
			renderer.setInScroll(true);

			mContainer.removeAllViews();
			mContainer.addView(new GraphicalView(getActivity(), new Histogram(getActivity(), renderer, values)));
		}
	}
}
