package org.ohmage.activity;

import org.ohmage.R;
import org.ohmage.controls.DateFilterControl;
import org.ohmage.controls.FilterControl;
import org.ohmage.controls.FilterControl.FilterChangeListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Campaign;
import org.ohmage.feedback.visualization.ResponseHistory;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;
import org.ohmage.ui.OhmageFilterable.CampaignSurveyFilter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Pair;
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
import android.widget.ViewSwitcher.ViewFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RHCalendarViewActivity extends ResponseHistory implements OnClickListener, ViewFactory
{
	private static final String tag = "CalendarViewActivity";

	private TextView mNumResponseSummary;
	private Button mCurrentMonthButton;
	private Button mPrevMonthButton;
	private Button mNextMonthButton;
	private GridView calendarView;
	private GridCellAdapter adapter;
	private Calendar _calendar;
	private int mSelectedMonth, mSelectedYear;
	private final DateFormat dateFormatter = new DateFormat();
	private static final String dateTemplate = "MMMM yyyy";
	private FilterControl mCampaignFilter;
	private FilterControl mSurveyFilter;
	private DateFilterControl mDateFilter;
	private TextSwitcher mTextSwitcher;
	
	private final SimpleDateFormat clickDateFormatter = new SimpleDateFormat("dd-MMMM-yyyy");


	//TODO
	//Change current dateFilterControl to the generic DateFilterControl.
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calendar_view);

		//Num of Response Summary Text View
		mNumResponseSummary = (TextView) this.findViewById(R.id.num_response_summary);
		
		//Summary Text Switcher
		mTextSwitcher = (TextSwitcher) this.findViewById(R.id.summary_text_switcher);
		mTextSwitcher.setFactory(this);
		mTextSwitcher.setInAnimation(this, android.R.anim.fade_in);
		mTextSwitcher.setOutAnimation(this, android.R.anim.fade_out);

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

		//Filters
		setupFilters();

		adapter = new GridCellAdapter(getApplicationContext(), R.id.calendar_gridcell_num_responses, mSelectedMonth, mSelectedYear,
				mCampaignFilter.getValue(), mSurveyFilter.getValue());
		adapter.notifyDataSetChanged();
		calendarView.setAdapter(adapter);
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		RHTabHost.setCampaignFilterIndex(mCampaignFilter.getIndex());
		RHTabHost.setSurveyFilterIndex(mSurveyFilter.getIndex());

		Calendar cal = Calendar.getInstance();
		cal.set(mSelectedYear, mSelectedMonth-1, 1);
		RHTabHost.setDateFilterValue(cal);
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		String campaignUrn = getIntent().getStringExtra(RHTabHost.EXTRA_CAMPAIGN_URN);
		getIntent().removeExtra(RHTabHost.EXTRA_CAMPAIGN_URN);
		String surveyId = getIntent().getStringExtra(RHTabHost.EXTRA_SURVEY_ID);
		getIntent().removeExtra(RHTabHost.EXTRA_SURVEY_ID);

		if(campaignUrn != null){
			mCampaignFilter.setIndex(mCampaignFilter.getIndex(campaignUrn));
		}
		else{
			mCampaignFilter.setIndex(RHTabHost.getCampaignFilterIndex());
		}
		
		if(surveyId != null){
			mSurveyFilter.setIndex(mSurveyFilter.getIndex(campaignUrn + ":" + surveyId));
		}
		else{
			mSurveyFilter.setIndex(RHTabHost.getSurveyFilterIndex());
		}
		
		Calendar cal = RHTabHost.getDateFilterValue();
		mSelectedMonth = cal.get(Calendar.MONTH)+1;
		mSelectedYear = cal.get(Calendar.YEAR);
		mCurrentMonthButton.setText(DateFormat.format(dateTemplate, cal.getTime()));
		
		setGridCellAdapterToDate(
				mSelectedMonth,
				mSelectedYear, 
				mCampaignFilter.getValue(), 
				mSurveyFilter.getValue());
	}

	public void setupFilters(){
		//Set filters
		mCampaignFilter = (FilterControl)findViewById(R.id.campaign_filter);
		mSurveyFilter = (FilterControl)findViewById(R.id.survey_filter);

		final ContentResolver cr = getContentResolver();
		mCampaignFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(boolean selfChange, String curCampaignValue) {
				Cursor surveyCursor;

				String[] projection = {Surveys.SURVEY_TITLE, Surveys.CAMPAIGN_URN, Surveys.SURVEY_ID};
				
				mSurveyFilter.clearAll();
				if(!curCampaignValue.equals("all")){
					surveyCursor = cr.query(Campaigns.buildSurveysUri(curCampaignValue), projection, null, null, null);

					//Update SurveyFilter
					//Concatenate Campain_URN and Survey_ID with a colon for survey filer values,
					//in order to handle 'All Campaign' case.
					//This requirement is not valid any more.

					for(surveyCursor.moveToFirst();!surveyCursor.isAfterLast();surveyCursor.moveToNext()){
						mSurveyFilter.add(new Pair<String, String>(
								surveyCursor.getString(surveyCursor.getColumnIndex(Surveys.SURVEY_TITLE)),
								surveyCursor.getString(surveyCursor.getColumnIndex(Surveys.CAMPAIGN_URN)) + 
								":" +
								surveyCursor.getString(surveyCursor.getColumnIndex(Surveys.SURVEY_ID))
								));
					}
					surveyCursor.close();
				}
				mSurveyFilter.add(0, new Pair<String, String>("All Surveys", mCampaignFilter.getValue() + ":" + "all"));

				setGridCellAdapterToDate(RHCalendarViewActivity.this.mSelectedMonth, 
						RHCalendarViewActivity.this.mSelectedYear,
						mCampaignFilter.getValue(),
						mSurveyFilter.getValue());
			}
		});

		mSurveyFilter.setOnChangeListener(new FilterChangeListener() {
			@Override
			public void onFilterChanged(boolean selfChange, String curValue) {
				setGridCellAdapterToDate(
						RHCalendarViewActivity.this.mSelectedMonth,
						RHCalendarViewActivity.this.mSelectedYear, 
						mCampaignFilter.getValue(), 
						mSurveyFilter.getValue());			
			}
		});

		String select = Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY;
		String[] projection = {Campaigns.CAMPAIGN_NAME, Campaigns.CAMPAIGN_URN};
		Cursor campaigns = cr.query(Campaigns.CONTENT_URI, projection, select, null, null);
		mCampaignFilter.populate(campaigns, Campaigns.CAMPAIGN_NAME, Campaigns.CAMPAIGN_URN);
		mCampaignFilter.add(0, new Pair<String, String>("All Campaigns", "all"));
	}

	/**
	 * 
	 * @param month
	 * @param year
	 */
	private void setGridCellAdapterToDate(int month, int year, String curCamValue, String curSurValue)
	{
		adapter = new GridCellAdapter(getApplicationContext(), R.id.calendar_gridcell_num_responses, month, year, curCamValue
				,curSurValue);
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
			setGridCellAdapterToDate(mSelectedMonth, mSelectedYear, mCampaignFilter.getValue(), mSurveyFilter.getValue());
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
			setGridCellAdapterToDate(mSelectedMonth, mSelectedYear, mCampaignFilter.getValue(), mSurveyFilter.getValue());
		}
	}

	@Override
	public void onDestroy()
	{
		Log.d(tag, "Destroying View ...");
		super.onDestroy();
	}
	
	private TextView getTextViewForTextSwitcher()
	{
		TextView textview = new TextView(this);
		textview.setTextColor(R.color.white);
		textview.setBackgroundColor(R.color.dkgray);
		textview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
		textview.setTypeface(null, Typeface.ITALIC);
		textview.setGravity(Gravity.CENTER);
		
		int padding_in_dp = 4;  // 6 dps
		final float scale = getResources().getDisplayMetrics().density;
		int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
		    
		textview.setPadding(0, 0, 0, padding_in_px);
		return textview;		
	}
	
	@Override
	public View makeView() {
		TextView textview = new TextView(this);
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
		private int daysInMonth, prevMonthDays;
		private final int currentDayOfMonth;
		private final int currentWeekDay;
		private Button numOfResponsesDisplay;
		private TextView dateDisplay;
		private final HashMap eventsPerMonthMap;
		private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MMM-yyyy");
		private final int currentMonth;

		// Days in Current Month
		public GridCellAdapter(Context context, int textViewResourceId, int month, int year, String curCampaignValue, 
				String curSurveyValue)
		{
			super();
			this._context = context;
			this.list = new ArrayList<String>();
			this.currentMonth = Calendar.getInstance().get(Calendar.MONTH);

			Log.d(tag, "==> Passed in Date FOR Month: " + month + " " + "Year: " + year);
			Calendar calendar = Calendar.getInstance();
			this.currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
			this.currentWeekDay = calendar.get(Calendar.DAY_OF_WEEK);
			Log.d(tag, "New Calendar:= " + calendar.getTime().toString());
			Log.d(tag, "CurrentDayOfWeek :" + getCurrentWeekDay());
			Log.d(tag, "CurrentDayOfMonth :" + getCurrentDayOfMonth());

			// Print Month
			printMonth(month, year);

			// Find Number of Events
			eventsPerMonthMap = findNumberOfEventsPerMonth(year, month, curSurveyValue);
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
//				Log.d(tag, "*->PrevYear: " + prevYear + " PrevMonth:" + prevMonth + " NextMonth: " + nextMonth + " NextYear: " + nextYear);
			}
			else if (currentSelectedMonth == 0)
			{
				prevMonth = 11;
				prevYear = yy - 1;
				nextYear = yy;
				daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
				nextMonth = 1;
//				Log.d(tag, "**--> PrevYear: " + prevYear + " PrevMonth:" + prevMonth + " NextMonth: " + nextMonth + " NextYear: " + nextYear);
			}
			else
			{
				prevMonth = currentSelectedMonth - 1;
				nextMonth = currentSelectedMonth + 1;
				nextYear = yy;
				prevYear = yy;
				daysInPrevMonth = getNumberOfDaysOfMonth(prevMonth);
//				Log.d(tag, "***---> PrevYear: " + prevYear + " PrevMonth:" + prevMonth + " NextMonth: " + nextMonth + " NextYear: " + nextYear);
			}

			// Compute how much to leave before before the first day of the
			// month.
			// getDay() returns 0 for Sunday.
			int currentWeekDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
			trailingSpaces = currentWeekDay;

//			Log.d(tag, "Week Day:" + currentWeekDay + " is " + getWeekDayAsString(currentWeekDay));
//			Log.d(tag, "No. Trailing space to Add: " + trailingSpaces);
//			Log.d(tag, "No. of Days in Previous Month: " + daysInPrevMonth);

			if (cal.isLeapYear(cal.get(Calendar.YEAR)) && mm == 2)
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
//				Log.d(currentMonthName, String.valueOf(i) + " " + getMonthAsString(currentSelectedMonth) + " " + yy);
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
//				Log.d(tag, "NEXT MONTH:= " + getMonthAsString(nextMonth));
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
		private HashMap<String, Integer> findNumberOfEventsPerMonth(int year, int month, String curSurveyValue)
		{
			ContentResolver cr = RHCalendarViewActivity.this.getContentResolver();

			String campaignUrn = curSurveyValue.substring(0, curSurveyValue.lastIndexOf(":"));
			String surveyID = curSurveyValue.substring(curSurveyValue.lastIndexOf(":")+1, curSurveyValue.length());

			//Create Uri
			Uri uri;
			if(campaignUrn.equals("all")){

				if(surveyID.equals("all")){
					uri = Responses.CONTENT_URI;
				}
				else{
					uri = Campaigns.buildResponsesUri(campaignUrn, surveyID);				    		
				}

			}
			else{
				if(surveyID.equals("all")){
					uri = Campaigns.buildResponsesUri(campaignUrn);				    		
				}
				else{
					uri = Campaigns.buildResponsesUri(campaignUrn, surveyID);				    		
				}
			}


			GregorianCalendar greCalStart = new GregorianCalendar(mSelectedYear, mSelectedMonth-1, 1);
			GregorianCalendar greCalEnd = new GregorianCalendar(mSelectedYear, mSelectedMonth-1, greCalStart.getActualMaximum(GregorianCalendar.DAY_OF_MONTH), 23, 59, 59);
			
			String selection = 
					Responses.RESPONSE_TIME + " > " + greCalStart.getTime().getTime() +
					" AND " + 
					Responses.RESPONSE_TIME + " < " + greCalEnd.getTime().getTime();

			//Create Query
			Cursor responseCursorThisMonth = cr.query(
					uri, 
					null, 
					selection, 
					null, 
					Responses.RESPONSE_DATE
					);
			
			Cursor responseCursorTotal = cr.query(uri, null, null, null, null);

			HashMap<String, Integer> map = new HashMap<String, Integer>();
			Calendar cal = Calendar.getInstance();

			int numOfResponse = 0;
			for(responseCursorThisMonth.moveToFirst();!responseCursorThisMonth.isAfterLast();responseCursorThisMonth.moveToNext()){
				Long time = responseCursorThisMonth.getLong(responseCursorThisMonth.getColumnIndex(Responses.RESPONSE_TIME));
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
			//mNumResponseSummary.setText(this.getMonthAsString(month-1) + ": " + numOfResponse + " / Total: " + responseCursorTotal.getCount());
			
			mTextSwitcher.setText(this.getMonthAsString(month-1) + ": " + numOfResponse + " / Total: " + responseCursorTotal.getCount());
	
			responseCursorThisMonth.close();
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
				LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = inflater.inflate(R.layout.calendar_view_gridcell, parent, false);
			}

			// Get a reference to the Day gridcell
			numOfResponsesDisplay = (Button) row.findViewById(R.id.calendar_gridcell_num_responses);
			numOfResponsesDisplay.setOnClickListener(this);

			// ACCOUNT FOR SPACING

			// Log.d(tag, "Current Day: " + getCurrentDayOfMonth());
			String[] day_color = list.get(position).split("-");
			String theday = day_color[0];
			String themonth = day_color[2];
			String theyear = day_color[3];
			if ((!eventsPerMonthMap.isEmpty()) && (eventsPerMonthMap != null))
			{
				if (eventsPerMonthMap.containsKey(theday))
				{
					// Set the Day GridCell
					numOfResponsesDisplay.setText(eventsPerMonthMap.get(theday).toString());
					Log.d(tag, "Setting GridCell " + theday + "-" + themonth + "-" + theyear);
				}
			}
			dateDisplay = (TextView) row.findViewById(R.id.calendar_gridcell_date);
			dateDisplay.setBackgroundColor(Color.WHITE);
			dateDisplay.setText(theday);
			try {
				Date clickedDate = clickDateFormatter.parse(theday+"-"+themonth+"-"+theyear);
				numOfResponsesDisplay.setTag(clickedDate.getTime());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (day_color[1].equals("OUTOFTHISMONTH"))
			{
				dateDisplay.setTextColor(Color.LTGRAY);
				numOfResponsesDisplay.setTextColor(Color.WHITE);
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

//			try
//			{
				Button btn = (Button)view.findViewById(R.id.calendar_gridcell_num_responses);
				String num = btn.getText().toString();
				if(!num.equals("")){
					Intent intent = new Intent(RHCalendarViewActivity.this, ResponseListActivity.class);
					String surveyFilter = mSurveyFilter.getValue().substring(mSurveyFilter.getValue().lastIndexOf(":")+1);
					if(!"all".equals(mCampaignFilter.getValue()))
						intent.putExtra(CampaignFilter.EXTRA_CAMPAIGN_URN, mCampaignFilter.getValue());
					if(!"all".equals(surveyFilter))
						intent.putExtra(CampaignSurveyFilter.EXTRA_SURVEY_ID, surveyFilter);
					intent.putExtra(ResponseListActivity.EXTRA_DATE_FLITER, (Long) view.getTag());
					startActivity(intent);					
				}

				//Need to remove later
//				Date parsedDate = dateFormat.getInstance();
//				cal.setTimeInMillis(parsedDate.getTime());
//				Toast.makeText(RHCalendarViewActivity.this, 
//						cal.get(Calendar.MONTH) + "-" + (cal.get(Calendar.DAY_OF_MONTH)) + " " + mCampaignFilter.getText() + " " + mSurveyFilter.getText()
//						, Toast.LENGTH_SHORT).show();
//			}
//			catch (ParseException e)
//			{
//				e.printStackTrace();
//			}
		}

		public int getCurrentDayOfMonth()
		{
			return currentDayOfMonth;
		}

		public int getCurrentWeekDay()
		{
			return currentWeekDay;
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

		@Override
		public String getItem(int position)
		{
			return list.get(position);
		}
		@Override
		public int getCount()
		{
			return list.size();
		}
	}


}


