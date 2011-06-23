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
	
	/**
	 * Returns true if the current value falls between the minimum and the
	 * maximum.
	 */
	@Override
	public boolean isPromptAnswered() {
		return((mValue >= mMinimum) && (mValue <= mMaximum));
	}

	@Override
	protected Object getTypeSpecificResponseObject() {
		return Integer.valueOf(mValue);
	}
	
	/**
	 * The text to be displayed to the user if the prompt is considered
	 * unanswered.
	 */
	@Override
	public String getUnansweredPromptText() {
		return("Please choose a value between " + mMinimum + " and " + mMaximum + ".");
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
