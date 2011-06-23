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
 * The maps activity to display and modify the coordinates
 * associated with each place (category).
 * 
 * This class has a complex implementation because of the 
 * limitations in the maps api. The following scenarios are
 * implemented here:
 * 	- Overlays of different types
 *  - Drawing circles around overlays
 *  - Long press on any geo point
 *  - Tap on overlays
 *  - Show and hide additional views on a geo point.
 *  
 *  Since all the above scenarios cannot be address at once
 *  using the built in mechanisms, the implementation below
 *  uses its own overridden overlay mechanism and touch handling.
 */

/*
 * TODO: This Activity has leaking dialogs. Needs to be fixed.
 * Currently, activity rotation is disabled in the manifest
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.FloatMath;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.ItemizedOverlay.OnFocusChangeListener;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.triggers.config.LocTrigConfig;
import edu.ucla.cens.andwellness.triggers.utils.TrigPrefManager;
import edu.ucla.cens.andwellness.triggers.utils.TrigTextInput;

/* The maps activity */
public class LocTrigMapsActivity extends MapActivity 
			 implements LocationListener, 
			 			OnFocusChangeListener, 
			 			LocTrigAddLocBalloon.ActionListener {

	private static final String DEBUG_TAG = "LocationTrigger";
	
	public static final String TOOL_TIP_PREF_NAME =
		LocTrigMapsActivity.class.getName() + "tool_tip_pref";
	public static final String KEY_TOOL_TIP_DO_NT_SHOW =
		LocTrigMapsActivity.class.getName() + "tool_tip_do_not_show";
	
	//The delay before showing the tool tip
	private static long TOOL_TIP_DELAY = 500; //ms
	
	//Location id for the 'my location' overlay
	private static final int CURR_LOC_ID = -1;
	
	//Number of retries of address look up in case of failures
	private static final int GEOCODING_RETRIES = 5;
	//Interval between consecutive address lookups
	private static final long GEOCODING_RETRY_INTERVAL = 500; //ms
	
	/* Menu ids */
	private static final int MENU_MY_LOC_ID = Menu.FIRST;
	private static final int MENU_SEARCH_ID = Menu.FIRST + 1;
	private static final int MENU_SATELLITE_ID = Menu.FIRST + 2;
	private static final int MENU_HELP_ID = Menu.FIRST + 3;
	
	//Timeout for 'my location' GPS sampling
	private static final long MY_LOC_SAMPLE_TIMEOUT = 180000; //3 mins
	private static final long MY_LOC_EXPIRY = 60000; //1min
	
	//Alarm action string for GPS timeout
	private static final String ACTION_ALRM_GPS_TIMEOUT = 
				"edu.ucla.cens.loctriggers.activity.MapsActivity.myloc_timeout";
	
	//GPS timeout alarm PI
	private PendingIntent mGpsTimeoutPI = null;
	//Mapview instance
	private MapView mMapView;
	//The list of overlay items. This class also handles
	//the touch actions
	private MapsOverlay mOverlayItems = null;
	//Location manager (for GPS)
	private LocationManager mLocMan;
	//Db instance
	private LocTrigDB mDb;
	//The category id for which this activity is opened
	private int mCategId = 0;
	//Balloon displaying 'add this/my location'
	private LocTrigAddLocBalloon mAddLocBalloon = null;
	//The async task for address search 
	private AsyncTask<String, Void, Address> mSearchTask = null;
	//Alarm manager (for GPS timeout)
	private AlarmManager mAlarmMan = null;
	private Location mLatestLoc = null;

	/*****************************************************************************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(DEBUG_TAG, "Maps: onCreate");
    	
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.trigger_loc_maps);
        
        //Get the category id from the intent
        Bundle extras = getIntent().getExtras();
        if(extras == null) {
        	Log.e(DEBUG_TAG, "Maps: Intent extras is null");
        	
        	finish();
        	return;
        }
        
        mCategId = extras.getInt(LocTrigDB.KEY_ID);
        Log.i(DEBUG_TAG, "Maps: category id = " + mCategId);
        	
        
        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.setBuiltInZoomControls(true);
        
        //Handle done button and exit this activity
        Button bDone = (Button) findViewById(R.id.button_maps_done);
        bDone.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				exitMaps();
			}
		});
        
        mAlarmMan = (AlarmManager) getSystemService(ALARM_SERVICE);
        mLocMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        mDb = new LocTrigDB(this);
        mDb.open();
        
        //Create the balloon to display 'add location' 
        mAddLocBalloon = new LocTrigAddLocBalloon(this, mMapView);
        mAddLocBalloon.setActionListener(this);
        
        //Create the itemized overlay with red marker as the default
        Drawable redMarker = this.getResources().getDrawable(
        					R.drawable.trigger_loc_marker_red);
        mOverlayItems = new MapsOverlay(redMarker);
        mOverlayItems.setOnFocusChangeListener(this);
        
        //Initialize the overlays with an ItemizedOverlay object.
        //The object contains zero items initially
        //This is required to handle events when there are
        //no markers on the map
        mMapView.getOverlays().clear();
        mMapView.getOverlays().add(mOverlayItems);
    	
        /* Add all the markers and 
         * Set the zoom level to display all the markers */
        
    	int minLat = Integer.MAX_VALUE;
    	int minLong = Integer.MAX_VALUE;
    	int maxLat = Integer.MIN_VALUE;
    	int maxLong = Integer.MIN_VALUE;

    	Cursor c = mDb.getLocations(mCategId);
    	if(c.moveToFirst()) {
        	do {
        		int latE6 = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_LAT));
        		int longE6 = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_LONG));
        		int locationId = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_ID));
        		float r = c.getFloat(c.getColumnIndexOrThrow(LocTrigDB.KEY_RADIUS));
        		GeoPoint prevLoc = new GeoPoint(latE6, longE6);
        		
        		MapsOverlayItem prevLocItem = new MapsOverlayItem(prevLoc, locationId, r);
		        
		        mOverlayItems.addOverlay(prevLocItem);
		        
		        minLat  = Math.min(latE6, minLat);
        	    minLong = Math.min(longE6, minLong);
        	    maxLat  = Math.max(latE6, maxLat);
        	    maxLong = Math.max(longE6, maxLong);
        	}while(c.moveToNext());
    	}
    	
    	if(c.getCount() > 0) {
    		//Make all the markers visible
    		mMapView.getController().zoomToSpan(
    				Math.abs(minLat - maxLat), Math.abs(minLong - maxLong));
    		
    		GeoPoint centerPoint = new GeoPoint((minLat + maxLat)/2, (minLong + maxLong)/2);
    		mMapView.getController().setCenter(centerPoint);
    	}
    	else {
    		//No makers, show the last known location
    		Location lastP = mLocMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    		if(lastP != null) {
    			GeoPoint lastGp = new GeoPoint((int) (lastP.getLatitude() * 1E6), 
    									   (int) (lastP.getLongitude() * 1E6));
    			mMapView.getController().setCenter(lastGp);
    			mMapView.getController().setZoom(mMapView.getMaxZoomLevel());
    		}
    	}
    	
    	c.close();
    
    	//Display appropriate title text
        updateTitleText();
        
        Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				showHelpDialog();
			}
		};
		
		if(!shouldSkipToolTip()) {
			//Show the tool tip after a small delay
			new Handler().postDelayed(runnable, TOOL_TIP_DELAY);
		}
		
