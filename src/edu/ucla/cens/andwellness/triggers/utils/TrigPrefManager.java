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

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class TrigPrefManager {
	
	private static final String PREF_FILE_NAME
								= TrigPrefManager.class.getName();
	
	public static void registerPreferenceFile(Context context, String campaignUrn, String fileName) {
		SharedPreferences prefs = context.getSharedPreferences(
									PREF_FILE_NAME + "_" + campaignUrn, Context.MODE_PRIVATE);

		Editor editor = prefs.edit();
		editor.putBoolean(fileName, true);
		editor.commit();
	}
	
//	public static void clearAllPreferenceFiles(Context context) {
//		
//		SharedPreferences prefMan = context.getSharedPreferences(
//										PREF_FILE_NAME, Context.MODE_PRIVATE);
//
//		Map<String, ?> prefFileList = prefMan.getAll();
//		if(prefFileList == null) {
//			return;
//		}
//		
//		for(String prefFile : prefFileList.keySet()) {
//			SharedPreferences pref = context.getSharedPreferences(
//											prefFile, Context.MODE_PRIVATE);
//			Editor editPref = pref.edit();
//			editPref.clear();
//			editPref.commit();
//		}
//		
//		//Clear the preference manager itself
//		Editor editMan = prefMan.edit();
//		editMan.clear();
//		editMan.commit();
//	}
	
	public static void clearPreferenceFiles(Context context, String campaignUrn) {
		
		SharedPreferences prefMan = context.getSharedPreferences(
										PREF_FILE_NAME + "_"+ campaignUrn, Context.MODE_PRIVATE);

		Map<String, ?> prefFileList = prefMan.getAll();
		if(prefFileList == null) {
			return;
		}
		
		for(String prefFile : prefFileList.keySet()) {
			SharedPreferences pref = context.getSharedPreferences(
											prefFile, Context.MODE_PRIVATE);
			Editor editPref = pref.edit();
			editPref.clear();
			editPref.commit();
		}
		
		//Clear the preference manager itself
		Editor editMan = prefMan.edit();
		editMan.clear();
		editMan.commit();
	}
}
