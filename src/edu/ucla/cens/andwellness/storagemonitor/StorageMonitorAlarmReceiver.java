package edu.ucla.cens.andwellness.storagemonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import edu.ucla.cens.systemlog.Log;

public class StorageMonitorAlarmReceiver extends BroadcastReceiver {
	
	private static final String TAG = "STORAGE_MONITOR_ALARM_RECIEVER";
	
	public static final String ACTION_STORAGE_MONITOR_ALARM = "edu.ucla.cens.andwellness.storagemonitor.ACTION_STORAGE_MONITOR_ALARM";

	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.i(TAG, "Recieved: " + intent.getAction());
		
		Intent startIntent = new Intent(context, StorageMonitorService.class);
		context.startService(startIntent);
	}

}
