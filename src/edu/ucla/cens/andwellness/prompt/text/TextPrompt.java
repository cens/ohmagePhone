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
package edu.ucla.cens.andwellness.prompt.text;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.prompt.AbstractPrompt;

public class TextPrompt extends AbstractPrompt {
	
	private String mText;

	public TextPrompt() {
		super();
		mText = "";
	}
	
	@Override
	protected void clearTypeSpecificResponseData() {
		if (mDefaultValue != null) {
			mText = getDefaultValue();
		} else {
			mText = "";
		}
	}
	
	/**
	 * Returns true if the text for this prompt is not null nor an empty
	 * String.
	 */
	@Override
	public boolean isPromptAnswered() {
		return((mText != null) && (! "".equals(mText)));
	}

	@Override
	protected String getTypeSpecificResponseObject() {
		if (mText.equals("")) {
			return null;
		}
		else {
			return mText;
		}
	}
	
	/**
	 * The text to be displayed to the user if the prompt is considered
	 * unanswered.
	 */
	@Override
	public String getUnansweredPromptText() {
		return("Please enter any text before continuing.");
	}
	
	@Override
	protected Object getTypeSpecificExtrasObject() {
		return null;
	}

	@Override
	public View getView(Context context) {
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.prompt_text, null);
		
		EditText editText = (EditText) layout.findViewById(R.id.text);
		
		editText.setText(mText);
		
		editText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				mText = s.toString();
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
