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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import edu.ucla.cens.andwellness.db.DbHelper;

public class SurveyGeotagTimeoutReceiver extends BroadcastReceiver {

	private static final String TAG = "SurveyGeotagTimeoutReceiver";
	
	public static final String ACTION_GEOTAG_TIMEOUT_ALARM = "edu.ucla.cens.andwellness.service.ACTION_GEOTAG_TIMEOUT_ALARM";
	
	private static final float LOCATION_LAST_RESORT_ACCURACY_THRESHOLD = 1000;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		long now = System.currentTimeMillis();
		
		if (ACTION_GEOTAG_TIMEOUT_ALARM.equals(intent.getAction())) {
			if (context.stopService(new Intent(context, SurveyGeotagService.class)) ) {
				
				LocationManager locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
				Location networkLocation = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				Location gpsLocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				String status = SurveyGeotagService.LOCATION_UNAVAILABLE;
				
				boolean isGpsFresh = false;
				boolean isGpsAccurate = false;
				boolean isNetworkFresh = false;
				boolean isNetworkAccurate = false;
				
				if (gpsLocation != null) {
					isGpsFresh = now - gpsLocation.getTime() < SurveyGeotagService.LOCATION_STALENESS_LIMIT;
					isGpsAccurate = gpsLocation.getAccuracy() < LOCATION_LAST_RESORT_ACCURACY_THRESHOLD;
				}
				
				if (networkLocation != null) {
					isNetworkFresh = now - networkLocation.getTime() < SurveyGeotagService.LOCATION_STALENESS_LIMIT;
					isNetworkAccurate = networkLocation.getAccuracy() < LOCATION_LAST_RESORT_ACCURACY_THRESHOLD;
				}
				
				if(gpsLocation != null && networkLocation != null) {
					if (now - gpsLocation.getTime() < SurveyGeotagService.LOCATION_STALENESS_LIMIT) {
						if (gpsLocation.getAccuracy() < LOCATION_LAST_RESORT_ACCURACY_THRESHOLD) {
							if (gpsLocation.getAccuracy() < networkLocation.getAccuracy()) {
								status = SurveyGeotagService.LOCATION_VALID;
								updateLocInDb(context, status, gpsLocation);
							} else {
								if (now - networkLocation.getTime() < SurveyGeotagService.LOCATION_STALENESS_LIMIT) {
									
								}
							}
						} else {
							status = SurveyGeotagService.LOCATION_INACCURATE;
						}
					} else {
						status = SurveyGeotagService.LOCATION_STALE;
					}
				}
				
				if(gpsLocation != null && networkLocation != null) {
					//if gps is fresh and accurate, just use it, ignoring that network might be fresher or more accurate
					if (isGpsFresh && isGpsAccurate) { // && gpsLocation.getAccuracy() <= networkLocation.getAccuracy()) {
						status = SurveyGeotagService.LOCATION_VALID;
						updateLocInDb(context, status, gpsLocation);
						
					} else if (isNetworkFresh && isNetworkAccurate) {
						status = SurveyGeotagService.LOCATION_VALID;
						updateLocInDb(context, status, networkLocation);
						
					} else if (isGpsFresh || isNetworkFresh) {
						status = SurveyGeotagService.LOCATION_INACCURATE;
						//which to upload?
						//if gps is fresh, always use it, regardless of accuracy, if not, then use network
						if (isGpsFresh) {
							updateLocInDb(context, status, gpsLocation);
						} else {
							updateLocInDb(context, status, networkLocation);
						}
					} else {
						status = SurveyGeotagService.LOCATION_STALE;
						//which to upload?
						//upload the fresher one
						if (gpsLocation.getTime() >= networkLocation.getTime()) {
							updateLocInDb(context, status, gpsLocation);
						} else {
							updateLocInDb(context, status, networkLocation);
						}
					} 
					
				} else if (gpsLocation != null) {
					if (isGpsFresh && isGpsAccurate) {
						status = SurveyGeotagService.LOCATION_VALID;
					} else if (isGpsFresh) {
						status = SurveyGeotagService.LOCATION_INACCURATE;
					} else {
						status = SurveyGeotagService.LOCATION_STALE;
					}
					
					updateLocInDb(context, status, gpsLocation);
					
					/*if (now - gpsLocation.getTime() < SurveyLocationService.LOCATION_STALENESS_LIMIT && gpsLocation.getAccuracy() < ACCURACY_THRESHOLD) {
						updateLocInDb(context, gpsLocation);
					}*/
					
				} else if (networkLocation != null) {
					if (isNetworkFresh && isNetworkAccurate) {
						status = SurveyGeotagService.LOCATION_VALID;
					} else if (isNetworkFresh) {
						status = SurveyGeotagService.LOCATION_INACCURATE;
					} else {
						status = SurveyGeotagService.LOCATION_STALE;
					}
					
					updateLocInDb(context, status, networkLocation);
					
					/*if (now - networkLocation.getTime() < SurveyLocationService.LOCATION_STALENESS_LIMIT && networkLocation.getAccuracy() < ACCURACY_THRESHOLD) {
						updateLocInDb(context, networkLocation);
					}*/
				} 
			}
		}
	}


	private void updateLocInDb(Context context, String status, Location loc) {
		if (loc != null) {
			
			DbHelper dbHelper = new DbHelper(context);
			dbHelper.updateRecentRowLocations(status, loc.getLatitude(), loc.getLongitude(), loc.getProvider(), loc.getAccuracy(), loc.getTime());
		}
	}

}
