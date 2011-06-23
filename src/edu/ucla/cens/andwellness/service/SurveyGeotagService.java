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
package edu.ucla.cens.andwellness.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.systemlog.Log;

public class SurveyGeotagService extends Service {

	private static final String TAG = "SurveyGeotagService";
	
	public static final String LOCATION_VALID = "valid";
	public static final String LOCATION_INACCURATE = "inaccurate";
	public static final String LOCATION_STALE = "stale";
	public static final String LOCATION_UNAVAILABLE = "unavailable";
	
	public static final long LOCATION_STALENESS_LIMIT = 5 * 60 * 1000;
	public static final long LOCATION_ACCURACY_THRESHOLD = 30;
	private static final long UPDATE_FREQ = 0; //5 * 60 * 1000;
	
	
	private LocationManager mLocManager;
	private AlarmManager mAlarmManager;
	private int updateCounter;
	private PowerManager.WakeLock wl;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		updateCounter = 0;
		
		mLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_FREQ, 0, mLocationListener);
		mLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_FREQ, 0, mLocationListener);
		
		mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		Intent intentToFire = new Intent(SurveyGeotagTimeoutReceiver.ACTION_GEOTAG_TIMEOUT_ALARM);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, intentToFire, 0);
		mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + LOCATION_STALENESS_LIMIT, pendingIntent);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
	    wl.acquire();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		mLocManager.removeUpdates(mLocationListener);
		
		if (wl != null && wl.isHeld()) {
			wl.release();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private LocationListener mLocationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			Log.i(TAG, location.getProvider() + " provided location: " + location.toString());
			
			if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
				if (location.getAccuracy() < LOCATION_ACCURACY_THRESHOLD) {
					Log.i(TAG, "Accuracy threshold reached.");
					
					DbHelper dbHelper = new DbHelper(SurveyGeotagService.this);
					dbHelper.updateRecentRowLocations(LOCATION_VALID, location.getLatitude(), location.getLongitude(), location.getProvider(), location.getAccuracy(), location.getTime());
					
					stopSelf();
				}
			}
		}
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

	};
	
}
