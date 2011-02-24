package edu.ucla.cens.andwellness.service;

import edu.ucla.cens.andwellness.BackgroundManager;
import edu.ucla.cens.andwellness.SharedPreferencesHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
	public static final String TAG = "BOOT_RECEIVER";
	
	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.i(TAG, "onReceive");
		
		final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(context);
		boolean isFirstRun = preferencesHelper.isFirstRun();
		
		if (isFirstRun) {
			Log.i(TAG, "this is the first run");
			
		} else {
			Log.i(TAG, "this is not the first run");
			
			//start components
			BackgroundManager.initComponents(context);
		}
	}

}
