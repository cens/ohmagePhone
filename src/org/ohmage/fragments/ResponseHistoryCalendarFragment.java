package org.ohmage.fragments;

import edu.ucla.cens.systemlog.Analytics;

import org.ohmage.R;
import org.ohmage.activity.ResponseListActivity;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;
import org.ohmage.ui.OhmageFilterable.CampaignSurveyFilter;
import org.ohmage.ui.OhmageFilterable.TimeFilter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * <p>The {@link ResponseHistoryCalendarFragment} shows a calendar view of a single month of responses.</p>
 * 
 * <p>The {@link ResponseHistoryCalendarFragment} accepts {@link CampaignFilter#EXTRA_CAMPAIGN_URN}, {@link CampaignSurveyFilter# EXTRA_SURVEY_ID},
 *  {@link TimeFilter#EXTRA_MONTH}, and {@link TimeFilter#EXTRA_YEAR} as extras</p>
 * @author cketcham
 *
 */
public class ResponseHistoryCalendarFragment extends FilterableFragment {

	private MonthCellAdapter mAdapter;

	private TextSwitcher mTextSwitcher;
	private GridView mCalendarView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new MonthCellAdapter(getActivity(), R.id.calendar_gridcell_num_responses);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle args) {
		View view = inflater.inflate(R.layout.calendar_view, container, false);

		//Summary Text Switcher
		mTextSwitcher = (TextSwitcher) view.findViewById(R.id.summary_text_switcher);
		mTextSwitcher.setFactory(new ViewSwitcher.ViewFactory() {

			@Override
			public View makeView() {
				TextView textview = new TextView(getActivity());
				textview.setTextColor(Color.WHITE);
				textview.setBackgroundColor(Color.DKGRAY);
				textview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
				textview.setTypeface(null, Typeface.ITALIC);
				textview.setGravity(Gravity.CENTER);

				int padding_in_dp = 3;
				final float scale = getResources().getDisplayMetrics().density;
				int padding_in_px = (int) (padding_in_dp * scale + 0.5f);

				textview.setPadding(0, 0, 0, padding_in_px);
				return textview;
			}
		});
		mTextSwitcher.setInAnimation(getActivity(), android.R.anim.fade_in);
		mTextSwitcher.setOutAnimation(getActivity(), android.R.anim.fade_out);

		mCalendarView = (GridView) view.findViewById(R.id.calendar);

		mCalendarView.setAdapter(mAdapter);

		return view;
	}

	protected static class ResponseCalendarQuery {
		public static final String[] PROJECTION = new String[] {
			Responses._ID,
			Responses.RESPONSE_TIME
		};

		public static final int ID = 0;
		public static final int TIME = 1;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new ResponseLoader(this, ResponseCalendarQuery.PROJECTION).onCreateLoader(id, args);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.setMonth(getMonth(), getYear());
		mAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	public void onDateClicked(int date) {
		Intent intent = new Intent(getActivity(), ResponseListActivity.class);
		if(getCampaignUrn() != null)
			intent.putExtra(CampaignFilter.EXTRA_CAMPAIGN_URN, getCampaignUrn());
		if(getSurveyId() != null)
			intent.putExtra(CampaignSurveyFilter.EXTRA_SURVEY_ID, getSurveyId());
		intent.putExtra(TimeFilter.EXTRA_DAY, date);
		intent.putExtra(TimeFilter.EXTRA_MONTH, getMonth());
		intent.putExtra(TimeFilter.EXTRA_YEAR, getYear());
		startActivity(intent);       
	}

	public class MonthCellAdapter extends BaseAdapter implements OnClickListener {

		private static final String tag = "GridCellAdapter";
		private final Context mContext;

		private final ArrayList<String> list = new ArrayList<String>();
		private HashMap<String,Integer> eventsPerMonthMap = new HashMap<String,Integer>();

		private static final int DAY_OFFSET = 1;
		private final String[] weekdays = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
		private final String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
		private final int[] daysOfMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

		private Button numOfResponsesDisplay;
		private TextView dateDisplay;

		private final int currentDayOfMonth;
		private final int currentWeekDay;
		private final int currentMonth;

		private int mMonth;
		private int mYear;
		private Cursor mCursor;

		// Days in Current Month
		public MonthCellAdapter(Context context, int textViewResourceId) {
			super();
			mContext = context;

			Calendar calendar = Calendar.getInstance();
			currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
			currentWeekDay = calendar.get(Calendar.DAY_OF_WEEK);
			currentMonth = calendar.get(Calendar.MONTH);
		}

		public void setMonth(int month, int year) {
			mMonth = month;
			mYear = year;
		}

		public void swapCursor(Cursor cursor) {
			if(mCursor != cursor && mCursor != null)
				mCursor.close();

			mCursor = cursor;

			if(mCursor != null) {
				printMonth(mMonth, mYear);
				eventsPerMonthMap = findNumberOfEventsPerMonth(mCursor);
				notifyDataSetChanged();
			}
		}

		/**
		 * Prints Month
		 */
		private void printMonth(int mm, int yy) {
			list.clear();

			// The number of days to leave blank at
			// the start of this month.
			int trailingSpaces = 0;
			int daysInPrevMonth = 0;
			int prevMonth = 0;
			int prevYear = 0;
			int nextMonth = 0;
			int nextYear = 0;

			int currentSelectedMonth = mm;
			int daysInMonth = getNumberOfDaysOfMonth(currentSelectedMonth);

			GregorianCalendar cal = new GregorianCalendar(yy, currentSelectedMonth, 1);

			if (currentSelectedMonth == 11) {
				prevMonth = currentSelectedMonth - 1;
				daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
				nextMonth = 0;
				prevYear = yy;
				nextYear = yy + 1;
			} else if (currentSelectedMonth == 0) {
				prevMonth = 11;
				prevYear = yy - 1;
				nextYear = yy;
				daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
				nextMonth = 1;
			} else {
				prevMonth = currentSelectedMonth - 1;
				nextMonth = currentSelectedMonth + 1;
				nextYear = yy;
				prevYear = yy;
				daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
			}

			int currentWeekDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
			trailingSpaces = currentWeekDay;

			if (cal.isLeapYear(cal.get(Calendar.YEAR)) && mm == 2)
				++daysInMonth;

			// Trailing Month days
			for (int i = 0; i < trailingSpaces; i++)
				list.add(String.valueOf((daysInPrevMonth - trailingSpaces + DAY_OFFSET) + i) + "-OUTOFTHISMONTH" + "-" + getMonthAsString(prevMonth) + "-" + prevYear);

			// Current Month Days
			for (int i = 1; i <= daysInMonth; i++) {
				if (currentMonth == currentSelectedMonth && i == getCurrentDayOfMonth())
					list.add(String.valueOf(i) + "-TODAY" + "-" + getMonthAsString(currentSelectedMonth) + "-" + yy);
				else
					list.add(String.valueOf(i) + "-THISMONTH" + "-" + getMonthAsString(currentSelectedMonth) + "-" + yy);
			}

			// Leading Month days
			for (int i = 0; i < list.size() % 7; i++)
				list.add(String.valueOf(i + 1) + "-OUTOFTHISMONTH" + "-" + getMonthAsString(nextMonth) + "-" + nextYear);
		}

		/**
		 * NOTE: YOU NEED TO IMPLEMENT THIS PART Given the YEAR, MONTH, retrieve
		 * ALL entries from a SQLite database for that month. Iterate over the
		 * List of All entries, and get the dateCreated, which is converted into
		 * day.
		 * 
		 * @param year
		 * @param month
		 * @return
		 */
		private HashMap<String, Integer> findNumberOfEventsPerMonth(Cursor cursor)
		{
			ContentResolver cr = getActivity().getContentResolver();

			//Create Uri
			Uri uri;
			if(getCampaignUrn() == null){
				uri = Responses.CONTENT_URI;
			} else {
				if(getSurveyId() == null)
					uri = Campaigns.buildResponsesUri(getCampaignUrn());				    		
				else
					uri = Campaigns.buildResponsesUri(getCampaignUrn(), getSurveyId());
			}

			HashMap<String, Integer> map = new HashMap<String, Integer>();
			Calendar cal = Calendar.getInstance();

			int numOfResponse = 0;
			for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()){
				Long time = cursor.getLong(ResponseCalendarQuery.TIME);
				Log.i(tag, "Response time: "+time.toString());
				cal.setTimeInMillis(time);
				Integer responseDay = new Integer(cal.get(Calendar.DAY_OF_MONTH));
				if(map.containsKey(responseDay.toString())){
					Integer value = map.get(responseDay.toString()) + 1;
					map.put(responseDay.toString(), value);
				}
				else{
					map.put(responseDay.toString(),1);
				}
				numOfResponse ++;
			}

			Cursor responseCursorTotal = cr.query(uri, new String[] { "_id" }, null, null, null);
			mTextSwitcher.setText(getMonthAsString(mMonth) + ": " + numOfResponse + " / Total: " + responseCursorTotal.getCount());
			responseCursorTotal.close();

			return map;
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View row = convertView;
			if (row == null)
			{
				LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = inflater.inflate(R.layout.calendar_view_gridcell, parent, false);
			}

			// Get a reference to the Day gridcell
			numOfResponsesDisplay = (Button) row.findViewById(R.id.calendar_gridcell_num_responses);

			// ACCOUNT FOR SPACING

			// Log.d(tag, "Current Day: " + getCurrentDayOfMonth());
			String[] day_color = list.get(position).split("-");
			String theday = day_color[0];

			// Set the Day GridCell
			if (eventsPerMonthMap.containsKey(theday)) {
				numOfResponsesDisplay.setText(eventsPerMonthMap.get(theday).toString());
				numOfResponsesDisplay.setTag(theday);
				numOfResponsesDisplay.setOnClickListener(this);
			} else {
				numOfResponsesDisplay.setText(null);
				numOfResponsesDisplay.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						Analytics.widget(v, "Calendar Date (No responses)");
					}
				});
			}
			dateDisplay = (TextView) row.findViewById(R.id.calendar_gridcell_date);
			dateDisplay.setBackgroundColor(Color.WHITE);
			dateDisplay.setText(theday);


			numOfResponsesDisplay.setTextColor(Color.BLACK);
			numOfResponsesDisplay.setBackgroundResource(R.drawable.calendar_button_selector);

			if (day_color[1].equals("OUTOFTHISMONTH"))
			{
				dateDisplay.setTextColor(Color.LTGRAY);
				dateDisplay.setBackgroundColor(Color.WHITE);
				numOfResponsesDisplay.setTextColor(Color.WHITE);
				numOfResponsesDisplay.setBackgroundColor(Color.WHITE);
			}
			if (day_color[1].equals("THISMONTH"))
			{
				dateDisplay.setTextColor(Color.WHITE);
				dateDisplay.setBackgroundColor(Color.DKGRAY);
			}
			if (day_color[1].equals("TODAY"))
			{
				dateDisplay.setTextColor(Color.WHITE);
				dateDisplay.setBackgroundColor(Color.DKGRAY);
				numOfResponsesDisplay.setBackgroundResource(R.drawable.calendar_button_selector_today);
			}
			return row;
		}
		@Override
		public void onClick(View view) {
			Analytics.widget(view, "Calendar Date");
			if(view.getTag() instanceof String) {
				try {
					onDateClicked(Integer.valueOf((String) view.getTag()));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}

		public int getCurrentDayOfMonth() {
			return currentDayOfMonth;
		}

		public int getCurrentWeekDay() {
			return currentWeekDay;
		}
		private String getMonthAsString(int i) {
			return months[i];
		}

		private int getNumberOfDaysOfMonth(int i) {
			return daysOfMonth[i];
		}

		@Override
		public String getItem(int position) {
			return list.get(position);
		}

		@Override
		public int getCount() {
			return list.size();
		}
	}
}