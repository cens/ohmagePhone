/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.ucla.cens.andwellness.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import edu.ucla.cens.andwellness.Survey;

public class SurveyListAdapter extends BaseAdapter {
	
	private Context mContext;
	private int mSurveyLayoutId;
	private int mHeaderLayoutId;
	private List<Survey> mSurveys;
	private List<String> mActiveSurveyTitles;
	private List<Survey> mPending;
	private List<Survey> mAvailable;
	private List<Survey> mUnavailable;
	private LayoutInflater mInflater;
	private SharedPreferencesHelper mPreferencesHelper;
	
	private static final int VIEW_SURVEY = 0;
	private static final int VIEW_HEADER = 1;
	
	public static final int GROUP_PENDING = 0;
	public static final int GROUP_AVAILABLE = 1;
	public static final int GROUP_UNAVAILABLE = 2;
	
	public SurveyListAdapter(Context context, List<Survey> surveys, List<String> activeSurveyTitles, int surveyLayoutResource, int headerLayoutResource) {
		mContext = context;
		mSurveyLayoutId = surveyLayoutResource;
		mHeaderLayoutId = headerLayoutResource;
		mSurveys = surveys;
		mActiveSurveyTitles = activeSurveyTitles;
		mPending = new ArrayList<Survey>();
		mAvailable = new ArrayList<Survey>();
		mUnavailable = new ArrayList<Survey>();
		
		updateStates();
				
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPreferencesHelper = new SharedPreferencesHelper(context);
	}

	@Override
	public int getCount() {
		int count = 0;
		count += mSurveys.size();
		count += mPending.size() > 0 ? 1 : 0;
		count += mAvailable.size() > 0 ? 1 : 0;
		count += mUnavailable.size() > 0 ? 1 : 0;
		return count;
	}

