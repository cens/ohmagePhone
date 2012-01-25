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

import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;

import org.ohmage.db.DbHelper;
import org.ohmage.prompt.multichoicecustom.MultiChoiceCustomDbAdapter;
import org.ohmage.prompt.singlechoicecustom.SingleChoiceCustomDbAdapter;
import org.ohmage.triggers.glue.TriggerFramework;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.ContentHandler;
import java.net.URLStreamHandlerFactory;

public class OhmageApplication extends Application {
	
	private static final String TAG = "OhmageApplication";
	
	public static final String VIEW_MAP = "ohmage.intent.action.VIEW_MAP";
	
    private static final int IMAGE_TASK_LIMIT = 3;

    // 50% of available memory, up to a maximum of 32MB
    private static final long IMAGE_CACHE_SIZE = Math.min(Runtime.getRuntime().maxMemory() / 2,
            32 * 1024 * 1024);

    private ImageLoader mImageLoader;

	private static OhmageApplication self;
    
	@Override
	public void onCreate() {
		super.onCreate();
		
		self = this;

		Log.i(TAG, "onCreate()");
		
		edu.ucla.cens.systemlog.Log.initialize(this, "Ohmage");
		
        mImageLoader = createImageLoader(this);

        int currentVersionCode = 0;
        
        try {
			currentVersionCode = getPackageManager().getPackageInfo("org.ohmage", 0).versionCode;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "unable to retrieve current version code", e);
		}
		
		SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);
		int lastVersionCode = prefs.getLastVersionCode();
		boolean isFirstRun = prefs.isFirstRun();
		
		if (currentVersionCode != lastVersionCode && !isFirstRun) {
			BackgroundManager.initComponents(this);
			
			prefs.setLastVersionCode(currentVersionCode);
		}
	}
	
	public void resetAll() {
		//clear everything?
		
		//clear triggers
		TriggerFramework.resetAllTriggerSettings(this);
		
		//clear shared prefs
		new SharedPreferencesHelper(this).clearAll();
		
		//clear user prefs
		new UserPreferencesHelper(this).clearAll();

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
			Utilities.delete(getImageDirectory(this));
		} catch (IOException e) {
			Log.e(TAG, "Error deleting images", e);
		}
	}
	
    private static ImageLoader createImageLoader(Context context) {
        // Install the file cache (if it is not already installed)
        OhmageCache.install(context);
        
        // Just use the default URLStreamHandlerFactory because
        // it supports all of the required URI schemes (http).
        URLStreamHandlerFactory streamFactory = null;

        // Load images using a BitmapContentHandler
        // and cache the image data in the file cache.
        ContentHandler bitmapHandler = OhmageCache.capture(new BitmapContentHandler(), null);

        // For pre-fetching, use a "sink" content handler so that the
        // the binary image data is captured by the cache without actually
        // parsing and loading the image data into memory. After pre-fetching,
        // the image data can be loaded quickly on-demand from the local cache.
        ContentHandler prefetchHandler = OhmageCache.capture(OhmageCache.sink(), null);

        // Perform callbacks on the main thread
        Handler handler = null;
        
        return new ImageLoader(IMAGE_TASK_LIMIT, streamFactory, bitmapHandler, prefetchHandler,
                IMAGE_CACHE_SIZE, handler);
    }

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
    
    public static File getImageDirectory(Context context) {
    	try {
        	return new File(context.getExternalCacheDir(), "images");
    	} catch(NoSuchMethodError e) {
    		return new File(Environment.getExternalStorageDirectory(), "Android/data/org.ohmage/cache/images");
    	}
    }

	/**
	 * Static reference from the Application to return the context
	 * @return the application context
	 */
	public static Context getContext() {
		return self;
	}

	/**
	 * Determines if we are running on release or debug
	 * @return true if we are running Debug
	 * @throws Exception
	 */
	public static boolean isDebugBuild()
	{
		PackageManager pm = getContext().getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo(getContext().getPackageName(), 0);
			return ((pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
}
