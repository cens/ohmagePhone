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
package org.ohmage.prompt.number;

import org.ohmage.NumberPicker;
import org.ohmage.NumberPicker.OnChangedListener;
import org.ohmage.R;
import org.ohmage.prompt.AbstractPrompt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public class NumberPrompt extends AbstractPrompt {

	private int mMinimum;
	private int mMaximum;
	private int mValue;
	private NumberPicker mNumberPicker;

	public NumberPrompt() {
		super();
	}

	public void setMinimum(int value) {
		mMinimum = value;
	}

	public void setMaximum(int value) {
		mMaximum = value;
	}

	public int getMinimum(){
		return mMinimum;
	}

	public int getMaximum(){
		return mMaximum;
	}

	public int getValue(){
		return mValue;
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
		// If there is a number picker, see if the value is valid
		// And check the value is between min and max
		return (mNumberPicker != null && mNumberPicker.forceValidateInput() || mNumberPicker == null)
				&& ((mValue >= mMinimum) && (mValue <= mMaximum));
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
		View layout = inflater.inflate(getLayoutResource(), null);

		mNumberPicker = (NumberPicker) layout.findViewById(R.id.number_picker);

		mNumberPicker.setRange(mMinimum, mMaximum);
		mNumberPicker.setCurrent(mValue);

		mNumberPicker.setOnChangeListener(new OnChangedListener() {

			@Override
			public void onChanged(NumberPicker picker, int oldVal, int newVal) {
				mValue = newVal;				
			}
		});

		return layout;
	}

	protected int getLayoutResource() {
		return R.layout.prompt_number;
	}
}
