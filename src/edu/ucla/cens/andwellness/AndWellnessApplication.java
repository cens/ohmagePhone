package edu.ucla.cens.andwellness;

import edu.ucla.cens.systemlog.Log;
import android.app.Application;

public class AndWellnessApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.initialize(this, "AndWellness");
	}
	

}
