package org.ohmage.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * The {@link OhmageFilterable} class contains definitions for all the filterable options available to the data
 * and simple implementations to make them easier to use
 * @author cketcham
 *
 */
public class OhmageFilterable {
	public static interface CampaignFilterable {
		public void setCampaignUrn(String campaignUrn);
		public String getCampaignUrn();
	}

	public static class CampaignFilter implements CampaignFilterable {

		public static final String EXTRA_CAMPAIGN_URN = "extra_campaign_urn";

		private String mCampaignUrn;

		public CampaignFilter(Bundle args) {
			if(args != null && args.containsKey(EXTRA_CAMPAIGN_URN))
				mCampaignUrn = args.getString(EXTRA_CAMPAIGN_URN);
		}

		@Override
		public void setCampaignUrn(String campaignUrn) {
			mCampaignUrn = campaignUrn;
		}

		@Override
		public String getCampaignUrn() {
			return mCampaignUrn;
		}

	}

	public static interface SurveyFilterable {
		public void setSurveyId(String surveyId);
		public String getSurveyId();
	}

	public static class CampaignSurveyFilter implements CampaignFilterable, SurveyFilterable {

		public static final String EXTRA_SURVEY_ID = "extra_survey_id";

		private final CampaignFilter mCampaignFilter;
		private String mSurveyId;

		public CampaignSurveyFilter(Bundle args) {
			mCampaignFilter = new CampaignFilter(args);
			if(args != null && args.containsKey(EXTRA_SURVEY_ID))
				mSurveyId = args.getString(EXTRA_SURVEY_ID);
		}

		@Override
		public void setSurveyId(String surveyId) {
			mSurveyId = surveyId;
		}

		@Override
		public String getSurveyId() {
			return mSurveyId;
		}

		@Override
		public void setCampaignUrn(String campaignUrn) {
			mCampaignFilter.setCampaignUrn(campaignUrn);
			mSurveyId = null;
		}

		@Override
		public String getCampaignUrn() {
			return mCampaignFilter.getCampaignUrn();
		}

	}

	public static interface TimeFilterable {

		/**
		 * Returns the time in milliseconds of the time bounds (inclusive)
		 * @return
		 */
		public long getStartBounds();

		/**
		 * Returns the time in milliseconds of the end bounds (inclusive)
		 * @return
		 */
		public long getEndBounds();

		public void setDate(int day, int month, int year);
		public void setMonth(int month, int year);
		public int getMonth();
		public int getYear();
	}

	public static class TimeFilter implements TimeFilterable {

		public static final String EXTRA_DAY = "extra_day";
		public static final String EXTRA_MONTH = "extra_month";
		public static final String EXTRA_YEAR = "extra_year";

		private int mMonth;
		private int mYear;
		private int mDate;
		private int mCalendarUnit;

		public TimeFilter(Bundle args) {
			Calendar calendar = Calendar.getInstance();
			int month = calendar.get(Calendar.MONTH);
			int year = calendar.get(Calendar.YEAR);
			if(args != null && args.containsKey(EXTRA_MONTH))
				month = args.getInt(EXTRA_MONTH);
			if(args != null && args.containsKey(EXTRA_YEAR))
				year = args.getInt(EXTRA_YEAR);
			if(args != null && args.containsKey(EXTRA_DAY))
				setDate(args.getInt(EXTRA_DAY), month, year);
			else
				setMonth(month, year);
		}

		@Override
		public void setDate(int day, int month, int year) {
			mDate = day;
			mMonth = month;
			mYear = year;
			mCalendarUnit = Calendar.DATE;
		}

		@Override
		public void setMonth(int month, int year) {
			mDate = 1;
			mMonth = month;
			mYear = year;
			mCalendarUnit = Calendar.MONTH;
		}

		protected GregorianCalendar getCalendar() {
			return new GregorianCalendar(mYear, mMonth, mDate);
		}

		@Override
		public long getStartBounds() {
			return getCalendar().getTimeInMillis();
		}

		@Override
		public long getEndBounds() {
			GregorianCalendar calendar = getCalendar();
			calendar.add(mCalendarUnit, 1);
			return calendar.getTimeInMillis() - 1;
		}

		@Override
		public int getMonth() {
			return mMonth;
		}

		@Override
		public int getYear() {
			return mYear;
		}
	}

	public interface FilterableFragmentLoader extends CampaignFilterable, SurveyFilterable, TimeFilterable, LoaderCallbacks<Cursor> {
		public FragmentActivity getActivity();
	}
}
