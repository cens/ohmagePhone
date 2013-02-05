package org.ohmage.fragments;


import org.achartengine.GraphicalView;
import org.ohmage.MobilityHelper;
import org.mobilizingcs.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.charts.Histogram;
import org.ohmage.charts.HistogramBase.HistogramRenderer;
import org.ohmage.loader.MobilityAggregateLoader;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;
import org.ohmage.mobility.glue.MobilityInterface;

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
import android.widget.TextView;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class RecentMobilityChartFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final int MAX_DATA_COLUMNS = 10;

	private static final int LOAD_MOBILITY_DATA = 0;

	private static final int LOAD_MOBILITY_BASELINE_AGGREGATE = 1;

	private static final int LOAD_MOBILITY_LASTWEEK_AGGREGATE = 2;

	private ViewGroup mContainer;
	private TextView mTodayStill;
	private TextView mTodayWalk;
	private TextView mTodayRun;
	private TextView mTodayDrive;

	private UserPreferencesHelper mSharedPreferences;

	private TextView mAggregateBaseline;

	private TextView mAggregateLastweek;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mSharedPreferences = new UserPreferencesHelper(getActivity());

		getLoaderManager().initLoader(LOAD_MOBILITY_DATA, null, this);
		getLoaderManager().initLoader(LOAD_MOBILITY_BASELINE_AGGREGATE, null, this);
		getLoaderManager().initLoader(LOAD_MOBILITY_LASTWEEK_AGGREGATE, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.recent_mobility_activity_fragment_layout, container, false);

		mContainer = (ViewGroup) view.findViewById(R.id.feedback_response_graph);
		mTodayStill = (TextView) view.findViewById(R.id.recent_mobility_still);
		mTodayWalk = (TextView) view.findViewById(R.id.recent_mobility_walk);
		mTodayRun = (TextView) view.findViewById(R.id.recent_mobility_run);
		mTodayDrive = (TextView) view.findViewById(R.id.recent_mobility_drive);

		mAggregateBaseline = (TextView) view.findViewById(R.id.aggregate_baseline_count);
		mAggregateLastweek = (TextView) view.findViewById(R.id.aggregate_lastweek_count);

		return view;
	}

	public interface MobilityQuery {
		String[] PROJECTION = {
				MobilityInterface.KEY_DAY,
				MobilityInterface.KEY_MODE,
				MobilityInterface.KEY_DURATION
		};
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch(id) {
			case LOAD_MOBILITY_DATA:
				return new CursorLoader(getActivity(), MobilityInterface.AGGREGATES_URI,
						MobilityQuery.PROJECTION,
						MobilityInterface.KEY_DAY + " <= date('now', 'localtime')" + " AND " +
						MobilityInterface.KEY_DAY + " > date('now', 'localtime', '-10 days') AND " +
						MobilityInterface.KEY_USERNAME + "=?", new String[] {
					MobilityHelper.getMobilityUsername(mSharedPreferences.getUsername())
				}, MobilityInterface.KEY_DAY + " DESC");
			case LOAD_MOBILITY_BASELINE_AGGREGATE:
				return new MobilityAggregateLoader(getActivity(),
						UserPreferencesHelper.getBaseLineStartTime(getActivity()),
						UserPreferencesHelper.getBaseLineEndTime(getActivity()),
						mSharedPreferences.getUsername());
			case LOAD_MOBILITY_LASTWEEK_AGGREGATE:
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, -7);
				long endtime = cal.getTimeInMillis();
				cal.add(Calendar.DATE, -7);
				return new MobilityAggregateLoader(getActivity(),
						cal.getTimeInMillis(),
						endtime,
						mSharedPreferences.getUsername());
		}
		return null;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if(LOAD_MOBILITY_DATA == loader.getId()) {
			mTodayStill.setText(null);
			mTodayWalk.setText(null);
			mTodayRun.setText(null);
			mTodayDrive.setText(null);
		} if(LOAD_MOBILITY_BASELINE_AGGREGATE == loader.getId()) {
			mAggregateBaseline.setText(R.string.empty_value);
		} else if(LOAD_MOBILITY_LASTWEEK_AGGREGATE == loader.getId()) {
			mAggregateLastweek.setText(R.string.empty_value);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if(data == null)
			return;

		// Move to the beginning of the data
		data.moveToPosition(-1);

		if(LOAD_MOBILITY_DATA == loader.getId()) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			List<FeedbackItem> values = new LinkedList<FeedbackItem>();

			for (int i = 0; i < MAX_DATA_COLUMNS; i++) {
				FeedbackItem point = new FeedbackItem();
				point.time = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * i;
				point.value = 0.0;
				// Count up the data for each day
				try {
					while(data.moveToNext()
							&& DateUtils.isToday(formatter.parse(data.getString(0)).getTime() + DateUtils.DAY_IN_MILLIS * i)) {

						String mode = data.getString(1);
						if(MobilityInterface.WALK.equals(mode) || MobilityInterface.RUN.equals(mode) || MobilityInterface.BIKE.equals(mode))
							point.value+= data.getDouble(2);

						// If this came from today we should set the correct textview
						if (DateUtils.isToday(formatter.parse(data.getString(0)).getTime())) {
							if (MobilityInterface.STILL.equals(mode))
								mTodayStill.setText(formatTimeAmount(data.getLong(2)));
							else if (MobilityInterface.WALK.equals(mode))
								mTodayWalk.setText(formatTimeAmount(data.getLong(2)));
							else if (MobilityInterface.RUN.equals(mode))
								mTodayRun.setText(formatTimeAmount(data.getLong(2)));
							else if (MobilityInterface.DRIVE.equals(mode))
								mTodayDrive.setText(formatTimeAmount(data.getLong(2)));
						}
					}
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Change the value from ms to hours
				point.value /= DateUtils.HOUR_IN_MILLIS;
				values.add(point);
				data.moveToPrevious();
			}

			HistogramRenderer renderer = new HistogramRenderer(getActivity());
			renderer.setMargins(new int[] {
					Utilities.dpToPixels(20), Utilities.dpToPixels(30), Utilities.dpToPixels(10), Utilities.dpToPixels(10)
			});
			renderer.setYLabels(5);
			renderer.setYAxisMin(0);
			renderer.setYTitle("# of Hours");
			renderer.setInScroll(true);

			mContainer.removeAllViews();
			mContainer.addView(new GraphicalView(getActivity(), new Histogram(getActivity(), renderer, values, MAX_DATA_COLUMNS)));
		} else if(LOAD_MOBILITY_BASELINE_AGGREGATE == loader.getId()) {
			long time = ((MobilityAggregateLoader) loader).getTimeActive();
			if(time != 0)
				mAggregateBaseline.setText(formatTimeAmount(time));
		} else if(LOAD_MOBILITY_LASTWEEK_AGGREGATE == loader.getId()) {
			long time = ((MobilityAggregateLoader) loader).getTimeActive();
			if(time != 0)
				mAggregateLastweek.setText(formatTimeAmount(time));
		}
	}

	/**
	 * Formats the time amount if its less than one hour it will return the
	 * minutes. If it is greater than an hour it will return the number of
	 * hours with one decimal point.
	 * 
	 * @param time
	 * @return
	 */
	private CharSequence formatTimeAmount(long time) {
		if(time / DateUtils.HOUR_IN_MILLIS < 1) {
			return (time / DateUtils.MINUTE_IN_MILLIS) + " min";
		} else {
			NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.US);
			numberFormatter.setMaximumFractionDigits(1);
			return numberFormatter.format(time / new Double(DateUtils.HOUR_IN_MILLIS)) + " h";
		}
	}
}