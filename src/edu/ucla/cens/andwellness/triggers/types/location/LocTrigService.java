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
package edu.ucla.cens.andwellness.triggers.types.location;

/* 
 * The service which performs energy efficient 
 * location sampling and location change detection. 
 * 
 * The location updates are duty cycled based on the 
 * user speed and proximity to the nearest defined 
 * location.
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import edu.ucla.cens.accelservice.IAccelService;
import edu.ucla.cens.andwellness.db.Campaign;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.triggers.config.LocTrigConfig;
import edu.ucla.cens.andwellness.triggers.utils.SimpleTime;
import edu.ucla.cens.wifigpslocation.ILocationChangedCallback;
import edu.ucla.cens.wifigpslocation.IWiFiGPSLocationService;

public class LocTrigService extends Service 
			 implements LocationListener {

	private static final String DEBUG_TAG = "LocationTrigger";
	private static final String SYSTEM_LOG_TAG = "LocationTrigger";
	
	private static final String REMOTE_CLIENT_NAME = 
				LocTrigService.class.getName() + ".remote_client";
	
	private static final String WAKE_LOCK_TAG = 
					LocTrigService.class.getName() + ".wake_lock";
	private static final String RECR_WAKE_LOCK_TAG = 
					LocTrigService.class.getName() + ".recr_wake_lock";
	
	public static final String ACTION_START_TRIGGER = 
					LocTrigService.class.getName() + ".start_trigger";
	public static final String ACTION_REMOVE_TRIGGER = 
					LocTrigService.class.getName() + ".stop_trigger";
	public static final String ACTION_RESET_TRIGGER = 
					LocTrigService.class.getName() + ".reset_trigger";
	public static final String ACTION_UPDATE_LOCATIONS = 
					LocTrigService.class.getName() + ".update_locations";
	private static final String ACTION_HANDLE_ALARM =
					LocTrigService.class.getName() + ".handle_alarm";
	public static final String ACTION_UPDATE_TRACING_STATUS =
					LocTrigService.class.getName() + ".update_tracing";
	
	public static final String KEY_TRIG_ID = "trigger_id";
	public static final String KEY_TRIG_DESC = "trigger_description";
	private static final String KEY_ALARM_ACTION = "alarm_action";
	private static final String KEY_SAMPLING_ALARM_EXTRA = "alarm_extra";
	
	/* Invalid category id */
	private static final int CATEG_ID_INVAL = -1;
	
	/* Alarm actions */
	private static final String ACTION_ALRM_PASS_THROUGH = 
			"edu.ucla.cens.andwellness.triggers.types.location.LocTrigService.PASS_THROUGH";
	private static final String ACTION_ALRM_GPS_SAMPLE = 
			"edu.ucla.cens.andwellness.triggers.types.location.LocTrigService.GPS_SAMPLE";
	private static final String ACTION_ALRM_GPS_TIMEOUT = 
			"edu.ucla.cens.andwellness.triggers.types.location.LocTrigService.GPS_TIMEOUT";
	private static final String ACTION_ALRM_SRV_KEEP_ALIVE = 
			"edu.ucla.cens.andwellness.triggers.types.location.LocTrigService.KEEP_ALIVE";	
	private static final String ACTION_ALM_TRIGGER_ALWAYS = 
			"edu.ucla.cens.andwellness.triggers.types.location.LocTrigService.TRIGGER_ALWAYS";
	private static final String DATA_PREFIX_TRIG_ALWAYS_ALM = 
			"locationtrigger://edu.ucla.cens.triggers.types.location/";
	
	//Time value for the alarm to keep the service alive
	private static final long SERV_KEEP_ALIVE_TIME = 300000; //5min
	//Time value for the alarm to check if the user is passing through
	private static final long PASS_THROUGH_TIME = 180000; //3min
	private static final long STALE_LOC_TIME = 180000; //3min
	//The number of accurate samples to collect during every
	//sampling instance
	private static final int SAMPLES_LIMIT = 10;
	//GPS timeout alarm time value. The GPS is timed out after
	//this if it cannot obtain the above number of accurate 
	//samples.
	private static final long GPS_TIMEOUT = 45000; //45s
	//The threshold value to use when checking if a location
	//belongs to a category
	private static final float CATEG_ACCURACY_MARGIN = 20; //m
	//The maximum value of GPS duty cycle interval
	private static final long MAX_SLEEP_TIME = 360000; //6 mins
	//The minimum value of GPS duty cycle interval
	private static final long MIN_SLEEP_TIME = 30000; //30sec
	//The actual sleep time is calculated based on the speed. 
	//The distance (the maximum of which is given above) is calculated
	//from the proximity. The proximity is divided by this value get
	//the distance value which should be divided by the speed to get the
	//sleep time.
	private static final float SLEEP_DIST_FACTOR = 10;
	//Discard samples below this accuracy
	private static final float INACCURATE_SAMPLE_THRESHOLD = 17; //m
	//Ignore the speed below this value
	private static final float SPEED_MIN_THRESHOLD = 0.05F; //m/s
	//Calculate the speed from the displacement only if the user has
	//moved at least this much.
	private static final float SPEED_CALC_MIN_DISPLACEMENT 
							   = 4 * INACCURATE_SAMPLE_THRESHOLD;
	//Minimum accelerometer value for motion detection
	private static final double MOTION_DETECT_ACCEL_THRESHOLD = 5;
	//Use motion detection only if the sleep time is greater than this value
	private static final long SLEEP_TIME_MIN_MOTION_DETECT = 60000; //1 min
	//Ignore a movement callback if the motion detection was started
	//within this interval
	private static final long MOTION_DETECT_DELAY = 60000; //1min
	
	//Wake lock for GPS sampling
	private PowerManager.WakeLock mWakeLock = null;
	//Wake lock for alarm receiver
	private static PowerManager.WakeLock mRecvrWakeLock = null;
	
	private IWiFiGPSLocationService mWiFiGPSServ = null;
	private IAccelService mAccelServ = null;
	private ILocationChangedCallback mMotionDetectCB= null;
	private ServiceConnection mWifiGPSServConn = null;
	private ServiceConnection mAccelServConn = null;
	
	//Number of samples collected in the current duty cycle
	private int mNSamples = 0;
	//Number of initial samples collected when the very first time the
	//sampling is started
	private int mNInitialSamples = 0;
	//Current sleep time before the next duty cycle
	private long mCurrSleepTime = 0;
	//Speed calculated in the previous duty cycle
	private float mPrevSpeed = 0;
	//Speed calculated in the current duty cycle
	private float mCurrSpeed = 0;
	//Current value of the proximity distance
	private float mCurrProxDist = 0;
	//The latest location update received
	private Location mLastKnownLoc = new Location(LocationManager.GPS_PROVIDER);
	//Time stamp of the above location update
	private long mLastKnownLocTime = 0;
	//Location update received in the previous duty cycle
	private Location mLastKnownLocBackup = new Location(LocationManager.GPS_PROVIDER);
	//Time stamp of the above location update
	private long mLastKnownLocTimeBackup = 0;
	//The last category (place) where the user was known to be in
	private int mLatestCateg = CATEG_ID_INVAL;
	//Flag to check if a pass-through check is initiated
	private boolean mPassThroughChecking = false;
	//The category id for which the pass through check is being performed
	private int mPassThroughCheckCateg = CATEG_ID_INVAL;
	private long mCategPrevTS = LocTrigDB.TIME_STAMP_INVALID;
	//Flag to check if the sampling is started at all
	private boolean mSamplingStarted = false;
	private boolean mGPSStarted = false;
	private long mMotionDetectTS = 0;
	private WifiManager.WifiLock mWifiLock = null;
	//Status of location tracing
	private boolean mLocTraceEnabled = false;
	//Upload trace even if it is similar to the previous trace
	private boolean mLocTraceUploadAlways = false;
	//Latest location uploaded 
	private Location mLastLocTrace = new Location(LocationManager.GPS_PROVIDER);
	
	//Handler to handle motion detection callback
	//It is require to run the handling in the current thread
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			handleWifiGPSLocChange();
		}
	};
	
	//The list of all locations to watch for
	private LinkedList<LocListItem> mLocList;


	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.i(DEBUG_TAG, "LocTrigService: onCreate");
	
		//Let the service live forever
		setKeepAliveAlarm();
		
		//Cache the locations
		mLocList = new LinkedList<LocListItem>();
		populateLocList();
		
		initState(); 
		
		PowerManager powerMan = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = powerMan.newWakeLock(
    				PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);

		if(LocTrigConfig.useNetworkLocation) {
			Log.i(DEBUG_TAG, "LocTrigService: Using network location");
			WifiManager wifiMan = (WifiManager) getSystemService(WIFI_SERVICE);
			mWifiLock = wifiMan.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, DEBUG_TAG);
		}
		
		if(LocTrigConfig.useMotionDetection) {
			Log.i(DEBUG_TAG, "LocTrigService: Using motion detection");
		}
		
		mMotionDetectCB = new ILocationChangedCallback.Stub() {
			
			@Override
			public void locationChanged() throws RemoteException {
				if(LocTrigConfig.useMotionDetection) {
					mHandler.sendMessage(mHandler.obtainMessage());
				}
				
			}
		};
	
		mSamplingStarted = false;	
		
		super.onCreate();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.i(DEBUG_TAG, "LocTrigService: onStart");
		
		int trigId = intent.getIntExtra(KEY_TRIG_ID, -1);
		String trigDesc = intent.getStringExtra(KEY_TRIG_DESC);
		
		if(intent.getAction().equals(ACTION_START_TRIGGER)) {
			setTriggerAlwaysAlarm(trigId, trigDesc);
		}
		else if(intent.getAction().equals(ACTION_REMOVE_TRIGGER)) {
			cancelTriggerAlwaysAlarm(trigId);
		}
		else if(intent.getAction().equals(ACTION_RESET_TRIGGER)) {
			setTriggerAlwaysAlarm(trigId, trigDesc);
		}
		else if(intent.getAction().equals(ACTION_HANDLE_ALARM)) {
			handleAlarm(intent.getExtras());
		}
		else if(intent.getAction().equals(ACTION_UPDATE_LOCATIONS)) {
			populateLocList();
		}
		else if(intent.getAction().equals(ACTION_UPDATE_TRACING_STATUS)) {
			updateLocTracingState();
		}

		updateSamplingStatus();
		
		releaseRecvrWakeLock();
		
		super.onStart(intent, startId);
	}
	
	@Override
	public void onDestroy() {
		Log.i(DEBUG_TAG, "LocTrigService: onDestroy");

		stopGPS();
		
		mLocList.clear();
		
		disconnectRemoteServices();
		
		releaseWakeLock();
		releaseRecvrWakeLock();
		
		super.onDestroy();
	}
	
	private void connectToRemoteServices() {
		//Connect to WIFIGPS
		bindService(new Intent(IWiFiGPSLocationService.class.getName()), 
					mWifiGPSServConn, Context.BIND_AUTO_CREATE);
		
		//Connect to ACCEL
		bindService(new Intent(IAccelService.class.getName()), 
					mAccelServConn, Context.BIND_AUTO_CREATE);
	}
	
	private void disconnectRemoteServices() {
		if(mAccelServConn != null && mAccelServ != null) {
			unbindService(mAccelServConn);
		}
		
		if(mWifiGPSServConn != null && mWiFiGPSServ != null) {
			unbindService(mWifiGPSServConn);
		}
		
		mAccelServ = null;
		mAccelServConn = null;
		
		mWiFiGPSServ = null;
		mWifiGPSServConn = null;
	}
	
	private void acquireWakeLock() {
		if(!mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}
	}
	
	private void releaseWakeLock() {
		if(mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}
	
    private static void acquireRecvrWakeLock(Context context) {
		
    	if(mRecvrWakeLock == null) {
			PowerManager powerMan = (PowerManager) context.
									getSystemService(POWER_SERVICE);
			
			mRecvrWakeLock = powerMan.newWakeLock(
	    				PowerManager.PARTIAL_WAKE_LOCK, RECR_WAKE_LOCK_TAG);
			mRecvrWakeLock.setReferenceCounted(true);
    	}
    	
		if(!mRecvrWakeLock.isHeld()) {
			mRecvrWakeLock.acquire();
		}
	}
	
	private static void releaseRecvrWakeLock() {
	
		if(mRecvrWakeLock == null) {
			return;
		}
		
		if(mRecvrWakeLock.isHeld()) {
			mRecvrWakeLock.release();
		}
	}
   
	
	private void handleAlarm(Bundle extras) {
		
		String alm = extras.getString(KEY_ALARM_ACTION);
		
		if(alm.equals(ACTION_ALM_TRIGGER_ALWAYS)) {
			handleTriggerAlwaysAlarm(extras.getInt(KEY_TRIG_ID));
		}
		else if(alm.equals(ACTION_ALRM_GPS_SAMPLE)) {
			handleSampleGPSAlarm();
		}
		else if(alm.equals(ACTION_ALRM_GPS_TIMEOUT)) {
			handleGPSTimeoutAlarm();
		}
		else if(alm.equals(ACTION_ALRM_PASS_THROUGH)) {
			handlePassThroughCheckAlarm(extras.getInt(KEY_SAMPLING_ALARM_EXTRA));
		}
	}
	
	private void setKeepAliveAlarm() {
		Intent i = new Intent(ACTION_ALRM_SRV_KEEP_ALIVE);
		
		//set the alarm if not already existing
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
				   		   PendingIntent.FLAG_NO_CREATE);
		
		AlarmManager alarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
		if(pi != null) {
			alarmMan.cancel(pi);
			pi.cancel();
		}
		
    	pi = PendingIntent.getBroadcast(this, 0, i, 
    					   PendingIntent.FLAG_CANCEL_CURRENT);
    	
    	alarmMan.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
    						  SystemClock.elapsedRealtime() + SERV_KEEP_ALIVE_TIME, 
    						  SERV_KEEP_ALIVE_TIME, pi);
	}
	
    private Intent createTriggerAlwaysAlarmIntent(int trigId) {
    	Intent i = new Intent();
    	
    	i.setAction(ACTION_ALM_TRIGGER_ALWAYS);
    	i.setData(Uri.parse(DATA_PREFIX_TRIG_ALWAYS_ALM + trigId));
    	i.putExtra(KEY_TRIG_ID, trigId);
    	return i;
    }
	
	private void cancelTriggerAlwaysAlarm(int trigId) {
		Intent i = createTriggerAlwaysAlarmIntent(trigId);
		
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
				   		   	  PendingIntent.FLAG_NO_CREATE);
		
		if(pi != null) {
			//cancel the alarm
			AlarmManager alarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
			alarmMan.cancel(pi);
			pi.cancel();
		}
	}
	
	private void setTriggerAlwaysAlarm(int trigId, String trigDesc) {
		
		cancelTriggerAlwaysAlarm(trigId);
		
		LocTrigDesc desc = new LocTrigDesc();
		if(!desc.loadString(trigDesc)) {
			return;
		}
		
		if(!desc.shouldTriggerAlways()) {
			return;
		}
		
		Log.i(DEBUG_TAG, "LocTrigService: Setting trigger always alarm(" + 
				trigId + ", " + trigDesc + ")");
		
		Calendar target = Calendar.getInstance();
		target.set(Calendar.HOUR_OF_DAY, desc.getEndTime().getHour());
		target.set(Calendar.MINUTE, desc.getEndTime().getMinute());
		target.set(Calendar.SECOND, 0);
		
		LocationTrigger locTrig = new LocationTrigger();
		if(locTrig.hasTriggeredToday(this, trigId)) {
			target.add(Calendar.DAY_OF_YEAR, 1);
		}
		else if(System.currentTimeMillis() >= target.getTimeInMillis()) {
			target.add(Calendar.DAY_OF_YEAR, 1); 
		}
		
		Intent i = createTriggerAlwaysAlarmIntent(trigId);
	    PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
	    				   PendingIntent.FLAG_CANCEL_CURRENT);
	    
	    AlarmManager alarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
	    
	    Log.i(DEBUG_TAG, "LocTrigService: Calculated target time: " +
				  target.getTime().toString());
	    
	    long alarmTime = target.getTimeInMillis();
	    
	    /* Convert the alarm time to elapsed real time. 
	     * If we dont do this, a time change in the system might
	     * set off all the alarms and a trigger might go off before
	     * we get a chance to cancel it
	     */
	    long elapsedRT = alarmTime - System.currentTimeMillis();
	    if(elapsedRT <= 0) {
	    	Log.i(DEBUG_TAG, "LocTrigService: negative elapsed realtime - "
	    			+ "alarm not setting: "
					+ trigId);
	    	return;
	    }
	    
	    Log.i(DEBUG_TAG, "LocTrigService: Setting alarm for " + elapsedRT
				 + " millis into the future");
	    
	    alarmMan.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
					 SystemClock.elapsedRealtime() + elapsedRT, pi);
	}
	
	private void updateLocTracingState() {
		SharedPreferences prefs = 
						  PreferenceManager.getDefaultSharedPreferences(this);

		mLocTraceEnabled = prefs.getBoolean(
			        	   LocTrigTracingSettActivity.PREF_KEY_TRACING_STATUS, 
			        	   false); 
		
		mLocTraceUploadAlways = prefs.getBoolean(
								LocTrigTracingSettActivity.PREF_KEY_UPLOAD_ALWAYS, 
								false);
		
		Log.i(DEBUG_TAG, "LocTrigService: Updating tracing status to: " 
							+ mLocTraceEnabled + ", Upload always = "
							+ mLocTraceUploadAlways);
	}
	
 	private void initState() {
		Log.i(DEBUG_TAG, "LocTrigService: initState");
		
		mCurrSleepTime = 0;
        mCurrSpeed = 0;
        mPrevSpeed = 0;
        mCurrProxDist = 0;
        mNInitialSamples = 0;
        mLastKnownLoc.reset();
        mLastKnownLocBackup.reset();
        mLastKnownLocTime = 0;
        mLastKnownLocTimeBackup = 0;
        mLatestCateg = CATEG_ID_INVAL;
        mPassThroughChecking = false;
        mPassThroughCheckCateg = CATEG_ID_INVAL;
        mGPSStarted = false;
        mMotionDetectTS = 0;
        mCategPrevTS = LocTrigDB.TIME_STAMP_INVALID;
        
        mLastLocTrace.reset();
        mLastLocTrace.setTime(0);
        updateLocTracingState();       
	}
 	
	private void startSampling() {
		if(mSamplingStarted) {
			return;
		}

		Log.i(DEBUG_TAG, "LocTrigService: Starting sampling");
		
		startGPS();    
		mSamplingStarted = true;
	}
	
	private void stopSampling() {
		if(!mSamplingStarted) {
			return;
		}

		Log.i(DEBUG_TAG, "LocTrigService: Stopping sampling");
		
		stopGPS();
		
		//Remove all alarms
		cancelAllSamplingAlarms();
		
		mSamplingStarted = false;
		
		releaseWakeLock();
	}
	
	private void updateSamplingStatus() {
		LinkedList<Integer> actTrigs = new LinkedList<Integer>();
		LocationTrigger lt = new LocationTrigger();
		
		DbHelper dbHelper = new DbHelper(this);
		for (Campaign c : dbHelper.getCampaigns()) {
			actTrigs.addAll(lt.getAllActiveTriggerIds(this, c.mUrn));
		}
		
		
		//Start sampling if there are active surveys 
		//or if the location tracing is enabled.
		if(mLocTraceEnabled || 
		   (actTrigs.size() > 0 && mLocList.size() > 0)) {
			startSampling();
		}
		else {
			stopSampling();
		}
	}
	
	@SuppressWarnings("unchecked")
	private boolean hasUserMoved() {
	
		List<Double> fList = null;
		try {
			//TODO cast problem
			//AccelService must return a typed list
			fList = mAccelServ.getLastForce();
		} catch (RemoteException e) {
			Log.e(DEBUG_TAG, "LocTrigService: Exception while " +
						"getLastForce");
			Log.v(DEBUG_TAG, e.toString());
			return false;
		}
		
		if(fList == null) {
			Log.e(DEBUG_TAG, "LocTrigService: AccelService returned null" +
					" force list");
			return false;
		}
		
		if(fList.size() == 0) {
			Log.i(DEBUG_TAG, "LocTrigService: AccelService returned empty" +
						" force list");
			return false;
		}
		
		double mean = 0;
		for(double force : fList) {
			mean += force;
		}
		mean /= fList.size();
		
		double var = 0;
		for(double force : fList) {
			var += Math.pow(force - mean, 2);
		}
		var /= fList.size();
		var *= 1000;
		
		Log.i(DEBUG_TAG, "LocTrigService: Variance = " + var);
		
		if(var < MOTION_DETECT_ACCEL_THRESHOLD) {
			return false;
		}
		
		Log.i(DEBUG_TAG, "LocTrigService: Motion detected");
		return true;
	}
	
	private void handleWifiGPSLocChange() {
		Log.i(DEBUG_TAG, "LocTrigService: WifiGPS loc changed");
		
		long elapsed = SystemClock.elapsedRealtime() - mMotionDetectTS;
		if(elapsed < MOTION_DETECT_DELAY || 
				mCurrSleepTime < SLEEP_TIME_MIN_MOTION_DETECT) {
			
			Log.i(DEBUG_TAG, "LocTrigService: Too early, ignoring WifiGPS loc change");
			return;
		}
		
		try {
			if(mAccelServ == null || !mAccelServ.isRunning()) {
				Log.i(DEBUG_TAG, "Accel service is not running, " +
						"stopping motion detection");
				
				stopMotionDetection();
				return;
			}
		} catch (RemoteException e) {
			Log.e(DEBUG_TAG, "Error while checking accel status");
			return;
		}
		
		if(hasUserMoved()) {
			Log.i(DEBUG_TAG, "LocTrigService: Starting GPS due to movement");
			
			startGPS();
		}
	}
	
	private void registerMotionDetectionCB(){
		Log.i(DEBUG_TAG, "LocTrigService: Registering for WifiGPS CB");
		
		if(mWiFiGPSServ == null || mAccelServ == null) {
			Log.i(DEBUG_TAG, "LocTrigService: Not all services are connected yet." +
					" Skipping...");
			return;
		}
		
		Log.i(DEBUG_TAG, "LocTrigService: All services connected, " +
				"attempting to register");
		
		//Use the services opportunistically. Do not start
		//motion detection if the services are not already 
		//running
		try {
			if(!mWiFiGPSServ.isRunning() || !mAccelServ.isRunning()) {
				Log.i(DEBUG_TAG, "LocTrigService: Motion detection NOT started " +
						"as the services are not already running");
				
				disconnectRemoteServices();
				return;
			}
		
			mMotionDetectTS = SystemClock.elapsedRealtime();
		
			//Register a location change cb with wifigps
			mWiFiGPSServ.registerCallback(REMOTE_CLIENT_NAME, mMotionDetectCB);
			/*
			 * The following line is a hack/work-around. If this is not done, 
			 * the WifiGPS service will continue to run even after all the other
			 * have stopped. Essentially, the 'registerCallback' function above
			 * will increase the 'clientCount' inside the service even if we didnt
			 * start it explicitly. Thus, we need to call stop to nullify the effect.
			 */
			mWiFiGPSServ.stop(REMOTE_CLIENT_NAME);
		} catch (RemoteException e) {
			Log.e(DEBUG_TAG, "LocTrigService: Exception while " +
								"registering wifigps cb");
			Log.v(DEBUG_TAG, e.toString());
		}
	}
	
	private void startMotionDetection() {
		Log.i(DEBUG_TAG, "LocTrigService: Starting motion detection...");
		
		mWifiGPSServConn = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.i(DEBUG_TAG, "LocTrigService: WifiGPS disconnected");
				
				mWiFiGPSServ = null;
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(DEBUG_TAG, "LocTrigService: WifiGPS connected");
				
				mWiFiGPSServ = IWiFiGPSLocationService.Stub.asInterface(service);
				registerMotionDetectionCB();		}
		};
		
		
		mAccelServConn = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.i(DEBUG_TAG, "LocTrigService: AccelService disconnected");
				
				mAccelServ = null;
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(DEBUG_TAG, "LocTrigService: AccelService connected");
				
				mAccelServ = IAccelService.Stub.asInterface(service);
				registerMotionDetectionCB();
			}
		};
		
		connectToRemoteServices();
	}
	
	private void stopMotionDetection() {
		Log.i(DEBUG_TAG, "LocTrigService: Stopping motion detection...");
		
		if(mWiFiGPSServ != null) {
			try {
				mWiFiGPSServ.unregisterCallback(REMOTE_CLIENT_NAME, mMotionDetectCB);
				
				/* 
				 * A stop call is actually not required. But to be 
				 * on the safer side...
				 */
				mWiFiGPSServ.stop(REMOTE_CLIENT_NAME);
			} catch (RemoteException e) {
				Log.e(DEBUG_TAG, "LocTrigService: Exception while " +
									"stopping motion detection");
				Log.v(DEBUG_TAG, e.toString());
			}
		}
	
		disconnectRemoteServices();
	}
	
	private void startGPS() {
		if(mGPSStarted) {
			return;
		}
		
		if(LocTrigConfig.useMotionDetection) {
			stopMotionDetection();
		}
			
		//Get a wake lock for this duty cycle
		acquireWakeLock();
		
		mNSamples = 0;
		mPrevSpeed = mCurrSpeed;
		
		Log.i(DEBUG_TAG, "LocTrigService: Turning on location updates");
		
		LocationManager locMan = (LocationManager) getSystemService(LOCATION_SERVICE);
		locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				 						0, 0, this);
		
		//Use network location as well
		if(LocTrigConfig.useNetworkLocation) {
				  
			if(mWifiLock != null) {
				if(!mWifiLock.isHeld()) {
					mWifiLock.acquire();
				}
			}
			
			locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
						0, 0, this);
		}
		
		cancelSamplingAlarm(ACTION_ALRM_GPS_SAMPLE);
		//Set GPS timeout
		setSamplingAlarm(ACTION_ALRM_GPS_TIMEOUT, GPS_TIMEOUT, 0);
		
		mGPSStarted = true;
	}
	
	private void stopGPS() {
		if(!mGPSStarted) {
			return;
		}
		
		cancelSamplingAlarm(ACTION_ALRM_GPS_TIMEOUT);
		
		Log.i(DEBUG_TAG, "LocTrigService: Turning off location updates");
		
		LocationManager locMan = (LocationManager) getSystemService(LOCATION_SERVICE);
		locMan.removeUpdates(this);
		
		if(LocTrigConfig.useNetworkLocation) {
			if(mWifiLock != null) {
				if(mWifiLock.isHeld()) {
					mWifiLock.release();
				}
			}
		}
		
		mGPSStarted = false;
	}
	
	/*
	 * Convenience wrapper for SystemLog api.
	 * This assumes that the SystemLog is already initialized.
	 */
	private void systemLog(String msg) {
		
		edu.ucla.cens.systemlog.Log.i(SYSTEM_LOG_TAG, msg);
	}
	
	private void uploadLatestLocation() {
		
		final String KEY_LOC_LAT = "latitude";
		final String KEY_LOC_LONG = "longitude";
		final String KEY_LOC_ACC = "accuracy";
		final String KEY_LOC_PROVIDER = "provider";
		final String KEY_LOC_TIME = "time";
		final String TIME_STAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
		
		//Check if any location update has been received
		if(mLastKnownLocTime == 0 || mLastKnownLoc == null) {
			return;
		}
		
		//If 'upload always' is not enabled, check for the 
		//compile time constants and upload only if it is 
		//allowed
		if(!mLocTraceUploadAlways) {
			
			//Check if the user has moved at least the distance specified
			if(mLastKnownLoc.distanceTo(mLastLocTrace) < 
				LocTrigConfig.LOC_TRACE_MIN_DISTANCE_FOR_UPLOAD) {
			
				//Before discarding the trace, check the time stamp 
				//of the last upload. If it has been longer than the
				//specified time, upload.
				if(mLastKnownLoc.getTime() - mLastLocTrace.getTime() < 
					LocTrigConfig.LOC_TRACE_MAX_GAP_BETWEEN_UPLOADS) {
					
					Log.i(DEBUG_TAG, "LocTrigService: Skipping the location" +
							" trace upload");
					return;
				}
			}
		}
		
		//TODO move loc to JSON conversion to a common place
		//The trigger details upload also has the same logic
		JSONObject jLoc = new JSONObject();
		

		try {
			jLoc.put(KEY_LOC_LAT, mLastKnownLoc.getLatitude());
			jLoc.put(KEY_LOC_LONG, mLastKnownLoc.getLongitude());
			jLoc.put(KEY_LOC_ACC, mLastKnownLoc.getAccuracy());
			jLoc.put(KEY_LOC_PROVIDER, mLastKnownLoc.getProvider());
			
			SimpleDateFormat dateFormat = new SimpleDateFormat(TIME_STAMP_FORMAT);
			jLoc.put(KEY_LOC_TIME, dateFormat.format(
									new Date(mLastKnownLoc.getTime())));
		} catch (JSONException e) {
			
			Log.e(DEBUG_TAG, "LocTrigService: Error while converting " +
					" location to JSON for tracing", e);
			return;
		}
		
		String msg = "Location trace: " + jLoc.toString();
		//Upload the trace using SystemLog
		Log.i(DEBUG_TAG, "LocTrigService: Upload location trace: " + msg);
		systemLog(msg);
		
		//Save this location locally
		mLastLocTrace.set(mLastKnownLoc);
	}
	
	/* Calculate the sleep time and set the GPS sampling alarm */
	private void reScheduleGPS() {
		stopGPS();
		
		long sTime = getUpdatedSleepTime();
		setSamplingAlarm(ACTION_ALRM_GPS_SAMPLE, sTime, 0);
		
		if(LocTrigConfig.useMotionDetection) {
			if(sTime >= SLEEP_TIME_MIN_MOTION_DETECT) {
				startMotionDetection();
			}
		}
		
		//Upload the latest location for location tracing
		uploadLatestLocation();
		
		releaseWakeLock();
	}
	
	private void handleTriggerAlwaysAlarm(int trigId) {
		//Re-confirm that the trigger has not gone off
		//today. Just to prevent any issues due to 
		//async nature of alarms
		LocationTrigger locTrig = new LocationTrigger();
		if(!locTrig.hasTriggeredToday(this, trigId)) {
			locTrig.notifyTrigger(this, trigId);
		}
		
		//Set the alarm for the next day
		setTriggerAlwaysAlarm(trigId, locTrig.getTrigger(this, trigId));
	}
	
	private void handleSampleGPSAlarm() {
		Log.i(DEBUG_TAG, "LocTrigService: Handling GPS sample alarm");
		
		if(!mSamplingStarted) {
			return;
		}
		
		startGPS();
	}
	
	private void handleGPSTimeoutAlarm() {
		Log.i(DEBUG_TAG, "LocTrigService: Handling GPS timeout");
		
		//If insufficient samples are obtained, timeout the GPS
		if(mNSamples < SAMPLES_LIMIT) {
			Log.i(DEBUG_TAG, "LocTrigService: Unable to obtain samples. " +
					"Timing out");
			
			/* If a pass-through checking is scheduled, notify the user
			 * anyway. This is because when the GPS times out when a pass
			 * through check is scheduled, most likely the user has entered
			 * a building. Thus, it would a best to assume that the user is
			 * staying at the location of interest where the pass through 
			 * checking has been scheduled.
			 */
			if(mPassThroughChecking) {
				mPassThroughChecking = false;
				
				Log.i(DEBUG_TAG, "LocTrigService: Unable to verify location" +
						" after passthrough timer, still notifying");
			
				triggerIfRequired(mPassThroughCheckCateg);
			}

			//Assume speed = 0 here. This will help the sleep time
			//to slowly buildup to maximum value if the user continues
			//to remain in a place where it is difficult to get GPS 
			//samples
			Location loc = new Location(LocationManager.GPS_PROVIDER);
			loc.setSpeed(0);
			recordSpeed(loc);
			
			reScheduleGPS();
		}
	}
	
	private void handlePassThroughCheckAlarm(int categId) {
		Log.i(DEBUG_TAG, "LocTrigService: Handling pass through alarm");
		
		mPassThroughChecking = true;
		mPassThroughCheckCateg = categId;
		startGPS();
	}
	
	private void cancelSamplingAlarm(String action) {
		Intent i = new Intent(action);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
							PendingIntent.FLAG_NO_CREATE);
		if(pi != null) {
			AlarmManager alarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
			alarmMan.cancel(pi);
			pi.cancel();
		}
	}
	
	private void setSamplingAlarm(String action, long timeOut, int extra) {
		Log.i(DEBUG_TAG, "LocTrigService: Setting alarm: " + action);
	
		cancelSamplingAlarm(action);
		
		Intent i = new Intent(action);
		i.putExtra(KEY_SAMPLING_ALARM_EXTRA, extra);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
				   			PendingIntent.FLAG_CANCEL_CURRENT);
    	
		AlarmManager alarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMan.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
    					SystemClock.elapsedRealtime() + timeOut, pi);	
	}
	
	private void cancelAllSamplingAlarms() {
		cancelSamplingAlarm(ACTION_ALRM_GPS_SAMPLE);
		cancelSamplingAlarm(ACTION_ALRM_GPS_TIMEOUT);
		cancelSamplingAlarm(ACTION_ALRM_PASS_THROUGH);
	}
	
	/* Populate the cached list of locations */
	private void populateLocList() {
		mLocList.clear();
		
		LocTrigDB db = new LocTrigDB(this);
		db.open();
		Cursor c = db.getAllLocations();
		
		Log.i(DEBUG_TAG, "LocTrigService: populating loc list with "
			  + c.getCount() + " locations");
		
		if(c.moveToFirst()) {
			do {
				int latE6 = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_LAT));
				int longE6 = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_LONG));
				int cId = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_CATEGORY_ID));
				float r = c.getFloat(c.getColumnIndexOrThrow(LocTrigDB.KEY_RADIUS));
				
				Log.i(DEBUG_TAG, "LocTrigService: adding to the list the location: "
						  + latE6 + ", " + longE6
						  + ", category id = " + cId
						  + ", radius = " + r);
				
				mLocList.add(new LocListItem(latE6, longE6, cId, r));
				
			} while(c.moveToNext());
		}
		
		c.close();
		db.close();
	}
	
	/*
	 * Check if a location coordinate correspond to a category.
	 * Return the category id in that case.
	 */
	private int getLocCategory(Location loc) {
		for(LocListItem item : mLocList) {
			
			float[] dist = new float[1];
			Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), 
									 item.gp.getLatitudeE6() / 1E6, 
									 item.gp.getLongitudeE6() / 1E6, 
									 dist);
			
			//Check if the given location (including its accuracy)
			//completely falls inside an existing location (with an
			//error threshold)
			if((dist[0] + loc.getAccuracy()) <= (item.radius + CATEG_ACCURACY_MARGIN)) {
				return item.categoryId;
			}
		}
		
		return CATEG_ID_INVAL;
	}
	
	/*
	 * Can be optimized by caching
	 */
	private long getMinCategoryExpirationTime(int categId) {
		LocationTrigger locTrig = new LocationTrigger();
		
		LocTrigDB db = new LocTrigDB(this);
		db.open();
		String categName = db.getCategoryName(categId);
		db.close();
		
		int minReentry = -1;
		
		LinkedList<Integer> trigs = new LinkedList<Integer>();
		
		DbHelper dbHelper = new DbHelper(this);
		for (Campaign c : dbHelper.getCampaigns()) {
			trigs.addAll(locTrig.getAllActiveTriggerIds(this, c.mUrn));
		}
		
		for(int trig : trigs) {
			LocTrigDesc desc = new LocTrigDesc();
			
			if(!desc.loadString(locTrig.getTrigger(this, trig))) {
				continue;
			}
			
			if(!desc.getLocation().equalsIgnoreCase(categName)) {
				continue;
			}
			
			int cur = desc.getMinReentryInterval();
			if(minReentry == -1) {
				minReentry = cur;
			}
			else if(cur < minReentry) {
				minReentry = cur;
			}
		}
		
		if(minReentry == -1) {
			return -1;
		}
		
		return (minReentry * 60 * 1000);
	}
	
	private boolean checkIfCategoryExpired(int categId) {
		LocTrigDB db = new LocTrigDB(this);
		db.open();
		
		boolean expired = false;
		
		long categTS = db.getCategoryTimeStamp(categId);
		if(categTS == LocTrigDB.TIME_STAMP_INVALID) {
			expired = true;
		}
		else {
			long elapsed = System.currentTimeMillis() - categTS;
			if(elapsed >= getMinCategoryExpirationTime(categId)) {
				expired = true;
			}
		}
		
		db.close();
		
		return expired;
	}
	

	private float getDistanceToClosestCategory() {
	
		float minDist = -1;
		
		for(LocListItem item : mLocList) {
			
			//Check the distance to all locations to watch for.
			//This includes only those locations which have not 
			//expired.
			if(checkIfCategoryExpired(item.categoryId)) {
				
				float[] dist = new float[1];
				Location.distanceBetween(mLastKnownLoc.getLatitude(), 
										 mLastKnownLoc.getLongitude(), 
										 item.gp.getLatitudeE6() / 1E6, 
										 item.gp.getLongitudeE6() / 1E6, 
										 dist);
				
				float d = dist[0] - item.radius;
				if(d > 0) {
					minDist = (minDist == -1) ? d :
							  Math.min(minDist, d);
				}
			}
		}
		
		//If there is no closest location, and if the tracing
		//needs to be done, use a constant factor
		if(minDist == -1 && mLocTraceEnabled) {
			minDist = LocTrigConfig.LOC_TRACE_DISTANCE_FACTOR;
		}
		
		return minDist;
	}
	
	/* Reset the current sleep time */ 
	private void resetSleepTime() {
		mCurrSleepTime = Math.min(MIN_SLEEP_TIME, mCurrSleepTime);
	}
	
	/* Calculate the new sleep time based on the current speed
	 * and the distance to the closest category.
	 */
	private long getUpdatedSleepTime() {
		long sTime = mCurrSleepTime;
		float sleepDist = 0;
		
		float proximityDist = getDistanceToClosestCategory();
		
		Log.i(DEBUG_TAG, "LocTrigService: Proximity dist: " + proximityDist);
		
		//Based on the proximity, calculate the distance (sleepDist) 
		//which should be covered before the next sampling
		if(proximityDist != -1) {
			//sampleDist is a proportional to the proximity
			sleepDist = proximityDist / SLEEP_DIST_FACTOR;
			
			//If the user has covered half the proximity distance
			//reset the sleep time. This will help when the speed
			//cannot be measured and thus the sample time increases
			//even though the user moves close to a location.
			if(proximityDist <= mCurrProxDist / 2 && mCurrSpeed == 0) {
				sTime = Math.min(MIN_SLEEP_TIME, sTime);
			}
			
			mCurrProxDist = proximityDist;
		}

		//Check if the user is idle
		if(mCurrSpeed == 0) {
			//if the phone is idle now and was moving before, 
			//reset the sleep time.
			if(mPrevSpeed != 0) {
				sTime = MIN_SLEEP_TIME;
			}
			else {
				//Double interval when the phone maintains
				//the idle status
				sTime *= 2;
				if(sTime == 0) {
					sTime = MIN_SLEEP_TIME;
				}
			}
		}
		
		//If the speed is non-zero, calculate the sleep time based on it
		else if(sleepDist != 0) {
			float roundSpeed = (float) Math.ceil(mCurrSpeed);
			sTime = (long) ((sleepDist / roundSpeed) * 1000);
		}
		else {
			sTime = MAX_SLEEP_TIME;
		}
		
		//Bound the sleep time
		sTime = Math.min(MAX_SLEEP_TIME, sTime);
		
		Log.i(DEBUG_TAG, "LocTrigService: Sleep time updated to: " + sTime);
		mCurrSleepTime = sTime;
		return sTime;
	}
	
	private void recordSpeed(Location loc) {
		
		//Record speed as a running average
		if(loc.hasSpeed()) {
			mCurrSpeed = (mCurrSpeed + loc.getSpeed()) / 2;
		}
		else if(mCurrSpeed == 0 && mLastKnownLocTimeBackup != 0 &&
				mLastKnownLocBackup.getAccuracy() <= INACCURATE_SAMPLE_THRESHOLD) {
					
			float disp = mLastKnownLoc.distanceTo(mLastKnownLocBackup);
			Log.i(DEBUG_TAG, "LocTrigService: displacement: " + disp);
			
			//Calculate the speed only if the user has moved beyond
			//a threshold value. 
			if(disp > SPEED_CALC_MIN_DISPLACEMENT) {
				Log.i(DEBUG_TAG, "LocTrigService: Calculating speed...");
				
				long dT = mLastKnownLocTime - mLastKnownLocTimeBackup;
				mCurrSpeed = disp / dT; 
			}
		}
		
		//Round off very small values
		if(mCurrSpeed < SPEED_MIN_THRESHOLD) {
			mCurrSpeed = 0;
		}
		
		Log.i(DEBUG_TAG, "LocTrigService: Current speed: " + mCurrSpeed);
	}
	
	private void recordLocation(Location loc) {
		//Backup the previous location, needed for speed
		//calculation.
		if(mNSamples == 0) {
			mLastKnownLocBackup.set(mLastKnownLoc);
			mLastKnownLocTimeBackup = mLastKnownLocTime;
		}
		
		mLastKnownLoc.set(loc);
		mLastKnownLocTime = SystemClock.elapsedRealtime();
	}
	
	/* Save the id of the category last visited */
	private void recordLatestCategory(int categId) {
		mLatestCateg = categId;
		
		if(categId != CATEG_ID_INVAL) {
			LocTrigDB db = new LocTrigDB(this);
			db.open();
			db.setCategoryTimeStamp(categId, System.currentTimeMillis());
			db.close();
		}
	}
	
	private void triggerIfRequired(int categId) {
		LocTrigDB db = new LocTrigDB(this);
		db.open();
		String categName = db.getCategoryName(categId);
		db.close();
		
		LocationTrigger locTrig = new LocationTrigger();
		
		LinkedList<Integer> trigs = new LinkedList<Integer>();
		
		DbHelper dbHelper = new DbHelper(this);
		for (Campaign c : dbHelper.getCampaigns()) {
			trigs.addAll(locTrig.getAllActiveTriggerIds(this, c.mUrn));
		}
		
		for(int trigId : trigs) {			
			LocTrigDesc desc = new LocTrigDesc();
			
			if(!desc.loadString(locTrig.getTrigger(this, trigId))) {
				continue;
			}
			
			if(!desc.getLocation().equalsIgnoreCase(categName)) {
				continue;
			}
			
			if(desc.isRangeEnabled()) {
				
				Log.i(DEBUG_TAG, "LocTrigService: Range enabling. Checking whether" +
						" to trigger");
				
				if(locTrig.hasTriggeredToday(this, trigId)) {
					Log.i(DEBUG_TAG, "LocTrigService: Has triggered today, skipping");
					continue;
				}
			
				Calendar cal = Calendar.getInstance();
				SimpleTime now = new SimpleTime(cal.get(Calendar.HOUR_OF_DAY), 
												cal.get(Calendar.MINUTE));
				
				if(now.isBefore(desc.getStartTime())) {
					continue;
				}
				
				SimpleTime end = desc.getEndTime();
				if(now.isAfter(end)) {
					continue;
				}
				else if(now.equals(end) && cal.get(Calendar.SECOND) > 0){
					continue;
				}
				
				Log.i(DEBUG_TAG, "LocTrigService: Triggering now");
				cancelTriggerAlwaysAlarm(trigId);
				locTrig.notifyTrigger(this, trigId);
			}
			else if(mCategPrevTS == LocTrigDB.TIME_STAMP_INVALID) { 
				Log.i(DEBUG_TAG, "LocTrigService: Invalid categ timestamp." +
						" Triggering...");
				
				locTrig.notifyTrigger(this, trigId);
			}
			else {
				long elapsed = System.currentTimeMillis() - mCategPrevTS;
				long minReentry = desc.getMinReentryInterval() * 60 * 1000;
				
				if(elapsed > minReentry) {
					Log.i(DEBUG_TAG, "LocTrigService: Beyond minimum re-entry. " +
							"Triggering...");
					locTrig.notifyTrigger(this, trigId);
				}
				else {
					Log.i(DEBUG_TAG, "LocTrigService: Minimum re-entry has not expired. " +
							" Not triggering.");
				}
			}
		}	
	}
	
	/* Notify the user if pass through check succeeds */
	private void handlePassThroughCheckIfRequired(int categId) {
		if(mPassThroughChecking) {
			if(categId == mPassThroughCheckCateg) {
				Log.i(DEBUG_TAG, "LocTrigService: User hasnt changed category, " +
						"notifying");
				
				triggerIfRequired(categId);
			}
			
			mPassThroughChecking = false;
		}
	}
	
	/* Cancel the pass through check */
	private void cancelPassThroughCheckingIfRequired() {
		if(mPassThroughChecking) {
			mPassThroughChecking = false;
		}
	}
	
	@Override
	public void onLocationChanged(Location loc) {
		Log.i(DEBUG_TAG, "LocTrigService: new location received: " +
						 loc.getLatitude() + ", " +
						 loc.getLongitude() + " (" + 
						 loc.getProvider() + "), accuracy = " +
						 loc.getAccuracy() + ", speed = " + 
						 loc.getSpeed() + ", Time = " + 
						 new Date(loc.getTime()).toString());
		
		if(!mGPSStarted) {
			Log.i(DEBUG_TAG, "LocTrigService: Discarding stray location " +
					"after disabling locaiton updates");
			return;
		}
		
		//Discard if a stale location is received (could be possible in network
		//location)
		if((loc.getTime() + STALE_LOC_TIME) < System.currentTimeMillis()) {
			Log.i(DEBUG_TAG, "LocTrigService: Discarding stale location");
			return;
		}
		
		/* Check if the last known location belongs to a category. 
		 * This is required when the user defines a category on the current
		 * location. At this time, the surveys must not be 
		 * immediately triggered but only when the user enters this
		 * category the next time. Thus, at this point, check if the last
		 * known location has a category. This means that the last known location
		 * must not be triggered. 
		 * 
		 * There is a corner case where this will not work. Suppose, the user entered
		 * a location and the GPS sampling hasn't been done yet. So, this location is 
		 * not yet recorded. If the user defines a new category on this location and
		 * sets triggers, it will be triggered. This can happen only if the user sets
		 * a trigger with in MAX_SLEEP_TIME after entering a location. 
		 */
		int prevCateg = getLocCategory(mLastKnownLoc);
		if(prevCateg != CATEG_ID_INVAL && mLatestCateg == CATEG_ID_INVAL) {
			Log.i(DEBUG_TAG, "LocTrigService: Assigning category to the prev loc");
			recordLatestCategory(prevCateg);
		}
				
		if(!loc.hasAccuracy()) {
			Log.i(DEBUG_TAG, "LocTrigService: Discarding loc with no accuracy info");
			return;
		}

		recordLocation(loc);
		
		int locCateg = getLocCategory(loc);
		Log.i(DEBUG_TAG, "LocTrigService: Loc category = " + locCateg);
		
		if(locCateg != CATEG_ID_INVAL) {
			handlePassThroughCheckIfRequired(locCateg);
			
			//If entering a new category, trigger if necessary
			//set the pass through check alarm first
			if(locCateg != mLatestCateg) {
				//start triggering only after sufficient number of
				//initial samples have been collected
				if(mNInitialSamples >= SAMPLES_LIMIT) {
					
					//Cache the previous visit time for this category
					//as it is going to be updated now
					LocTrigDB db = new LocTrigDB(this);
					db.open();
					mCategPrevTS = db.getCategoryTimeStamp(locCateg);
					Log.i(DEBUG_TAG, "LocTrigService: Caching category time stamp: " +
							mCategPrevTS);
					
					db.close();
					
					setSamplingAlarm(ACTION_ALRM_PASS_THROUGH, 
									PASS_THROUGH_TIME, locCateg);
				}
			}
			
			recordLatestCategory(locCateg);
			
			mNInitialSamples = SAMPLES_LIMIT;
			mNSamples = SAMPLES_LIMIT;
			mCurrSpeed = 0;
			
			reScheduleGPS();
			return;
		}
		else {
			//Discard very inaccurate samples
			if(loc.getAccuracy() > INACCURATE_SAMPLE_THRESHOLD) {
				/* Refresh the time stamp of the latest category.
				 * If the user enters a building within a category
				 * then that category might expire due to inaccurate
				 * samples. Refreshing the time stamp will prevent
				 * duplicate triggers from happening at that category
				 */
				recordLatestCategory(mLatestCateg);
				
				Log.i(DEBUG_TAG, "LocTrigService: Discarding inaccurate sample");
				return;
			}
			
			//Detect an exit from a category and start aggressive sampling
			if(mLatestCateg != CATEG_ID_INVAL) {
				resetSleepTime();
			}
			
			recordLatestCategory(locCateg);
			
			//Do not turn off updates until sufficient samples with speed info
			//are obtained
			if(LocTrigConfig.useNetworkLocation) {
				if(loc.getProvider().equals(LocationManager.NETWORK_PROVIDER) && 
				   !loc.hasSpeed()) {
					Log.i(DEBUG_TAG, "LocTrigService: Discarding network " +
							"location without speed");
					return;
				}
			}
			
			recordSpeed(loc);
			
			//Collect some initial samples when the service/sampling starts for
			//the first time. This is to establish the current location.
			//This makes sure than the very first trigger set on the current
			//location is not triggered immediately.
			if(mNInitialSamples < SAMPLES_LIMIT) {
				mNInitialSamples++;
				
				Log.i(DEBUG_TAG, "LocTrigService: Collecting initial samples");
				return;
			}
			
			mNSamples++;
			//If the sample count hits the limit, reschedule.
			if(mNSamples == SAMPLES_LIMIT) {
				cancelPassThroughCheckingIfRequired();
				reScheduleGPS();	
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	
	}

	@Override
	public void onProviderEnabled(String provider) {
		updateSamplingStatus();
	}

	@Override
	public void onStatusChanged(String provider, int status, 
				Bundle extras) {
	}

	
	/************************ INNER CLASSES ************************/
    /* Class representing an item in the cached location list */
    private class LocListItem {
		public GeoPoint gp;
		public float radius;
		public int categoryId;

		public LocListItem(int latE6, int longE6, int cId, float radius) {
			this.gp = new GeoPoint(latE6, longE6);
			this.categoryId = cId;
			this.radius = radius;
		}
	}

    /* Receiver for all the alarms */
	public static class AlarmReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			
			acquireRecvrWakeLock(context);
			
			Intent i = new Intent(context, LocTrigService.class);
			i.setAction(ACTION_HANDLE_ALARM);
			i.replaceExtras(intent);
			i.putExtra(KEY_ALARM_ACTION, intent.getAction());
			
			context.startService(i);
		}
	}
}