	@Override
	public Object getItem(int position) {
		int sizeOfPendingSection = mPending.size() + (mPending.size() > 0 ? 1 : 0);
		int sizeOfAvailableSection = mAvailable.size() + (mAvailable.size() > 0 ? 1 : 0);
		int sizeOfUnavailableSection = mUnavailable.size() + (mUnavailable.size() > 0 ? 1 : 0);
		
		if (getItemViewType(position) == VIEW_SURVEY) {
			switch (getItemGroup(position)) {
			case GROUP_PENDING:
				if (position > 0 && position < sizeOfPendingSection) {
					return mPending.get(position - 1);
				}
				break;
				
			case GROUP_AVAILABLE:
				if (position - sizeOfPendingSection < sizeOfAvailableSection) {
					return mAvailable.get(position - sizeOfPendingSection - 1);
				}
				break;
				
			case GROUP_UNAVAILABLE:
				if (position - sizeOfPendingSection - sizeOfAvailableSection < sizeOfUnavailableSection) {
					return mUnavailable.get(position - sizeOfPendingSection - sizeOfAvailableSection - 1);
				}
				break;
			}
		}
		
		return null;
		
		/*try {
			if (position == 0) {
				if (sizeOfPendingSection > 0) {
					return GROUP_PENDING;
				} else if (sizeOfAvailableSection > 0) {
					return GROUP_AVAILABLE;
				} else {
					return GROUP_UNAVAILABLE;
				}
			} else if (position < sizeOfPendingSection) {
				return mPending.get(position - 1);
			} else if (position == sizeOfPendingSection) {
				if (sizeOfAvailableSection > 0) {
					return GROUP_AVAILABLE;
				} else {
					return GROUP_UNAVAILABLE;
				}
			} else if (position - sizeOfPendingSection < sizeOfAvailableSection) {
				return mAvailable.get(position - sizeOfPendingSection - 1);
			} else if (position - sizeOfPendingSection == sizeOfAvailableSection) {
				return GROUP_UNAVAILABLE;
			} else if (position - sizeOfPendingSection - sizeOfAvailableSection < sizeOfUnavailableSection) {
				return mUnavailable.get(position - sizeOfPendingSection - 1);
			} else {
				throw new IndexOutOfBoundsException();
			}
		} catch (IndexOutOfBoundsException e) {
			throw e;
		}*/
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public int getItemGroup(int position) {
		int sizeOfPendingSection = mPending.size() + (mPending.size() > 0 ? 1 : 0);
		int sizeOfAvailableSection = mAvailable.size() + (mAvailable.size() > 0 ? 1 : 0);
		int sizeOfUnavailableSection = mUnavailable.size() + (mUnavailable.size() > 0 ? 1 : 0);
		
		if (position == 0) {
			if (sizeOfPendingSection > 0) {
				return GROUP_PENDING;
			} else if (sizeOfAvailableSection > 0) {
				return GROUP_AVAILABLE;
			} else {
				return GROUP_UNAVAILABLE;
			}
		} else if (position < sizeOfPendingSection) {
			return GROUP_PENDING;
		} else if (position == sizeOfPendingSection) {
			if (sizeOfAvailableSection > 0) {
				return GROUP_AVAILABLE;
			} else {
				return GROUP_UNAVAILABLE;
			}
		} else if (position - sizeOfPendingSection < sizeOfAvailableSection) {
			return GROUP_AVAILABLE;
		} else if (position - sizeOfPendingSection == sizeOfAvailableSection) {
			return GROUP_UNAVAILABLE;
		} else if (position - sizeOfPendingSection - sizeOfAvailableSection < sizeOfUnavailableSection) {
			return GROUP_UNAVAILABLE;
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public int getItemViewType(int position) {
		int sizeOfPendingSection = mPending.size() + (mPending.size() > 0 ? 1 : 0);
		int sizeOfAvailableSection = mAvailable.size() + (mAvailable.size() > 0 ? 1 : 0);
		int sizeOfUnavailableSection = mUnavailable.size() + (mUnavailable.size() > 0 ? 1 : 0);
		
		if (position == 0 || position == sizeOfPendingSection || position - sizeOfPendingSection == sizeOfAvailableSection) {
			return VIEW_HEADER;
		} else {
			return VIEW_SURVEY;
		}
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		
		if (getItemViewType(position) == VIEW_HEADER) {
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public void notifyDataSetChanged() {
		
		updateStates();
		
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (getItemViewType(position) == VIEW_SURVEY) {
			SurveyViewHolder holder;
			
			if (convertView == null) {
				convertView = mInflater.inflate(mSurveyLayoutId, parent, false);
				holder = new SurveyViewHolder();
				holder.stateView = convertView.findViewById(R.id.survey_state_view);
				holder.titleText = (TextView) convertView.findViewById(R.id.survey_title_text);
				holder.lastTakenDateText = (TextView) convertView.findViewById(R.id.survey_last_taken_date_text);
				holder.lastTakenTimeText = (TextView) convertView.findViewById(R.id.survey_last_taken_time_text);
				convertView.setTag(holder);
			} else {
				holder = (SurveyViewHolder) convertView.getTag();
			}
			
			switch (getItemGroup(position)) {
			case GROUP_PENDING:
				holder.stateView.setBackgroundColor(Color.rgb(1, 173, 73));
				convertView.setEnabled(true);
				holder.titleText.setTextColor(Color.BLACK);
				holder.lastTakenDateText.setTextColor(Color.DKGRAY);
				holder.lastTakenTimeText.setTextColor(Color.DKGRAY);
				break;

			case GROUP_AVAILABLE:
				holder.stateView.setBackgroundColor(Color.rgb(56, 160, 220));
				convertView.setEnabled(true);
				holder.titleText.setTextColor(Color.BLACK);
				holder.lastTakenDateText.setTextColor(Color.DKGRAY);
				holder.lastTakenTimeText.setTextColor(Color.DKGRAY);
				break;
				
			case GROUP_UNAVAILABLE:
				convertView.setEnabled(false);
				holder.stateView.setBackgroundColor(Color.LTGRAY);
				holder.titleText.setTextColor(Color.GRAY);
				holder.lastTakenDateText.setTextColor(Color.GRAY);
				holder.lastTakenTimeText.setTextColor(Color.GRAY);
				holder.stateView.setBackgroundColor(Color.LTGRAY);
				break;
			}
			
			Survey survey = (Survey)getItem(position); 
			holder.titleText.setText(survey.getTitle());
			
			Long lastSubmitTime = mPreferencesHelper.getLastSurveyTimestamp(survey.getId());
			
			if (lastSubmitTime == 0) {
				holder.lastTakenDateText.setText("");
				holder.lastTakenTimeText.setText("");
			} else {
				//String [] dayAndTime = getDayAndTimeStrings(System.currentTimeMillis(), lastSubmitTime);
				int flags = DateUtils.FORMAT_ABBREV_RELATIVE | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
				String relativeTime = DateUtils.getRelativeDateTimeString(mContext, lastSubmitTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, flags).toString();
				String [] strings = relativeTime.split(", ");
				if (strings.length == 2) {
					holder.lastTakenDateText.setText(strings[0]);
					holder.lastTakenTimeText.setText(strings[1]);
				} else {
					holder.lastTakenDateText.setText("");
					holder.lastTakenTimeText.setText(relativeTime);
				}
			}
			
		} else {
			HeaderViewHolder holder;
			
			if (convertView == null) {
				convertView = mInflater.inflate(mHeaderLayoutId, parent, false);
				holder = new HeaderViewHolder();
				holder.headerText = (TextView) convertView.findViewById(R.id.header_text);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			
			switch (getItemGroup(position)) {
			case GROUP_PENDING:
				convertView.setBackgroundColor(Color.rgb(1, 173, 73));
				holder.headerText.setText("Pending Surveys");
				break;

			case GROUP_AVAILABLE:
				convertView.setBackgroundColor(Color.rgb(56, 160, 220));
				holder.headerText.setText("Available Surveys");
				break;
				
			case GROUP_UNAVAILABLE:
				convertView.setBackgroundColor(Color.LTGRAY);
				holder.headerText.setText("Currently Unavailable Surveys");
				break;
			}
		}
		
		return convertView;
	}

	static class SurveyViewHolder {
		View stateView;
		TextView titleText;
		TextView lastTakenDateText;
		TextView lastTakenTimeText;
	}
	
	static class HeaderViewHolder {
		TextView headerText;
	}
	
	private void updateStates() {
		
		mPending.clear();
		mAvailable.clear();
		mUnavailable.clear();
		
		for (Survey survey : mSurveys) {
			if (mActiveSurveyTitles.contains(survey.getTitle())) {
				mPending.add(survey);
			} else if (survey.isAnytime()) {
				mAvailable.add(survey);
			} else {
				mUnavailable.add(survey);
			}
		}
	}
	
	/*String [] getDayAndTimeStrings(long nowInMillis, long thenInMillis) {
		
		String [] dayAndTime = new String [2];
		dayAndTime[0] = DateFormat.format("MMMM dd", thenInMillis).toString();
		dayAndTime[1] = DateFormat.format("h:mmaa", thenInMillis).toString();
		
		Calendar now = Calendar.getInstance();
		now.setTimeInMillis(nowInMillis);
		
		Calendar then = Calendar.getInstance();
		then.setTimeInMillis(thenInMillis);
		
		int yearDiff = now.get(Calendar.YEAR) - then.get(Calendar.YEAR);
		
		if (yearDiff == 0) {
			
			int monthDiff = now.get(Calendar.MONTH) - then.get(Calendar.MONTH);
			
			if (monthDiff == 0) {
				
				int weekDiff = now.get(Calendar.WEEK_OF_MONTH) - then.get(Calendar.WEEK_OF_MONTH);
				
				if (weekDiff == 0) {
					
					int dayDiff = now.get(Calendar.DAY_OF_MONTH) - then.get(Calendar.DAY_OF_MONTH);
					
					
					
				} else if (weekDiff == 1) {
					dayAndTime[0] = "";
					dayAndTime[1] = "Last week";
				} else {
					dayAndTime[0] = "";
					dayAndTime[1] = String.valueOf(weekDiff) + " weeks ago";
				}
				
			} else if (monthDiff == 1) {
				dayAndTime[0] = "";
				dayAndTime[1] = "Last month";
			} else {
				dayAndTime[0] = "";
				dayAndTime[1] = String.valueOf(monthDiff) + " months ago";
			}
			
		} else if (yearDiff == 1) {
			dayAndTime[0] = "";
			dayAndTime[1] = "Last year";
		} else {
			dayAndTime[0] = "";
			dayAndTime[1] = String.valueOf(yearDiff) + " years ago";
		}
		
		return dayAndTime;
	}*/
	
	/*String getRelativeTimeString(long time) {
		final int SECOND = 1;
		final int MINUTE = 60 * SECOND;
		final int HOUR = 60 * MINUTE;
		final int DAY = 24 * HOUR;
		final int MONTH = 30 * DAY;
		
		
		long delta = System.currentTimeMillis() - time;

		if (delta < 0)
		{
		  return "not yet";
		}
		if (delta < 1 * MINUTE)
		{
		  return ts.Seconds == 1 ? "one second ago" : ts.Seconds + " seconds ago";
		}
		if (delta < 2 * MINUTE)
		{
		  return "a minute ago";
		}
		if (delta < 45 * MINUTE)
		{
		  return ts.Minutes + " minutes ago";
		}
		if (delta < 90 * MINUTE)
		{
		  return "an hour ago";
		}
		if (delta < 24 * HOUR)
		{
		  return ts.Hours + " hours ago";
		}
		if (delta < 48 * HOUR)
		{
		  return "yesterday";
		}
		if (delta < 30 * DAY)
		{
		  return ts.Days + " days ago";
		}
		if (delta < 12 * MONTH)
		{
		  int months = Convert.ToInt32(Math.Floor((double)ts.Days / 30));
		  return months <= 1 ? "one month ago" : months + " months ago";
		}
	}*/
}
