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
package org.ohmage;

import com.google.android.imageloader.ImageLoader;

import java.io.File;
import java.io.IOException;

import org.ohmage.db.DbHelper;
import org.ohmage.prompt.multichoicecustom.MultiChoiceCustomDbAdapter;
import org.ohmage.prompt.photo.PhotoPrompt;
import org.ohmage.prompt.singlechoicecustom.SingleChoiceCustomDbAdapter;
import org.ohmage.triggers.glue.TriggerFramework;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class OhmageApplication extends Application {
	
	private static final String TAG = "OhmageApplication";

	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.i(TAG, "onCreate()");
		
		edu.ucla.cens.systemlog.Log.initialize(this, "Ohmage");
		
        mImageLoader = createImageLoader(this);

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
	
    private static ImageLoader createImageLoader(Context context) {
        return new ImageLoader();
    }

    private ImageLoader mImageLoader;

    @Override
    public void onTerminate() {
        mImageLoader = null;
        super.onTerminate();
    }

	
    @Override
    public Object getSystemService(String name) {
        if (ImageLoader.IMAGE_LOADER_SERVICE.equals(name)) {
            return mImageLoader;
        } else {
            return super.getSystemService(name);
        }
    }
}
