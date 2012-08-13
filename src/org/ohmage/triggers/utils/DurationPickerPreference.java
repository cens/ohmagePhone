package org.ohmage.triggers.utils;

import org.ohmage.NumberPicker;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

public class DurationPickerPreference extends DialogPreference {

	private int mHours;
	private int mMin;
	private NumberPicker mHourPicker = null;
	private NumberPicker mMinPicker = null;
	private String tag = "DurationPickerPreference";
	
	public DurationPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}
	
	public void initialize(){
		mHours = 0;
		mMin = 0;
	}
	
	public void handleChange(int hours, int min){
		mHours = hours;
		mMin = min;
		this.setSummary(this.durationString());
	}
	
	public void set(int hrs, int min){
		handleChange(hrs,min);		
	}
	
	public void setHours(int hours){
		handleChange(hours,mMin);
	}
	public void setMin(int min){
		handleChange(mHours , min);
	}
	public int getHours(){
		return mHours;
	}
	public int getMin(){
		return mMin;
	}
	public long getDurationinMillis(){
		long millis = (mHours * 60 + mMin) * 60L * 1000L;
		return millis;
	}
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		mHourPicker.clearFocus();
		mMinPicker.clearFocus();
		if(positiveResult) {
			handleChange(mHourPicker.getCurrent() , mMinPicker.getCurrent());
		}
	}
	
	@Override
	protected View onCreateDialogView() {
		Log.d(tag , "onCreateDialogView called");
		mHourPicker = new NumberPicker(getContext());
		mMinPicker = new NumberPicker(getContext());
		mHourPicker.setRange(0, 12);
		mMinPicker.setRange(0, 59);
		mHourPicker.setCurrent(mHours);
		mMinPicker.setCurrent(mMin);
		this.setSummary(durationString());
		
		LinearLayout layout = new LinearLayout(getContext());
		layout.addView(mHourPicker);
		layout.addView(mMinPicker);
		TextView tv = new TextView(getContext());
		tv.setText("hrs:min");
		layout.addView(tv);
		return layout;
	}
	public String durationString(){
		String result = "";
		if (mHours == 1){
			result += "1 hour ";
		}
		else if (mHours > 1){
			result += mHours + " hours ";
		} 
		if (mMin == 1){
			result += "1 minute";
		}
		else if (mMin > 1){
			result += "" + mMin + " minutes";
		}
		return result;
	}

}
