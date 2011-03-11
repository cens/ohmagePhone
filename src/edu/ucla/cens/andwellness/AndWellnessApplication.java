package edu.ucla.cens.andwellness;

import android.app.Application;
import android.util.Log;

public class AndWellnessApplication extends Application {
	
	private static final String TAG = "AndWellnessApplication";

	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.i(TAG, "onCreate()");
		
		edu.ucla.cens.systemlog.Log.initialize(this, "AndWellness");
	}
	

}
