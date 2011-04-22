package edu.ucla.cens.andwellness;

import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.prompts.multichoicecustom.MultiChoiceCustomDbAdapter;
import edu.ucla.cens.andwellness.prompts.singlechoicecustom.SingleChoiceCustomDbAdapter;
import android.app.Application;
import android.util.Log;
//import edu.ucla.cens.systemlog.Log;

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
		
		//clear shared prefs
		new SharedPreferencesHelper(this).clearAll();
		
		//clear db
		new DbHelper(this).clearAll();
		
		//clear custom type dbs
		new SingleChoiceCustomDbAdapter(this).clearAll();
		new MultiChoiceCustomDbAdapter(this).clearAll();
		
		//clear triggers
		//
		
		
	}
}
