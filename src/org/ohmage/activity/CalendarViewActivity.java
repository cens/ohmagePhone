package org.ohmage.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.ohmage.R;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaign;
import org.ohmage.db.DbContract.Survey;
import org.ohmage.feedback.visualization.ResponseHistory;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class CalendarViewActivity extends ResponseHistory implements OnClickListener
{
	private static final String tag = "CalendarViewActivity";

	private Button mCurrentMonthButton;
	private Button mPrevMonthButton;
	private Button mNextMonthButton;
	private GridView calendarView;
	private GridCellAdapter adapter;
	private Calendar _calendar;
	private int mSelectedMonth, mSelectedYear;
	private final DateFormat dateFormatter = new DateFormat();
	private static final String dateTemplate = "MMMM yyyy";
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calendar_view);

		setupFilters();
		
		//Calendar
		_calendar = Calendar.getInstance(Locale.getDefault());
		mSelectedMonth = _calendar.get(Calendar.MONTH) + 1;
		mSelectedYear = _calendar.get(Calendar.YEAR);
		Log.d(tag, "Calendar Instance:= " + "Month: " + mSelectedMonth + " " + "Year: " + mSelectedYear);

		mPrevMonthButton = (Button) this.findViewById(R.id.prevMonth);
		mCurrentMonthButton = (Button) this.findViewById(R.id.currentMonth);
		mNextMonthButton = (Button) this.findViewById(R.id.nextMonth);
		
		mPrevMonthButton.setOnClickListener(this);
		mCurrentMonthButton.setText(DateFormat.format(dateTemplate, _calendar.getTime()));
		mNextMonthButton.setOnClickListener(this);

		calendarView = (GridView) this.findViewById(R.id.calendar);

		adapter = new GridCellAdapter(getApplicationContext(), R.id.calendar_gridcell_num_responses, mSelectedMonth, mSelectedYear);
		adapter.notifyDataSetChanged();
		calendarView.setAdapter(adapter);
	}
		
	/**
	 * 
	 * @param month
	 * @param year
	 */
	private void setGridCellAdapterToDate(int month, int year)
	{
		adapter = new GridCellAdapter(getApplicationContext(), R.id.calendar_gridcell_num_responses, month, year);
		_calendar.set(year, month - 1, _calendar.get(Calendar.DAY_OF_MONTH));
		mCurrentMonthButton.setText(dateFormatter.format(dateTemplate, _calendar.getTime()));
		adapter.notifyDataSetChanged();
		calendarView.setAdapter(adapter);
	}

	@Override
	public void onClick(View v)
		{
			if (v == mPrevMonthButton)
				{
					if (mSelectedMonth <= 1)
						{
							mSelectedMonth = 12;
							mSelectedYear--;
						}
					else
						{
							mSelectedMonth--;
						}
					Log.d(tag, "Setting Prev Month in GridCellAdapter: " + "Month: " + mSelectedMonth + " Year: " + mSelectedYear);
					setGridCellAdapterToDate(mSelectedMonth, mSelectedYear);
				}
			if (v == mNextMonthButton)
				{
					if (mSelectedMonth > 11)
						{
							mSelectedMonth = 1;
							mSelectedYear++;
						}
					else
						{
							mSelectedMonth++;
						}
					Log.d(tag, "Setting Next Month in GridCellAdapter: " + "Month: " + mSelectedMonth + " Year: " + mSelectedYear);
					setGridCellAdapterToDate(mSelectedMonth, mSelectedYear);
				}
		}

	@Override
	public void onDestroy()
		{
			Log.d(tag, "Destroying View ...");
			super.onDestroy();
		}

	// ///////////////////////////////////////////////////////////////////////////////////////
	// Inner Class
	public class GridCellAdapter extends BaseAdapter implements OnClickListener
		{
			private static final String tag = "GridCellAdapter";
			private final Context _context;

			private final List<String> list;
			private static final int DAY_OFFSET = 1;
			private final String[] weekdays = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
			private final String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
			private final int[] daysOfMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
			private final int month, year;
			private int daysInMonth, prevMonthDays;
			private int currentDayOfMonth;
			private int currentWeekDay;
			private Button numOfResponsesDisplay;
			private TextView dateDisplay;
			private final HashMap eventsPerMonthMap;
			private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MMM-yyyy");
			private int currentMonth;

			// Days in Current Month
			public GridCellAdapter(Context context, int textViewResourceId, int month, int year)
				{
					super();
					this._context = context;
					this.list = new ArrayList<String>();
					this.month = month;
					this.year = year;
					this.currentMonth = Calendar.getInstance().get(Calendar.MONTH);

					Log.d(tag, "==> Passed in Date FOR Month: " + month + " " + "Year: " + year);
					Calendar calendar = Calendar.getInstance();
					setCurrentDayOfMonth(calendar.get(Calendar.DAY_OF_MONTH));
					setCurrentWeekDay(calendar.get(Calendar.DAY_OF_WEEK));
					Log.d(tag, "New Calendar:= " + calendar.getTime().toString());
					Log.d(tag, "CurrentDayOfWeek :" + getCurrentWeekDay());
					Log.d(tag, "CurrentDayOfMonth :" + getCurrentDayOfMonth());

					// Print Month
					printMonth(month, year);

					// Find Number of Events
					eventsPerMonthMap = findNumberOfEventsPerMonth(year, month);
				}
			private String getMonthAsString(int i)
				{
					return months[i];
				}

			private String getWeekDayAsString(int i)
				{
					return weekdays[i];
				}

			private int getNumberOfDaysOfMonth(int i)
				{
					return daysOfMonth[i];
				}

			public String getItem(int position)
				{
					return list.get(position);
				}

			@Override
			public int getCount()
				{
					return list.size();
				}

			/**
			 * Prints Month
			 * 
			 * @param mm
			 * @param yy
			 */
			private void printMonth(int mm, int yy)
				{
					Log.d(tag, "==> printMonth: mm: " + mm + " " + "yy: " + yy);
					// The number of days to leave blank at
					// the start of this month.
					int trailingSpaces = 0;
					int leadSpaces = 0;
					int daysInPrevMonth = 0;
					int prevMonth = 0;
					int prevYear = 0;
					int nextMonth = 0;
					int nextYear = 0;

					int currentSelectedMonth = mm - 1;
					String currentMonthName = getMonthAsString(currentSelectedMonth);
					daysInMonth = getNumberOfDaysOfMonth(currentSelectedMonth);

					Log.d(tag, "Current Month: " + " " + currentMonthName + " having " + daysInMonth + " days.");

					// Gregorian Calendar : MINUS 1, set to FIRST OF MONTH
					GregorianCalendar cal = new GregorianCalendar(yy, currentSelectedMonth, 1);
					Log.d(tag, "Gregorian Calendar:= " + cal.getTime().toString());

					if (currentSelectedMonth == 11)
						{
							prevMonth = currentSelectedMonth - 1;
							daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
							nextMonth = 0;
							prevYear = yy;
							nextYear = yy + 1;
							Log.d(tag, "*->PrevYear: " + prevYear + " PrevMonth:" + prevMonth + " NextMonth: " + nextMonth + " NextYear: " + nextYear);
						}
					else if (currentSelectedMonth == 0)
						{
							prevMonth = 11;
							prevYear = yy - 1;
							nextYear = yy;
							daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
							nextMonth = 1;
							Log.d(tag, "**--> PrevYear: " + prevYear + " PrevMonth:" + prevMonth + " NextMonth: " + nextMonth + " NextYear: " + nextYear);
						}
					else
						{
							prevMonth = currentSelectedMonth - 1;
							nextMonth = currentSelectedMonth + 1;
							nextYear = yy;
							prevYear = yy;
							daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
							Log.d(tag, "***---> PrevYear: " + prevYear + " PrevMonth:" + prevMonth + " NextMonth: " + nextMonth + " NextYear: " + nextYear);
						}

					// Compute how much to leave before before the first day of the
					// month.
					// getDay() returns 0 for Sunday.
					int currentWeekDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
					trailingSpaces = currentWeekDay;

					Log.d(tag, "Week Day:" + currentWeekDay + " is " + getWeekDayAsString(currentWeekDay));
					Log.d(tag, "No. Trailing space to Add: " + trailingSpaces);
					Log.d(tag, "No. of Days in Previous Month: " + daysInPrevMonth);

					if (cal.isLeapYear(cal.get(Calendar.YEAR)) && mm == 1)
						{
							++daysInMonth;
						}

					// Trailing Month days
					for (int i = 0; i < trailingSpaces; i++)
						{
							Log.d(tag, "PREV MONTH:= " + prevMonth + " => " + getMonthAsString(prevMonth) + " " + String.valueOf((daysInPrevMonth - trailingSpaces + DAY_OFFSET) + i));
							list.add(String.valueOf((daysInPrevMonth - trailingSpaces + DAY_OFFSET) + i) + "-OUTOFTHISMONTH" + "-" + getMonthAsString(prevMonth) + "-" + prevYear);
						}

					// Current Month Days
					for (int i = 1; i <= daysInMonth; i++)
						{
							Log.d(currentMonthName, String.valueOf(i) + " " + getMonthAsString(currentSelectedMonth) + " " + yy);
							if (currentMonth == currentSelectedMonth && i == getCurrentDayOfMonth())
								{
									list.add(String.valueOf(i) + "-TODAY" + "-" + getMonthAsString(currentSelectedMonth) + "-" + yy);
								}
							else
								{
									list.add(String.valueOf(i) + "-THISMONTH" + "-" + getMonthAsString(currentSelectedMonth) + "-" + yy);
								}
						}

					// Leading Month days
					for (int i = 0; i < list.size() % 7; i++)
						{
							Log.d(tag, "NEXT MONTH:= " + getMonthAsString(nextMonth));
							list.add(String.valueOf(i + 1) + "-OUTOFTHISMONTH" + "-" + getMonthAsString(nextMonth) + "-" + nextYear);
						}
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
			private HashMap findNumberOfEventsPerMonth(int year, int month)
				{
				    ContentResolver cr = CalendarViewActivity.this.getContentResolver();
				    		
				    //Cursor cursor = cr.query(queryUri, null, null, null, null);

					HashMap map = new HashMap<String, Integer>();
					 DateFormat dateFormatter2 = new DateFormat();
					 
					 //String day = dateFormatter2.format("dd").toString();
					 String day = "15";
					
					 if (map.containsKey(day))
					 {
					 Integer val = (Integer) map.get(day) + 1;
					 map.put(day, val);
					 }
					 else
					 {
					 map.put(day, 1);
					 }
					
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
							LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
							row = inflater.inflate(R.layout.calendar_view_gridcell, parent, false);
						}

					// Get a reference to the Day gridcell
					numOfResponsesDisplay = (Button) row.findViewById(R.id.calendar_gridcell_num_responses);
					numOfResponsesDisplay.setOnClickListener(this);

					// ACCOUNT FOR SPACING

					Log.d(tag, "Current Day: " + getCurrentDayOfMonth());
					String[] day_color = list.get(position).split("-");
					String theday = day_color[0];
					String themonth = day_color[2];
					String theyear = day_color[3];
					if ((!eventsPerMonthMap.isEmpty()) && (eventsPerMonthMap != null))
						{
							if (eventsPerMonthMap.containsKey(theday))
								{
									// Set the Day GridCell
									numOfResponsesDisplay.setText(theday);
									Log.d(tag, "Setting GridCell " + theday + "-" + themonth + "-" + theyear);
									
								}
						}
					dateDisplay = (TextView) row.findViewById(R.id.calendar_gridcell_date);
					dateDisplay.setBackgroundColor(Color.WHITE);
					dateDisplay.setText(theday);
					numOfResponsesDisplay.setTag(theday + "-" + themonth + "-" + theyear);

					if (day_color[1].equals("OUTOFTHISMONTH"))
						{
							dateDisplay.setTextColor(Color.LTGRAY);
							dateDisplay.setBackgroundColor(Color.WHITE);
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
							numOfResponsesDisplay.setBackgroundColor(Color.rgb(250, 229, 192));
						}
					return row;
				}
			@Override
			public void onClick(View view)
				{
					String date_month_year = (String) view.getTag();

					try
						{
							Date parsedDate = dateFormatter.parse(date_month_year);
							Toast.makeText(CalendarViewActivity.this, parsedDate.toString(), Toast.LENGTH_SHORT).show();
						}
					catch (ParseException e)
						{
							e.printStackTrace();
						}
				}

			public int getCurrentDayOfMonth()
				{
					return currentDayOfMonth;
				}

			private void setCurrentDayOfMonth(int currentDayOfMonth)
				{
					this.currentDayOfMonth = currentDayOfMonth;
				}
			public void setCurrentWeekDay(int currentWeekDay)
				{
					this.currentWeekDay = currentWeekDay;
				}
			public int getCurrentWeekDay()
				{
					return currentWeekDay;
				}
		}
}


