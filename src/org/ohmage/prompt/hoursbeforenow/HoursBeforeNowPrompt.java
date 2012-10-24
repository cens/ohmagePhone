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
package org.ohmage.prompt.hoursbeforenow;

import org.ohmage.R;
import org.ohmage.prompt.number.NumberPrompt;

public class HoursBeforeNowPrompt extends NumberPrompt {

	public HoursBeforeNowPrompt() {
		super();
	}

	@Override
	protected void clearTypeSpecificResponseData() {
		super.clearTypeSpecificResponseData();
	}
	
	@Override
	protected int getLayoutResource() {
		return R.layout.prompt_hours_before_now;
	}
}
