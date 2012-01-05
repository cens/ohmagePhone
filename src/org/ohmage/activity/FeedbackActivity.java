
package org.ohmage.activity;

import org.ohmage.ChartFragment;
import org.ohmage.R;
import org.ohmage.charts.Histogram;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.fragments.RecentChartFragment;
import org.ohmage.ui.BaseActivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;

import java.util.Arrays;

public class FeedbackActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "FeedbackActivity";

	/**
	 * true when the charts have been loaded
	 */
	private boolean mChartsLoaded;

	/**
	 * true when the responses have been loaded
	 */
	private boolean mResponsesLoaded;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback_layout);

		if (getSupportFragmentManager().findFragmentById(R.id.feedback_chart_container) == null) {
			Fragment chartFragment = new RecentChartFragment() {

				@Override
				public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
					super.onLoadFinished(loader, data);
					mChartsLoaded = true;
					maybeShowContent();
				}
			};
			getSupportFragmentManager().beginTransaction().add(R.id.feedback_chart_container, chartFragment).commit();

		}

		if (getSupportFragmentManager().findFragmentById(R.id.feedback_response_graph) == null) {
			ChartFragment f = ChartFragment.newInstance();
			getSupportFragmentManager().beginTransaction().add(R.id.feedback_response_graph, f).commit();
			getSupportLoaderManager().initLoader(0, null, this);
		}
	}

	private void maybeShowContent() {
		setLoadingVisibility(!mChartsLoaded || !mResponsesLoaded);
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		Button b = (Button) findViewById(R.id.feedback_charts_more);
		b.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(FeedbackActivity.this, ChartFeedbackActivity.class));
			}
		});
		b = (Button) findViewById(R.id.feedback_response_history_calendar);
		b.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(FeedbackActivity.this, ResponseHistoryActivity.class));
			}
		});
		b = (Button) findViewById(R.id.feedback_response_history_map);
		b.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(FeedbackActivity.this, ResponseHistoryActivity.class).putExtra(ResponseHistoryActivity.EXTRA_SHOW_MAP, true));
			}
		});

		setLoadingVisibility(true);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(FeedbackActivity.this, Responses.CONTENT_URI,
				new String[] { Responses.RESPONSE_TIME },
				null, null, Responses.RESPONSE_TIME + " DESC");
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// Nothing to do?
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		double[] values = new double[10];
		Arrays.fill(values, 0);

		for(int i=0;i<values.length;i++) {
			while(data.moveToNext() && DateUtils.isToday(data.getLong(0) + DateUtils.DAY_IN_MILLIS * i)) {
				values[i]++;
			}
			data.moveToPrevious();
		}

		HistogramRenderer renderer = new HistogramRenderer(FeedbackActivity.this);
		renderer.setMargins(new int[] {
				30, 35, 15, 30
		});
		renderer.setYLabels(5);
		renderer.setYTitle("# of Responses");
		renderer.setInScroll(true);

		ChartFragment chart = (ChartFragment) getSupportFragmentManager().findFragmentById(R.id.feedback_response_graph);
		chart.setChart(new Histogram(this, renderer, values));

		mResponsesLoaded = true;
		maybeShowContent();
	}
}
