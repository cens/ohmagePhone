package edu.ucla.cens.andwellness.prompt.hoursbeforenow;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import edu.ucla.cens.andwellness.NumberPicker;
import edu.ucla.cens.andwellness.NumberPicker.OnChangedListener;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;

public class HoursBeforeNowPrompt extends AbstractPrompt {

	private int mMinimum;
	private int mMaximum;
	private int mValue;
	
	public HoursBeforeNowPrompt() {
		super();
	}
	
	void setMinimum(int value) {
		mMinimum = value;
	}
	
	void setMaximum(int value) {
		mMaximum = value;
	}

	@Override
	protected void clearTypeSpecificResponseData() {
		if (mDefaultValue != null && ! mDefaultValue.equals("")) {
			mValue = Integer.parseInt(getDefaultValue());
		} else {
			mValue = mMinimum;
		}
	}

	@Override
	protected Object getTypeSpecificResponseObject() {
		return Integer.valueOf(mValue);
	}
	
	@Override
	protected Object getTypeSpecificExtrasObject() {
		return null;
	}

	@Override
	public View getView(Context context) {
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.prompt_hours_before_now, null);
		
		NumberPicker numberPicker = (NumberPicker) layout.findViewById(R.id.number_picker);
		
		numberPicker.setRange(mMinimum, mMaximum);
		numberPicker.setCurrent(mValue);
				
		numberPicker.setOnChangeListener(new OnChangedListener() {
			
			@Override
			public void onChanged(NumberPicker picker, int oldVal, int newVal) {
				mValue = newVal;				
			}
		});
		
		return layout;
	}

	@Override
	public void handleActivityResult(Context context, int requestCode,
			int resultCode, Intent data) {
		// TODO Auto-generated method stub
		
	}

}
