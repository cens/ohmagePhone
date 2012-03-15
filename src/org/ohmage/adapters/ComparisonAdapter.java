package org.ohmage.adapters;

import org.ohmage.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.Utilities.DataMapper;
import org.ohmage.adapters.ComparisonAdapter.ComparisonAdapterItem;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TableLayout;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;

public class ComparisonAdapter extends ArrayAdapter<ComparisonAdapterItem>{
	private static final String TAG = "ComparisionAdapter";

	public static class ComparisonAdapterItem {
		String title;
		String baseLine;
		String lastWeek;
		String current;

		public static final int AVG = 0;
		public static final int WEEKAVG = 1;
		public static final int PERCENT = 2;
		public static final int WEEKPERCENT = 3;

		public ComparisonAdapterItem(String title) {
			this.title = title;
		}

		public void setData(Context context, LinkedList<FeedbackItem> data, DataMapper mapper, int type) {
			Calendar cal = Calendar.getInstance();
			long now = cal.getTimeInMillis();
			cal.add(Calendar.DATE, -cal.get(Calendar.DAY_OF_WEEK) + 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			long oneWeek = cal.getTimeInMillis();
			cal.add(Calendar.DATE, -7);
			long twoWeeks = cal.getTimeInMillis();
			long base = UserPreferencesHelper.getBaseLineEndTime(context);
			if(base == 0) {
				//If baseline is not set, we set it to 3 months ago
				cal.add(Calendar.MONTH, -3);
				base = cal.getTimeInMillis();
			}

			ArrayList<Double> nowvalues = new ArrayList<Double>();
			ArrayList<Double> weekvalues = new ArrayList<Double>();
			ArrayList<Double> basevalues = new ArrayList<Double>();

			long firstTime = base;

			for(FeedbackItem point : data) {
				if(point.time < base) {
					firstTime = Math.min(firstTime, point.time);
					basevalues.add(point.value);
				} else if(point.time >= twoWeeks && point.time < oneWeek) {
					weekvalues.add(point.value);
				} else if(point.time >= oneWeek && point.time < now) {
					nowvalues.add(point.value);	
				}
			}

			long baseDays = (base - firstTime) / DateUtils.DAY_IN_MILLIS;
			baseLine = calcAverage(basevalues, baseDays, mapper, type);
			long weekDays = (oneWeek - twoWeeks) / DateUtils.DAY_IN_MILLIS;
			lastWeek = calcAverage(weekvalues, weekDays, mapper, type);
			long currentDays = (now - oneWeek) / DateUtils.DAY_IN_MILLIS + 1; //Plus one to include today
			current = calcAverage(nowvalues, currentDays, mapper, type);		
		}

		protected String calcAverage(ArrayList<Double> values, long days, DataMapper mapper, int type) {
			Double count = 0.0;
			for(Double i : values) {
				count += mapper.translate(i);
			}

			NumberFormat formatter = NumberFormat.getInstance();
			formatter.setMaximumFractionDigits(2);

			switch(type) {
				case AVG: {
					return formatter.format(Double.valueOf(count) / values.size());
				} case WEEKAVG: {
					return formatter.format(Double.valueOf(count) / (values.size() / 7.0));
				}
			}
			return null;
		}
	}

	public static class ComparisonAdapterSubItem extends ComparisonAdapterItem {

		private final int mValue;
		int mColor;

		public ComparisonAdapterSubItem(String title, int value, int color) {
			super(title);
			mValue = value;
			mColor = color;
		}

		@Override
		protected String calcAverage(ArrayList<Double> values, long days, DataMapper mapper, int type) {
			int count = 0;
			for(Double i : values) {
				if(i.intValue() == mValue)
					count++;
			}

			NumberFormat mFormat = NumberFormat.getInstance();
			mFormat.setMaximumFractionDigits(1);
			return mFormat.format(Double.valueOf(count) / values.size() * 100) + "%";

		}
	}

	public ComparisonAdapter(Context context) {
		super(context, R.layout.feedback_comparison_row, R.id.feedback_comparision_title, new ArrayList<ComparisonAdapterItem>());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);

		ComparisonAdapterItem item = getItem(position);
		if(item instanceof ComparisonAdapterSubItem) {
			view.setVisibility(View.GONE);
			view.setBackgroundColor((position%2==0) ? Utilities.lightenColor(((ComparisonAdapterSubItem) item).mColor) : ((ComparisonAdapterSubItem) item).mColor);
			TableLayout.LayoutParams params = (TableLayout.LayoutParams) view.getLayoutParams();
			params.leftMargin = 60;
			view.setPadding(-60, 0, 0, 0);
		} else {
			view.setVisibility(View.VISIBLE);
			view.setBackgroundResource((position%2==0) ? R.color.lightestgray : R.color.lightergray);
		}

		TextView title = (TextView) view.findViewById(R.id.feedback_comparision_title);
		title.setText(item.title);
		title.setTypeface((item instanceof ComparisonAdapterSubItem) ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);

		TextView baseline = (TextView) view.findViewById(R.id.feedback_comparision_baseline);
		baseline.setText(item.baseLine);

		TextView lastweek = (TextView) view.findViewById(R.id.feedback_comparision_lastweek);
		lastweek.setText(item.lastWeek);
		lastweek.setTextColor(R.color.black);

		TextView current = (TextView) view.findViewById(R.id.feedback_comparision_current);
		current.setText(item.current);
		lastweek.setTextColor(R.color.black);

		return view;
	}
}