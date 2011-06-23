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
package edu.ucla.cens.andwellness.triggers.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class TrigListPreference extends ListPreference {

	private TrigListPreference.onCancelListener mListener = null;
	
	public interface onCancelListener {
		public void onCancel();
	}
	
	public void setOnCancelListener(TrigListPreference.onCancelListener listener) {
		mListener = listener;
	}
	
	public TrigListPreference(Context context) {
		super(context);
	}
	
	public TrigListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		
		if(which == DialogInterface.BUTTON_NEGATIVE) {
			if(mListener != null) {
				mListener.onCancel();
			}
		}
		
		super.onClick(dialog, which);
	}

}
