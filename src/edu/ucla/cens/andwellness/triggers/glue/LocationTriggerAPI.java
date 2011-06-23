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
package edu.ucla.cens.andwellness.triggers.glue;

import android.content.Context;
import android.content.Intent;
import edu.ucla.cens.andwellness.triggers.types.location.LocTrigTracingSettActivity;

/*
 * APIs exported from location triggers. This class
 * exports interfaces which can be used by the clients
 * to directly interact with location based triggers 
 * (without going through the framework).
 */
public class LocationTriggerAPI {
	
	/*
	 * Launch the activity with the location tracing settings. 
	 */
	public static void launchLocationTracingSettingsActivity(Context context) {
		
		context.startActivity(new Intent(context, 
							      LocTrigTracingSettActivity.class));
	}
}
