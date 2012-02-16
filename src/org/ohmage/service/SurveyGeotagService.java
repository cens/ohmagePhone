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

package org.ohmage.service;

import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Response;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import java.util.LinkedList;
import java.util.List;

public class SurveyGeotagService extends WakefulService implements LocationListener{

	private static final String TAG = "SurveyGeotagService";

	public static final String LOCATION_VALID = "valid";
	public static final String LOCATION_UNAVAILABLE = "unavailable";

	public static final long LOCATION_STALENESS_LIMIT = 2 * 60 * 1000;
	public static final long LOCATION_ACCURACY_THRESHOLD = 30;
	private static final long UPDATE_FREQ = 0; // 5 * 60 * 1000;

	private LocationManager mLocManager;

	private final List<ResponseLocationSaver> responses = new LinkedList<ResponseLocationSaver>();

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// The timer expired without a gps location so tell the saver to save the network location
			((ResponseLocationSaver) msg.obj).saveLocation(null);

			// Remove the response from the queue
			responses.remove(msg.obj);

			// If there are no more responses we should stop
			if(!mHandler.hasMessages(0)) {
				releaseLock();
				stopSelf();
			}
		}
	};

	private class ResponseLocationSaver implements LocationListener {
		private final long mId;
		private Location mLocation;

		public ResponseLocationSaver(long id) {
			mId = id;
			mLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_FREQ, 0, this);
		}

		/**
		 * Saves this response to the db with the given location. If the
		 * location provided is null, we will try to use the network location
		 * that we have
		 * 
		 * @param location
		 */
		public void saveLocation(Location location) {
			//Remove our location listener if it still exists
			mLocManager.removeUpdates(this);

			// The default values that say this response is no longer waiting for a location
			ContentValues values = new ContentValues();
			values.put(Responses.RESPONSE_STATUS, Response.STATUS_STANDBY);
			values.put(Responses.RESPONSE_LOCATION_STATUS, LOCATION_UNAVAILABLE);

			// If the location provided is null, use the network location
			if(location == null)
				location = mLocation;

			// If we have a location, add the values
			if(location != null) {
				values.put(Responses.RESPONSE_LOCATION_STATUS, LOCATION_VALID);
				values.put(Responses.RESPONSE_LOCATION_LATITUDE, location.getLatitude());
				values.put(Responses.RESPONSE_LOCATION_LONGITUDE, location.getLongitude());
				values.put(Responses.RESPONSE_LOCATION_PROVIDER, location.getProvider());
				values.put(Responses.RESPONSE_LOCATION_ACCURACY, location.getAccuracy());
				values.put(Responses.RESPONSE_LOCATION_TIME, location.getTime());
			}

			// Update the db with the location information
			SurveyGeotagService.this.getContentResolver().update(
					Responses.buildResponseUri(mId), values, Responses.RESPONSE_STATUS + "=" + Response.STATUS_WAITING_FOR_LOCATION, null);
		}

		@Override
		public void onLocationChanged(Location location) {
			// If this location is accurate at all, save it as the network location
			if(location.hasAccuracy()) {
				mLocation = location;
				mLocManager.removeUpdates(this);
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
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Create the location manager and start listening to the GPS
		mLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_FREQ, 0, this);
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		// Create a response location saver to get a location for this response and send it to the handler
		ResponseLocationSaver response = new ResponseLocationSaver(ContentUris.parseId(intent.getData()));
		responses.add(response);
		Message msg = mHandler.obtainMessage(0, response);
		mHandler.sendMessageDelayed(msg, LOCATION_STALENESS_LIMIT);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Make sure the location listener has been removed
		mLocManager.removeUpdates(this);

		// If there are any response location savers still here we need to remove them
		flushResponses();
	}

	/**
	 * For all the responses this should set the network location if it exists
	 */
	private void flushResponses() {
		setResponsesLocation(null);
	}

	/**
	 * For all the responses set a location
	 * @param location
	 */
	private void setResponsesLocation(Location location) {
		mHandler.removeMessages(0);
		for (ResponseLocationSaver response : responses) {
			response.saveLocation(location);
		}
		responses.clear();
	}

	@Override
	public void onLocationChanged(Location location) {
		if (locationValid(location))
			// If we have a good enough location, we can save the responses and finish the service
			mLocManager.removeUpdates(this);

		try {
			setResponsesLocation(location);
		} finally {
			releaseLock();
			stopSelf();
		}
	}

	/**
	 * Checks to see that this location is accurate enough
	 * @param location
	 * @return
	 */
	public static boolean locationValid(Location location) {
		return LocationManager.GPS_PROVIDER.equals(location.getProvider()) && location != null
				&& location.hasAccuracy() && (location.getAccuracy() < LOCATION_ACCURACY_THRESHOLD);
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

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
