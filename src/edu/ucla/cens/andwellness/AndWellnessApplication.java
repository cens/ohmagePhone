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
		new SingleChoiceCustomDbAdapter(this).clearAll();
		new MultiChoiceCustomDbAdapter(this).clearAll();
		
		//clear images
		try {
			Utilities.delete(new File(PhotoPrompt.IMAGE_PATH));
		} catch (IOException e) {
			Log.e(TAG, "Error deleting images", e);
		}
	}
}
