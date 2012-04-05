
package org.ohmage.activity;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.achartengine.GraphicalView;
import org.ohmage.R;
import org.ohmage.charts.Histogram;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
import org.ohmage.controls.ActionBarControl;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.fragments.RecentChartFragment;
import org.ohmage.fragments.RecentCompareFragment;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;
import org.ohmage.responsesync.ResponseSyncService;
import org.ohmage.ui.BaseActivity;
import org.ohmage.ui.TabsAdapter;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TabHost;

import java.util.LinkedList;
import java.util.List;

public class FeedbackActivity extends BaseActivity {

	private static final int ACTION_RESPONSESYNC = 0;

	TabHost mTabHost;
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.tab_layout);
		setActionBarShadowVisibility(false);

		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mViewPager = (ViewPager) findViewById(R.id.pager);

		mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

		mTabsAdapter.addTab("Compare", RecentCompareFragment.class, null);
		mTabsAdapter.addTab("Charts", RecentChartFragment.class, null);
		mTabsAdapter.addTab("Responses", RecentResponsesFragment.class, null);

		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}

		// add a sync command to the response history action bar
		ActionBarControl actionBar = getActionBar();
		actionBar.clearActionBarCommands();

		actionBar.addActionBarCommand(ACTION_RESPONSESYNC, "sync responses", R.drawable.btn_title_refresh);

		actionBar.setOnActionListener(new ActionListener() {
			@Override
			public void onActionClicked(int commandID) {
				switch (commandID) {
					case ACTION_RESPONSESYNC:
						Intent i = new Intent(FeedbackActivity.this, ResponseSyncService.class);
						i.putExtra(ResponseSyncService.EXTRA_INTERACTIVE, true);
						WakefulIntentService.sendWakefulWork(FeedbackActivity.this, i);
						break;
				}
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("tab", mTabHost.getCurrentTabTag());
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
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
					startActivity(new Intent(getActivity(), ResponseHistoryActivity.class)
					.putExtra(ResponseHistoryActivity.EXTRA_SHOW_MAP, true));
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
