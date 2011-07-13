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
package edu.ucla.cens.andwellness;

import java.io.File;
import java.io.IOException;

import android.app.Application;
import android.util.Log;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.prompt.multichoicecustom.MultiChoiceCustomDbAdapter;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.prompt.singlechoicecustom.SingleChoiceCustomDbAdapter;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;

public class AndWellnessApplication extends Application {
	
	private static final String TAG = "AndWellnessApplication";

	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.i(TAG, "onCreate()");
		
		edu.ucla.cens.systemlog.Log.initialize(this, "AndWellness");
	}
	
	public void resetAll() {
		//clear everything?
		
		//clear triggers
		TriggerFramework.resetAllTriggerSettings(this);
		
		//clear shared prefs
		new SharedPreferencesHelper(this).clearAll();
		
		//clear db
		new DbHelper(this).clearAll();
		
		//clear custom type dbs
		SingleChoiceCustomDbAdapter singleChoiceDbAdapter = new SingleChoiceCustomDbAdapter(this); 
		if (singleChoiceDbAdapter.open()) {
			singleChoiceDbAdapter.clearAll();
			singleChoiceDbAdapter.close();
		}
		MultiChoiceCustomDbAdapter multiChoiceDbAdapter = new MultiChoiceCustomDbAdapter(this); 
		if (multiChoiceDbAdapter.open()) {
			multiChoiceDbAdapter.clearAll();
			multiChoiceDbAdapter.close();
		}
		
		//clear images
		try {
			Utilities.delete(new File(PhotoPrompt.IMAGE_PATH));
		} catch (IOException e) {
			Log.e(TAG, "Error deleting images", e);
		}
	}
}
