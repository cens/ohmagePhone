package edu.ucla.cens.andwellness;

import java.io.File;

import edu.ucla.cens.andwellness.prompts.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.service.UploadReceiver;
import edu.ucla.cens.andwellness.storagemonitor.StorageMonitorService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

public class BackgroundManager {
	
	private static final String TAG = "BACKGROUND_MANAGER";

	public static void initComponents(Context context) {
		
		Log.i(TAG, "initializing application components");
		
		Context appContext = context.getApplicationContext();
		
		new File(PhotoPrompt.IMAGE_PATH).mkdirs();
		
		//uploadservice
		AlarmManager alarms = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);
		Intent intentToFire = new Intent(UploadReceiver.ACTION_UPLOAD_ALARM);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intentToFire, 0);
		//alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), pendingIntent);
		alarms.cancel(pendingIntent);
		alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_HOUR, pendingIntent);
		Log.i(TAG, "UploadReceiver repeating alarm set");
		
		//storagemonitor
		appContext.startService(new Intent(appContext, StorageMonitorService.class));
		Log.i(TAG, "started storage monitor service");
		
		
	}
}