//		TrigPrefManager.registerPreferenceFile(LocTrigMapsActivity.this, 
//				   								TOOL_TIP_PREF_NAME);
    }
    
    private boolean shouldSkipToolTip() {

		SharedPreferences pref = getSharedPreferences(TOOL_TIP_PREF_NAME, 
											Context.MODE_PRIVATE);
		return pref.getBoolean(KEY_TOOL_TIP_DO_NT_SHOW, false);
    }
    
    private void showHelpDialog() {
    	Dialog dialog = new Dialog(this);

    	dialog.setContentView(R.layout.trigger_loc_maps_tips);
    	dialog.setTitle("Defining locations");
    	dialog.setOwnerActivity(this);
    	dialog.show();
    	
    	WebView webView = (WebView) dialog.findViewById(R.id.web_view);
    	webView.loadUrl("file:///android_asset/trigger_loc_maps_help.html");
    	
    	CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.check_do_not_show);
    	checkBox.setChecked(shouldSkipToolTip());
    	checkBox.setOnCheckedChangeListener(
    			new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, 
											boolean isChecked) {
				
				SharedPreferences pref = LocTrigMapsActivity.this
											.getSharedPreferences(
												TOOL_TIP_PREF_NAME, 
												Context.MODE_PRIVATE);

				SharedPreferences.Editor editor = pref.edit();
				editor.putBoolean(KEY_TOOL_TIP_DO_NT_SHOW, isChecked);
				editor.commit();
			}
		});
    	
    	Button button = (Button) dialog.findViewById(R.id.button_close);
    	button.setTag(dialog);
    	button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Object tag = v.getTag();
				if(tag != null && tag instanceof Dialog) {
					((Dialog) tag).dismiss();
				}
			}
		});
    }
    
   
    @Override
    public void onDestroy() {
    	Log.i(DEBUG_TAG, "Maps: onDestroy");
    	
    	if(mSearchTask != null) {
    		mSearchTask.cancel(true);
    		mSearchTask = null;
    	}
    	
    	stopGPS();
    	
    	mOverlayItems.setFocus(null);
    	mOverlayItems = null;
    	
    	mDb.close();
    	mDb = null;
    	
    	System.gc();
    	super.onDestroy();
    }
    
    @Override
    public void onStop() {
    	Log.i(DEBUG_TAG, "Maps: onStop");
    	
    	stopGPS();
    	super.onStop();
    }
    
    /* Exit the maps activity */
    private void exitMaps() {
    	finish();
    }    

    /* Notify the service when the location list changes */
    private void notifyService() {
    	Intent i = new Intent(this, LocTrigService.class);
    	i.setAction(LocTrigService.ACTION_UPDATE_LOCATIONS);
    	startService(i);
    }
    
    /* Handle the address search result. Display 
     * the 'add location' balloon
     */
    private void handleSearchResult(Address adr) {
    	mSearchTask = null;
    	
    	GeoPoint gp = new GeoPoint((int) (adr.getLatitude() * 1E6), 
    							   (int) (adr.getLongitude() * 1E6));
    	
    	String addrText = "";
    	
        int addrLines = adr.getMaxAddressLineIndex();
    	for (int i=0; i<Math.min(2, addrLines); i++) {
    		addrText += adr.getAddressLine(i) + "\n";
    	}
 
    	mMapView.getController().setZoom(mMapView.getMaxZoomLevel());
    	mAddLocBalloon.show(gp, getString(R.string.add_this_loc), addrText);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_MY_LOC_ID, 0, R.string.menu_my_loc)
        	.setIcon(android.R.drawable.ic_menu_mylocation);
        menu.add(0, MENU_SEARCH_ID, 1, R.string.menu_search)
        	.setIcon(android.R.drawable.ic_menu_search);
        menu.add(0, MENU_HELP_ID, 4, R.string.menu_help)
        	.setIcon(android.R.drawable.ic_menu_help);
        
        return ret;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	boolean ret = super.onPrepareOptionsMenu(menu);
    	
    	menu.removeItem(MENU_SATELLITE_ID);
    	
    	String txt = "Satellite mode";
        if(mMapView.isSatellite()) {
        	txt = "Map mode";
        }
         
        menu.add(0, MENU_SATELLITE_ID, 3, txt)
     		.setIcon(android.R.drawable.ic_menu_mapmode);
         
    	return ret;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	
    	case MENU_MY_LOC_ID: //Show my location
    		  
    		if(mOverlayItems.getCurrentLocOverlay() != null) {
    			mOverlayItems.removeOverlay(CURR_LOC_ID);
    			mMapView.invalidate();
    		}
    		
            List<String> provs = mLocMan.getProviders(true);
            if(provs == null || provs.size() == 0) {
            	Toast.makeText(this, "Location provideres " +
            			"are disabled in settings!", Toast.LENGTH_SHORT)
            		 .show();
            	return true;
            }
            
    		Toast.makeText(this, 
    				R.string.determining_loc, Toast.LENGTH_SHORT).show();
    		
    		//Start GPS
    		getGPSSamples();
    		
    		return true;
    		
    	case MENU_SEARCH_ID: //Search an address
    		//get the address
    		TrigTextInput ti = new TrigTextInput(this);
    		ti.setTitle(getString(R.string.search_addr_title));
			ti.setPositiveButtonText(getString(R.string.menu_search));
			ti.setNegativeButtonText(getString(R.string.cancel));
			ti.setAllowEmptyText(false);
			ti.setOnClickListener(new TrigTextInput.onClickListener() {
				@Override
				public void onClick(TrigTextInput ti, int which) {
					if(which == TrigTextInput.BUTTON_POSITIVE) {
						//Start the search task
	    				mSearchTask = new SearchAddressTask()
	    								.execute(ti.getText());
					}
				}
			});
			ti.showDialog()
			  .setOwnerActivity(this);
   
    		return true;
    		
    	case MENU_SATELLITE_ID:
    		mMapView.setSatellite(!mMapView.isSatellite());
    		mMapView.invalidate();
    		return true;
    		
    	case MENU_HELP_ID: //Show help
    		showHelpDialog();
    		return true;
    	}
    	
        return super.onOptionsItemSelected(item);
    }
    
    /*
     * Set dynamic help on the title
     */
    private void updateTitleText() {
    	MapsOverlayItem item = mOverlayItems.getFocus();
    	String categName = mDb.getCategoryName(mCategId);
    	
    	//Default title help text
    	String title = getString(R.string.maps_tile_default, categName);
    	
    	if(mOverlayItems.size() == 0) {
    		//No overlay items (including blue circle) present
    		//retain the default title
		}
    	else if(item != null && item.locationId != CURR_LOC_ID) {
    		//a red marker is in focus
			title = getString(R.string.maps_title_focused);
		}
    	else if(mOverlayItems.getCurrentLocOverlay() != null) {
    		//no focus and blue circle is present
    		title = getString(R.string.maps_title_myloc, categName);
    	}
		else if(item == null) {
			//no focus and there are markers present
			title = getString(R.string.maps_title_nofocus);
		}
		
    	 TextView header = (TextView) findViewById(R.id.text_maps_header);
         header.setText(title);
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    //Suppress warning related to unparameterized ItemizedOverlay
	@SuppressWarnings("unchecked")
	@Override
	public void onFocusChanged(ItemizedOverlay overlay, 
							   OverlayItem focused) {
		//Update dynamic title text when the overlay focus changes
		updateTitleText();
	}
    
    /* Stop GPS */
    private void stopGPS() {
    	Log.i(DEBUG_TAG, "Maps: stopGPS");
    	
    	mLocMan.removeUpdates(this);
    	
    	//Remove the GPS timeout timer
    	if(mGpsTimeoutPI != null) {
    		mAlarmMan.cancel(mGpsTimeoutPI);
    		mGpsTimeoutPI.cancel();
    		mGpsTimeoutPI = null;
    	}
    }

    /* Get GPS samples. Also, set GPS timeout alarm */
    private void getGPSSamples() {
    	Log.i(DEBUG_TAG, "Maps: getGPSSamples");
    	
    	BroadcastReceiver bRecr = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals(ACTION_ALRM_GPS_TIMEOUT)) {
					stopGPS();
				}
			}
		};
			
		IntentFilter intFilter = new IntentFilter(ACTION_ALRM_GPS_TIMEOUT);
		registerReceiver(bRecr, intFilter);
		
    	if(mGpsTimeoutPI != null) {
    		mAlarmMan.cancel(mGpsTimeoutPI);
    		mGpsTimeoutPI.cancel();
    	}
    	
    	mLatestLoc = null;
    	
    	mLocMan.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 0, 0, this);
    	
    	//Use network location as well
    	mLocMan.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, this);
    	
    	Intent intent = new Intent(ACTION_ALRM_GPS_TIMEOUT);
    	mGpsTimeoutPI = PendingIntent.getBroadcast(this, 0, intent, 
    					   PendingIntent.FLAG_CANCEL_CURRENT);
    	
    	
    	
    	mAlarmMan.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
    				SystemClock.elapsedRealtime() + MY_LOC_SAMPLE_TIMEOUT, 
    				mGpsTimeoutPI);

    }
    
    /* Handle long press on maps. Display the 'add location' balloon */
	private void handleLongPress(Point p) {
		Log.i(DEBUG_TAG, "Maps: Handling long press");
	
		GeoPoint gp = mMapView.getProjection().fromPixels(p.x, p.y);
		mAddLocBalloon.show(gp, getString(R.string.add_this_loc), "");
	}
	
	/* Handle tap on maps. If tapped on 'my location' overlay, 
	 * display 'add my location' balloon
	 */
	private void handleMarkerTap(int locId) {
		Log.i(DEBUG_TAG, "Maps: Hanlding overlay tap");
		
		if(locId == CURR_LOC_ID) {
			//Stop the GPS, no need of any more samples
			stopGPS();

			GeoPoint gp = mOverlayItems.getCurrentLocOverlay().getPoint();
			mOverlayItems.removeOverlay(CURR_LOC_ID);
			mAddLocBalloon.show(gp, getString(R.string.add_my_loc), "");
			//Update help text
			updateTitleText();
		}
	}
	
	/* Handle long press on a marker. Display the 'delete' message */
	private void handleMarkerLongPress(int locId) {
		Log.i(DEBUG_TAG, "Maps: Hanlding overlay long press");
		
		if(locId != CURR_LOC_ID) {
			new DeleteLocDialog(locId).show(this);
		}
	}
	
	/* GPS location changed callback. Display the blue marker */
	public void onLocationChanged(Location loc) {
		Log.i(DEBUG_TAG, "Maps: new location received: " +
				 loc.getLatitude() + ", " +
				 loc.getLongitude() + " (" + 
				 loc.getProvider() + "), accuracy = " +
				 loc.getAccuracy() + ", speed = " + 
				 loc.getSpeed());
		
		//Check to see if the activity has exited. This callback might get
		//invoked even after that. A boolean flag can be used here, but this
		//variable is anyway set to null on exit. So, its better to make use of it
		if(mDb == null) {
			return;
		}
		
		//Use this loc only if it more accurate than the last one. 
		//If the most accurate one is more than MY_LOC_EXPIRY old,
		//discard it.
		if(mLatestLoc != null) {
			
			if(loc.getAccuracy() > mLatestLoc.getAccuracy()) {
				long dTime = loc.getTime() - mLatestLoc.getTime();
				
				if(dTime < MY_LOC_EXPIRY) {
					return;
				}
			}
		}
		
		mLatestLoc = new Location(loc);
		
		GeoPoint currLoc = new GeoPoint((int)(loc.getLatitude() * 1E6), 
						  (int) (loc.getLongitude() * 1E6));
        
		boolean currOverlayPresent = (mOverlayItems.getCurrentLocOverlay() == null) ? false :
																					 true;
		
		MapsOverlayItem currLocItem = new MapsOverlayItem(currLoc, CURR_LOC_ID, 
													loc.getAccuracy());
		
        mOverlayItems.setCurrentLocOverlay(currLocItem);
        mMapView.invalidate();
        
        //Animate to my location only the first time 
        if(!currOverlayPresent) {
        	mAddLocBalloon.hide();
        	mMapView.getController().animateTo(currLoc);
        	mMapView.getController().setZoom(mMapView.getMaxZoomLevel());
        	mOverlayItems.setFocus(null);
        	updateTitleText();
        }
	}

	@Override
	public void onProviderDisabled(String prov) {

	}

	@Override
	public void onProviderEnabled(String arg0) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if(provider.equals(LocationManager.GPS_PROVIDER)) {
			switch(status) {
			
			case LocationProvider.OUT_OF_SERVICE:
				Toast.makeText(this, R.string.gps_out, 
						Toast.LENGTH_SHORT).show();
				break;
				
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				break;
			}
		}
	}
	
	/* Display a message */
	private void displayMessage(String cName, int resId) {
		AlertDialog dialog = new AlertDialog.Builder(this)
		.setTitle(R.string.loc_overlap_title)
		.setMessage(getString(resId, cName))
		.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.create();
		dialog.show();
		dialog.setOwnerActivity(this);
	}
	
	/* Check if a location overlaps a location is another category 
	 * Returns the name of the overlapping category. Returns null
	 * otherwise 
	 */
	private String chekLocOverlap(int categId, GeoPoint gp, float radius) {
	
		String cName = null;
		Cursor c = mDb.getAllLocations();
		if(c.moveToFirst()) {
			do {
				int cId = c.getInt(c.getColumnIndexOrThrow(
						LocTrigDB.KEY_CATEGORY_ID));
				
				if(cId != categId) {
					
					int lat = c.getInt(c.getColumnIndexOrThrow(
									LocTrigDB.KEY_LAT));
					int lng = c.getInt(c.getColumnIndexOrThrow(
									LocTrigDB.KEY_LONG));
					
					float locr = c.getFloat(c.getColumnIndexOrThrow(
									LocTrigDB.KEY_RADIUS));
					
					float[] dist = new float[1];
					Location.distanceBetween(gp.getLatitudeE6() / 1E6, 
											 gp.getLongitudeE6() / 1E6, 
											 lat / 1E6, 
											 lng / 1E6, 
											 dist);
					
					//Check overlap
					if(dist[0] < locr + radius + LocTrigConfig.MIN_LOC_GAP) {
						cName = mDb.getCategoryName(cId);
						break;
					}
				}
				
			} while(c.moveToNext());
		}
		
		c.close();
		//return the name of overlapping category
		return cName;
	}
	

	@Override
	/* Callback from add location balloon */ 
	public void onAddLocationClick(GeoPoint gp) {
		mAddLocBalloon.hide();
		
		//Add this location with default radius. But before that,
		//check if it is too close to any other category
		
		String cName = chekLocOverlap(mCategId, gp, 
									  LocTrigConfig.LOC_RADIUS_DEFAULT);

		if(cName != null) {
			displayMessage(cName, R.string.loc_overlap_msg);
			return;
		}
		
		int locId = mDb.addLocation(mCategId, gp, 
								LocTrigConfig.LOC_RADIUS_DEFAULT);
		
		MapsOverlayItem newLocItem = new MapsOverlayItem(gp, locId, 
								LocTrigConfig.LOC_RADIUS_DEFAULT);
		
		mOverlayItems.addOverlay(newLocItem);
		mOverlayItems.setFocus(newLocItem);
		mMapView.invalidate();
		
		notifyService();
	}
	
	private static Drawable boundBottomLeft(Drawable marker) {
		marker.setBounds(0, -marker.getIntrinsicHeight(), 
						 marker.getIntrinsicWidth(), 0);
		return marker;
	}
	
	/******************************* INNER CLASSES ********************************/
	
	/* The class to represent an overlay item. Stores the 
	 * location details.
	 */
	private class MapsOverlayItem extends OverlayItem {

		public int locationId = 0;
		public GeoPoint point;
		public float radius = 0;
		
		public MapsOverlayItem(GeoPoint point, int locId, float radius) {
			super(point, "", "");
			
			this.point = point;
			this.locationId = locId;
			this.radius = radius;
		}
	}
	
	/* The class representing all the overlay items 
	 * Implements functions to handle touch and gestures
	 */
	private class MapsOverlay extends ItemizedOverlay<MapsOverlayItem> {

		//Drag gesture threshold
		private static final int DRAG_THREHOLD = 16; //pixels
		//Long press action delay
		private static final int LONG_PRESS_DELAY = 500; //ms
		//Alpha value for light circles
		private static final int ALPHA_LIGHT = 20;
		//Alpha value for dark circles
		private static final int ALPHA_DARK = 50;
		
		//Overlay list
		private ArrayList<MapsOverlayItem> mOverlays = 
			new ArrayList<MapsOverlayItem>();
		
		//Flag to check if the user if touching the screen
		private boolean mTouching = false;
		//Flag to check if a drag action is being performed
		private boolean mDragging = false;
		//The number of runnables posted while a touch down even is 
		//received. Only the last runnable needs to handled.
		private int mPendingRunnables = 0;
		//The point which was touched
		private Point mTouchPoint = new Point();
		//The point where a drag operation is being performed
		private Point mDragPoint = new Point();
		//Flag to check if the circle around a marker is being resized
		private boolean mCircleResizing = false;
		//The radius if a circle being resized 
		private float mDragItemRadius = LocTrigConfig.LOC_RADIUS_DEFAULT;
		
		//Index of 'my location' (blue marker) overlay in the list
		private int mMyLocOverlayIndex = -1;
		//The marker drawables
		private Drawable mBluMarker = getResources()
									.getDrawable(R.drawable.trigger_loc_marker_blue);
		private Drawable mRedMarker = getResources()
									.getDrawable(R.drawable.trigger_loc_marker_red);
		
		//Thread handler (for posting runnables)
		private Handler mHandler = new Handler();
		
		
		public MapsOverlay(Drawable defaultMarker) {
			super(boundBottomLeft(defaultMarker));
			
			populate();
		}
		
		@Override
		protected MapsOverlayItem createItem(int i) {
			return mOverlays.get(i);
		}

		@Override
		public int size() {
			return mOverlays.size();
		}
	
		
		/* Handle touch event to detect long press, tap and drag */
		public boolean onTouchEvent(MotionEvent ev, MapView view) {
			//Log.i(DEBUG_TAG, "Maps: onTouchEvent: " + ev.getAction());
			
			Point currP = new Point((int) ev.getX(), (int) ev.getY());
			
			/* State machine for handling touch events. 
			   Long press is handled by posting a delayed
			   runnable to this thread. If the action is still
			   'down' when the runnable is run, it is a long press.
			   The variable 'pendingRunnables' is used to ignore 
			   unwanted runs due to previous 'down' events */
			
			//Runnable for detecting long press
		    Runnable runLongpress = new Runnable() {
		    	
				public void run() {
					//If the user hasn't lifted the finger, invoke the
					//long press handler
					if(mTouching && mPendingRunnables == 1) {
						
						//set 'touching' to false to avoid a tap 
						//event
						mTouching = false;
						
						int hitIndex = hitTestMarker(mTouchPoint);
						if(hitIndex != -1) {
							//Long pressed a red marker
							handleMarkerLongPress(mOverlays.get(hitIndex).locationId);
						}
						else if(hitTestOverlayCircle(mTouchPoint) != null) {
							//Long pressed a red circle
							if(!mDragging && hitTestOverlayCircle(mTouchPoint) != null) {
								mDragging = true;
								handleCircleDragStart(mTouchPoint);
							}
						}
						else {
							//Long pressed elsewhere
							handleLongPress(mTouchPoint);
						}
					}
					mPendingRunnables--; //Handle only the last runnable
				} 
			};
			
			//Check if the drag ended
			if(ev.getAction() != MotionEvent.ACTION_MOVE && mDragging) {
				mDragging = false;
				handleCircleDragEnd(currP);
				return true;
			}
			
			//Check for other events
			switch(ev.getAction()) {
			
			case MotionEvent.ACTION_DOWN: //Press down

				//Set the flag and post the runnable for long press
				mTouching = true;
				mTouchPoint.set((int) ev.getX(), (int) ev.getY());
				
				//wait for long press 
				mPendingRunnables++;
				mHandler.postDelayed(runLongpress, LONG_PRESS_DELAY);
				break;
				
			case MotionEvent.ACTION_UP: //Lift finger
					
				if(mTouching) {
					mTouching = false;
					
					//remove the balloon
					if(mAddLocBalloon != null) {    
						mAddLocBalloon.hide();
					}
				
					//Check if it is a 'tap' action on the overlays
					int hitIndex = hitTestMarker(mTouchPoint);
					if(hitIndex != -1) {
						handleMarkerTap(mOverlays.get(hitIndex).locationId);	
					}
					//Prevent marker from going out of focus when tapping
					//on the circle
					else if(hitTestOverlayCircle(mTouchPoint) != null) {
						return true;
					}
				}
				break;
				
			case MotionEvent.ACTION_MOVE: //Drag
				
				if(mDragging) {
					//The circle is being dragged
					handleCircleDrag(currP);
					return true;
				}
				
				//Clear the 'touching' flag only after dragging beyond
				//a threshold
				if(mTouching) {	
					if(euclidDist(currP, mTouchPoint) > DRAG_THREHOLD) {
						mTouching = false;
					}
				}
				break;
				
			default:
				mTouching = false;
				break;
			}
			
			return false;
		}
		
		/* Check if a point hits a red circle.
		 * Returns the overlay item in that case. 
		 * Returns null otherwise.
		 */
		private MapsOverlayItem hitTestOverlayCircle(Point p) {
			MapsOverlayItem item = mOverlayItems.getFocus();
			
			if(item == null || item.locationId == CURR_LOC_ID) {
				//No red circle is visible
				return null;
			}
			
			Point markerP = mMapView.getProjection().toPixels(
									item.getPoint(), null);
			
			float rInPixels = mMapView.getProjection().metersToEquatorPixels(item.radius);
			if(euclidDist(p, markerP) > rInPixels) {
				return null;
			}
			
			return item;
		}
		
		/* Test if a point belongs to any of the markers.
		 * If the test if positive, returns the index of
		 * the marker in the list. Otherwise, returns -1.
		 */
		private int hitTestMarker(Point p) {
			//Check all the markers for a hit
			for(int i=0; i<mOverlays.size(); i++) {
				MapsOverlayItem item = mOverlays.get(i);
				GeoPoint ovGp = item.getPoint();
				Point ovP = mMapView.getProjection().toPixels(ovGp, null);
				
				RectF markerRect = new RectF();
				
				int w; 
				int h;
				
				if(i == mMyLocOverlayIndex) {
					w = mBluMarker.getIntrinsicWidth();
					h = mBluMarker.getIntrinsicHeight();
					markerRect.set(-w/2 - 5, -h/2 - 5, w/2 + 5, h/2 + 5);
				}
				else {
					w = mRedMarker.getIntrinsicWidth();
					h = mRedMarker.getIntrinsicHeight();
					markerRect.set(0, -h, w, 0);
				}

				markerRect.offset(ovP.x, ovP.y);
				
				if(markerRect.contains(p.x, p.y)) {
					return i;
				}
			}
			
			return -1;
		}

		/* Calculate the euclidian distance between two points */
		private float euclidDist(Point a, Point b) {
			int dx = a.x - b.x;
			int dy = a.y - b.y;
			
			return FloatMath.sqrt(dx * dx + dy * dy);
		}
		
		
		/* Draw a circle */
		private void drawCircle(Canvas c, int color, GeoPoint center, 
								float radius, int alpha, boolean border) {
			
			Point scCoord = mMapView.getProjection().toPixels(center, null);
			float r = mMapView.getProjection().metersToEquatorPixels(radius);
			 
			Paint p = new Paint();
	        p.setStyle(Style.FILL);
			p.setColor(color);
			p.setAlpha(alpha);
			p.setAntiAlias(true);
			c.drawCircle(scCoord.x, scCoord.y, r, p);
			
			if(border) { //Draw border
				p.setStyle(Style.STROKE);
				p.setColor(color);
				p.setAlpha(100);
				p.setStrokeWidth(1.2F);
				p.setAntiAlias(true);
				c.drawCircle(scCoord.x, scCoord.y,r, p);
			}
		}
		
		/* The overridden draw method of the overlay.
		 * This method is overridden to draw the circle when the
		 * overlay is in focus
		 */
		public void draw(Canvas canvas, MapView mMapView, boolean shadow) {
		
			MapsOverlayItem item = getFocus();
			//Draw circle if a red marker is focused
			if(item != null && item.locationId != CURR_LOC_ID) {
				//Draw dark circle while resizing
				int alpha = mCircleResizing ? ALPHA_DARK : ALPHA_LIGHT;
				boolean border = mCircleResizing ? true : false;
				drawCircle(canvas, Color.RED, item.getPoint(), 
								item.radius, alpha, border);
			}
			
			//Draw blue circle (accuracy) if my location dot is present
			if(mMyLocOverlayIndex != -1) {
				MapsOverlayItem currItem = getItem(mMyLocOverlayIndex);
				if(currItem.radius > 0) {
					drawCircle(canvas, Color.BLUE, currItem.getPoint(), 
								currItem.radius, ALPHA_LIGHT, true);
				}
			}
			
			super.draw(canvas, mMapView, shadow);
		}
		
		/* The focused red circle is about to get resized */
		private void handleCircleDragStart(Point p) {
			Log.i(DEBUG_TAG, "Maps: Drag Start");
			
			MapsOverlayItem item = getFocus();
			if(item == null) {
				return;
			}
			
			//Backup the old radius (needed if the resize fails)
			mDragItemRadius = item.radius;
			
			mCircleResizing = true;
			mDragPoint.set(p.x, p.y);
			mMapView.invalidate();
		}
		
		/* The focused red circle is being resized */ 
		private void handleCircleDrag(Point p) {
			//Log.i(DEBUG_TAG, "Maps: Dragging");
			
			if(!mCircleResizing) {
				return;
			}
			
			MapsOverlayItem item = getFocus();
			if(item == null) {
				return;
			}
			
			if(euclidDist(mDragPoint, p) < DRAG_THREHOLD) {
				return;
			}
			
			//If the resize radius falls within allowed range, update
			//the radius of the overlay
			GeoPoint gp = mMapView.getProjection().fromPixels(p.x, p.y);
			float dist[] = new float[1];
			Location.distanceBetween(item.point.getLatitudeE6() / 1E6, 
					item.point.getLongitudeE6() / 1E6, 
					gp.getLatitudeE6() / 1E6, 
					gp.getLongitudeE6() / 1E6, dist);
			
			if(dist[0] <= LocTrigConfig.LOC_RADIUS_MAX && 
			   dist[0] >= LocTrigConfig.LOC_RADIUS_MIN) {
				item.radius = dist[0];
			}

			mMapView.invalidate();
		}
		
		/* Handle circle resize end */
		private void handleCircleDragEnd(Point p) {
			Log.i(DEBUG_TAG, "Maps: Drag End");
			
			mCircleResizing = false;
			
			MapsOverlayItem item = getFocus();
			if(item == null) {
				return;
			}
			
			String cName = chekLocOverlap(mCategId, item.point, item.radius);
			if(cName != null) {
				//Overlapping, restore the radius
				item.radius = mDragItemRadius;
				displayMessage(cName, R.string.loc_too_big_msg);
			}
			else {
				//Everything ok, update the radius in the db and notify service
				mDb.updateLocationRadius(item.locationId, item.radius);
				notifyService();
			}
			
			mMapView.invalidate();
		}
		
		/* Add a new overlay item */
		public void addOverlay(MapsOverlayItem overlay) {
			mOverlays.add(overlay);
		    populate();
		}
		
		/* Remove an overlay item */
		public void removeOverlay(int locId) {
			
			if(locId == CURR_LOC_ID) {
				mOverlays.remove(mMyLocOverlayIndex);
				mMyLocOverlayIndex = -1;
			}
			else {
				for(int i=0; i<mOverlays.size(); i++) {
					if(mOverlays.get(i).locationId == locId) {
						mOverlays.remove(i);
						break;
					}
				}
				
				if(mMyLocOverlayIndex > 0) {
					mMyLocOverlayIndex--;
				}
			}
			//Work around for platform bug
			setLastFocusedIndex(-1);
		    populate();
		}
		
		/* Add the 'my location' (blue marker) overlay.
		 * This will add a new item to the overlay list if there
		 * is not 'my location' overlay item in the list. If there is
		 * one, it will be replaced.
		 */
		public void setCurrentLocOverlay(MapsOverlayItem overlay) {
			
			overlay.setMarker(boundCenter(mBluMarker));
			
			if(mMyLocOverlayIndex == -1) {
				if(mOverlays.add(overlay)) {
					mMyLocOverlayIndex = mOverlays.size() - 1;
				}
			}
			else {
				mOverlays.set(mMyLocOverlayIndex, overlay);
			}
			
		    populate();
		}
		
		/*
		 * Return 'my location' overlay item if present
		 */
		public MapsOverlayItem getCurrentLocOverlay() {
			if(mMyLocOverlayIndex != -1) {
				return mOverlays.get(mMyLocOverlayIndex);
			}
			
			return null;
		}
		
	}
	
	/* The class to display the delete message and delete the 
	 * overlay item (location) is required
	 */
	private class DeleteLocDialog 
			implements  DialogInterface.OnClickListener{

		private int mLocId;
		
		public DeleteLocDialog(int locId) {
			this.mLocId = locId;
		}
		
		/* Display the dialog */
		public void show(Context context) {
			AlertDialog dialog = new AlertDialog.Builder(context)
			.setTitle(R.string.exisiting_loc)
			.setMessage(R.string.delete_loc)
			.setPositiveButton(R.string.yes, this)
			.setNegativeButton(R.string.no, this)
			.create();
			dialog.show();
			dialog.setOwnerActivity(LocTrigMapsActivity.this);
		}
		
		/* Delete the location */
		private void deleteLocation() {
			mDb.removeLocation(mLocId);
     	    mOverlayItems.removeOverlay(mLocId);
     	    mOverlayItems.setFocus(null);
     	    mMapView.invalidate();
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if(which == AlertDialog.BUTTON_POSITIVE) {
				deleteLocation();
				notifyService();
			}
			
			dialog.dismiss();
		}
	}
	
	/* The search address task class. Performs address search in the bg thread */
	private class SearchAddressTask extends AsyncTask<String, Void, Address> {
		
		private ProgressDialog mBusyPrg = null;
		
		@Override
		protected void onPreExecute() {
			//Start progress bar
			mBusyPrg = ProgressDialog.show(LocTrigMapsActivity.this, "", 
                    				getString(R.string.searching_msg), true);
			mBusyPrg.setOwnerActivity(LocTrigMapsActivity.this);
		}
		
		@Override
		protected Address doInBackground(String... params) {
			//Search the address
			Geocoder geoCoder = new Geocoder(LocTrigMapsActivity.this, 
											 Locale.getDefault());
			
			for(int i = 0; i < GEOCODING_RETRIES; i++) {
			    try {
			    	List<Address> addrs = geoCoder.getFromLocationName(params[0], 5);
		
			        if(addrs.size() > 0) {
			        	return addrs.get(0);
			        }
			    }
			    catch (Exception e) {
			    }
			    
			    try {
					Thread.sleep(GEOCODING_RETRY_INTERVAL);
				} catch (InterruptedException e) {
				}
			}
		    
		    return null;
		}
		
		@Override
		protected void onPostExecute(Address adr) {
			//Address search done, kill progressbar and notify
			
			if(mBusyPrg != null) {
				mBusyPrg.cancel();
				mBusyPrg = null;
			}

			if(adr != null && adr.hasLongitude() && adr.hasLatitude()) {
				handleSearchResult(adr);
			}
			else {
				Toast.makeText(getApplicationContext(), 
							   getString(R.string.search_fail),
							   Toast.LENGTH_SHORT).show();
			}
	    }
	}
}


