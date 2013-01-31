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
package org.ohmage.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.ohmage.BackgroundManager;
import org.ohmage.ConfigHelper;
import org.ohmage.logprobe.Log;

public class BootReceiver extends BroadcastReceiver {
	
	public static final String TAG = "BOOT_RECEIVER";
	
	@Override
	public void onReceive(final Context context, Intent intent) {
		
		final ConfigHelper preferencesHelper = new ConfigHelper(context);
		boolean isFirstRun = preferencesHelper.isFirstRun();
		
		if (isFirstRun) {
			Log.v(TAG, "this is the first run");
			
		} else {
			Log.v(TAG, "this is not the first run");
			
			//start components
			BackgroundManager.initComponents(context);
		}
	}

}
