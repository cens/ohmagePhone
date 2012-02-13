package org.ohmage.responsesync;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.systemlog.Log;

public class ResponseSyncReceiver extends BroadcastReceiver {
	
	//alarm to check for new data while phone is plugged in
	public static final String ACTION_FBSYNC_ALARM = "org.ohmage.responsesync.ACTION_FBSYNC_ALARM";
	private static final long ALARM_FREQ = AlarmManager.INTERVAL_HOUR;
	private static final String TAG = "ResponseSyncReceiver"; 
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.i(TAG, "Broadcast received: " + action);
		
		//When the alarm goes off, get battery change sticky intent, if plugged in, start sync
		if (ResponseSyncReceiver.ACTION_FBSYNC_ALARM.equals(action)) {
			Context appContext = context.getApplicationContext();
			Intent battIntent = appContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			int level = battIntent.getIntExtra("level", -1);
			int scale = battIntent.getIntExtra("scale", -1);
			float percent = (float) level * 100 / (float) scale;
			Log.i(TAG, "Battey level: " + percent + "% ("+ level + " / " + scale + ")");
			if (percent > 20) {
				Log.i(TAG, "Power is not low.");
				Log.i(TAG, "Starting ResponseSyncService.");
				
				WakefulIntentService.sendWakefulWork(context, ResponseSyncService.class);
			} else {
				Log.i(TAG, "Power is low.");
				Log.i(TAG, "Not starting ResponseSyncService.");
			}
		}
	}

}
