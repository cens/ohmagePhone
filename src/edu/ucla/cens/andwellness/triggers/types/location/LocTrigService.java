package edu.ucla.cens.andwellness.triggers.types.location;

/* Location triggers service.
 * Performs location detection based on adaptive 
 * location sampling.
 */

import java.util.Calendar;
import java.util.LinkedList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import edu.ucla.cens.andwellness.triggers.utils.SimpleTime;

public class LocTrigService extends Service 
			 implements LocationListener {

	private static final String DEBUG_TAG = "LocationTrigger";
	
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
	//The number of accurate samples to collect during every
	//sampling instance
	private static final int SAMPLES_LIMIT = 10;
	//GPS timeout alarm time value. The GPS is timed out after
	//this if it cannot obtain the above number of accurate 
	//samples.
	private static final long GPS_TIMEOUT = 60000; //1min
	//The threshold value to use when checking if a location
	//belongs to a category
	private static final float CATEG_ACCURACY_MARGIN = 20; //m
	//The maximum value of GPS duty cycle interval
	private static final long MAX_SLEEP_TIME = 300000; //5 mins
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
							   = 2 * INACCURATE_SAMPLE_THRESHOLD;
	
	//Wake lock for GPS sampling
	private PowerManager.WakeLock mWakeLock = null;
	//Wake lock for alarm receiver
	private static PowerManager.WakeLock mRecvrWakeLock = null;
	
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
	//Flag to check if the sampling is started at all
	private boolean mSamplingStarted = false;
	
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
		
		updateSamplingStatus();
		
		releaseRecvrWakeLock();
		
		super.onStart(intent, startId);
	}
	
	@Override
	public void onDestroy() {
		Log.i(DEBUG_TAG, "LocTrigService: onDestroy");

		stopGPS();
		
		mLocList.clear();
		
		releaseWakeLock();
		releaseRecvrWakeLock();
		
		super.onDestroy();
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
		else if(alm.endsWith(ACTION_ALRM_PASS_THROUGH)) {
			handlePassThroughCheckAlarm(extras.getInt(KEY_SAMPLING_ALARM_EXTRA));
		}
	}
	
	private void setKeepAliveAlarm() {
		Intent i = new Intent(ACTION_ALRM_SRV_KEEP_ALIVE);
		
		//set the alarm if not already existing
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 
				   		   PendingIntent.FLAG_NO_CREATE);
		if(pi != null) {
			//Already exists, do nothing
			return;
		}
		
    	pi = PendingIntent.getBroadcast(this, 0, i, 
    					   PendingIntent.FLAG_CANCEL_CURRENT);
    	
    	AlarmManager alarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
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
		LinkedList<Integer> actTrigs = new LocationTrigger()
										.getAllActiveTriggerIds(this);
		
		if(actTrigs.size() > 0 && mLocList.size() > 0) {
			startSampling();
		}
		else {
			stopSampling();
		}
	}

	private void startGPS() {
		//Get a wake lock for this duty cycle
		acquireWakeLock();
		
		mNSamples = 0;
		mPrevSpeed = mCurrSpeed;
		//mCurrSpeed = 0;
		
		LocationManager locMan = (LocationManager) getSystemService(LOCATION_SERVICE);
		locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				 						0, 0, this);
		
		cancelSamplingAlarm(ACTION_ALRM_GPS_SAMPLE);
		//Set GPS timeout
		setSamplingAlarm(ACTION_ALRM_GPS_TIMEOUT, GPS_TIMEOUT, 0);
	}
	
	private void stopGPS() {
		cancelSamplingAlarm(ACTION_ALRM_GPS_TIMEOUT);
		
		LocationManager locMan = (LocationManager) getSystemService(LOCATION_SERVICE);
		locMan.removeUpdates(this);
	}
	
	/* Calculate the sleep time and set the GPS sampling alarm */
	private void reScheduleGPS() {
		stopGPS();
		
		long sTime = getUpdatedSleepTime();
		setSamplingAlarm(ACTION_ALRM_GPS_SAMPLE, sTime, 0);
		
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
		LinkedList<Integer> trigs = locTrig.getAllActiveTriggerIds(this);
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
			sTime = (long) ((sleepDist / mCurrSpeed) * 1000);
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
		
		//Set the speed to next integer val
		mCurrSpeed = (float) Math.ceil(mCurrSpeed);
		
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
		long categTS = db.getCategoryTimeStamp(categId);
		db.close();
		
		LocationTrigger locTrig = new LocationTrigger();
		
		LinkedList<Integer> trigs = locTrig.getAllActiveTriggerIds(this);
		for(int trigId : trigs) {			
			LocTrigDesc desc = new LocTrigDesc();
			
			if(!desc.loadString(locTrig.getTrigger(this, trigId))) {
				continue;
			}
			
			if(!desc.getLocation().equalsIgnoreCase(categName)) {
				continue;
			}
			
			if(desc.isRangeEnabled()) {
				
				if(locTrig.hasTriggeredToday(this, trigId)) {
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
				
				cancelTriggerAlwaysAlarm(trigId);
				locTrig.notifyTrigger(this, trigId);
			}
			else if(categTS != LocTrigDB.TIME_STAMP_INVALID) { 
				locTrig.notifyTrigger(this, trigId);
			}
			else {
				long elapsed = System.currentTimeMillis() - categTS;
				long minReentry = desc.getMinReentryInterval() * 60 * 1000;
				
				if(elapsed > minReentry) {
					locTrig.notifyTrigger(this, trigId);
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
						 loc.getSpeed());
		
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
		
		recordLocation(loc);
		
		if(!loc.hasAccuracy()) {
			Log.i(DEBUG_TAG, "LocTrigService: Discarding loc with no accuracy info");
			return;
		}
		
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
			recordSpeed(loc);
			
			//Collect some initial samples when the service/sampling starts for
			//the first time. This is to establish the current location.
			if(mNInitialSamples < SAMPLES_LIMIT) {
				mNInitialSamples++;
				
				Log.i(DEBUG_TAG, "LocTrigService: Collecting initial samples");
				return;
			}
			
			mNSamples++;
			//If the sample count hits the limit, reschedule.
			if(mNSamples >= SAMPLES_LIMIT) {
				cancelPassThroughCheckingIfRequired();
				reScheduleGPS();	
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		if(provider.equals(LocationManager.GPS_PROVIDER)) {
			stopSampling();
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		if(provider.equals(LocationManager.GPS_PROVIDER)) {
			updateSamplingStatus();
		}
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
