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
package edu.ucla.cens.andwellness.storagemonitor;

import java.io.File;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.os.SystemClock;
import edu.ucla.cens.systemlog.Log;

public class StorageMonitorService extends Service {

	private static final String TAG = "STORAGE_MONITOR_SERVICE";
	
	private static final long MONITORING_FREQ = AlarmManager.INTERVAL_FIFTEEN_MINUTES; //in minutes
	private static final int FREE_STORAGE_VERY_LOW_THRESHOLD = 2;
	private static final int FREE_STORAGE_LOW_THRESHOLD = 25;
	private static final int FREE_STORAGE_MEDIUM_THRESHOLD = 50;
	private static final int FREE_STORAGE_HIGH_THRESHOLD = 75;
	
	public static final String ACTION_STORAGE_REPORT = "edu.ucla.cens.andwellness.storagemonitor.ACTION_STORAGE_REPORT";
	public static final String ACTION_FREE_STORAGE_VERY_LOW = "edu.ucla.cens.andwellness.storagemonitor.ACTION_FREE_STORAGE_VERY_LOW";
	public static final String ACTION_FREE_STORAGE_LOW = "edu.ucla.cens.andwellness.storagemonitor.ACTION_FREE_STORAGE_LOW";
	public static final String ACTION_FREE_STORAGE_MEDIUM = "edu.ucla.cens.andwellness.storagemonitor.ACTION_FREE_STORAGE_MEDIUM";
	public static final String ACTION_FREE_STORAGE_HIGH = "edu.ucla.cens.andwellness.storagemonitor.ACTION_FREE_STORAGE_HIGH";
	public static final String ACTION_FREE_STORAGE_VERY_HIGH = "edu.ucla.cens.andwellness.storagemonitor.ACTION_FREE_STORAGE_VERY_HIGH";
	
	public static final String EXTRA_STORAGE_STATUS = "edu.ucla.cens.andwellness.storagemonitor.EXTRA_STORAGE_STATUS";
	public static final String EXTRA_FREE_STORAGE_PERCENT = "edu.ucla.cens.andwellness.storagemonitor.EXTRA_FREE_STORAGE_PERCENT";
	
	public static final int FREE_STORAGE_VERY_LOW = 1;
	public static final int FREE_STORAGE_LOW = 2;
	public static final int FREE_STORAGE_MEDIUM = 3;
	public static final int FREE_STORAGE_HIGH = 4;
	public static final int FREE_STORAGE_VERY_HIGH = 5;
	
		
	private AlarmManager alarms;
	private PendingIntent alarmIntent;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.i(TAG, "Service created.");
		
		alarms = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		
		Intent intentToFire = new Intent(StorageMonitorAlarmReceiver.ACTION_STORAGE_MONITOR_ALARM);
		alarmIntent = PendingIntent.getBroadcast(this, 0, intentToFire, 0);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		Log.i(TAG, "Service started.");
		
		//SharedPreferences prefs = getSharedPreferences("STORAGE_MONITOR_PREFS", Activity.MODE_PRIVATE);
		//boolean isMonitoring = prefs.getBoolean("IS_MONITORING", false);
		boolean isMonitoring = true; 
			
		if (isMonitoring) {
			checkDiskSpace();
			
			int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
			long timeForNextCheck = SystemClock.elapsedRealtime() + MONITORING_FREQ;
			alarms.set(alarmType, timeForNextCheck, alarmIntent);
			Log.i(TAG, "Alarm set.");
		}
		else {
			alarms.cancel(alarmIntent);
			Log.i(TAG, "Alarm cancelled.");
		}
		
		Log.i(TAG, "Stopping self.");
		stopSelf();
	}
	
	

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.i(TAG, "Service destroyed.");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	
	private void checkDiskSpace() {
		
		File path = Environment.getDataDirectory();
    	StatFs stat = new StatFs(path.getPath());
    	int availableBlocks = stat.getAvailableBlocks();
    	int totalBlocks = stat.getBlockCount();
    	int percentFree = availableBlocks * 100 / totalBlocks; 
    	
    	//String action = ACTION_FREE_STORAGE_VERY_HIGH;
    	String action = ACTION_STORAGE_REPORT;
    	int status = FREE_STORAGE_VERY_HIGH;
    	
    	if (percentFree >= FREE_STORAGE_HIGH_THRESHOLD) {
    		//action = ACTION_FREE_STORAGE_HIGH;
    		status = FREE_STORAGE_HIGH;
    	} else if (percentFree >= FREE_STORAGE_MEDIUM_THRESHOLD) {
    		//action = ACTION_FREE_STORAGE_MEDIUM;
    		status = FREE_STORAGE_MEDIUM;
    	} else if (percentFree >= FREE_STORAGE_LOW_THRESHOLD) {
    		//action = ACTION_FREE_STORAGE_LOW;
    		status = FREE_STORAGE_LOW;
    	} else if (percentFree >= FREE_STORAGE_VERY_LOW_THRESHOLD) {
    		//action = ACTION_FREE_STORAGE_VERY_LOW;
    		status = FREE_STORAGE_VERY_LOW;
    	}
    	
    	Log.i(TAG, "Broadcasting: " + action);
    	
    	Intent broadcastIntent = new Intent();
    	broadcastIntent.setAction(action);
    	broadcastIntent.putExtra(EXTRA_STORAGE_STATUS, status);
    	broadcastIntent.putExtra(EXTRA_FREE_STORAGE_PERCENT, percentFree);
    	sendBroadcast(broadcastIntent);
	}
}
