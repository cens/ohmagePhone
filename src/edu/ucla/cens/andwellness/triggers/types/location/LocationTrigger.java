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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import edu.ucla.cens.andwellness.R;
import edu.ucla.cens.andwellness.triggers.base.TriggerBase;

/*
 * The concrete instance of the location based triggers class. 
 * 
 * This class inherits TriggerBase and is registered with the
 * framework by adding an entry in TriggerTypeMap. This class
 * implements the abstract functions in TriggerBase through
 * which the framework interacts with location triggers.   
 */
public class LocationTrigger extends TriggerBase {

	private static final String DEBUG_TAG = "LocationTrigger";
	
	//This string must be unique across all trigger types 
	//registered with the framework.
	private static final String TRIGGER_TYPE = "LocationTrigger";
	
	//TODO localize
	private static final String DISP_NAME = "Location Trigger";
	
	private static final String KEY_PLACES = "places";
	private static final String KEY_NAME = "name";
	private static final String KEY_LOCATIONS = "locations";
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	private static final String KEY_RADIUS = "radius";
	
	/* IMPLEMENTATION OF THE ABSTRACT FUNCTIONS */
	
	@Override
	public String getTriggerTypeDisplayName() {
		
		return DISP_NAME;
	}

	@Override
	public String getTriggerType() {
		
		return TRIGGER_TYPE;
	}
	
	@Override
	public boolean hasSettings() {
		
		return true;
	}
	
	@Override
	/*
	 * The summary of a location trigger is the information 
	 * related to its time range. If there is no time range set, 
	 * returns "Always". If there is a time range, it returns 
	 * the range in the form "start - end". 
	 */
	public String getDisplaySummary(Context context, String trigDesc) {
		LocTrigDesc desc = new LocTrigDesc();
		
		if(!desc.loadString(trigDesc)) {
			return null;
		}
		
		String ret = "";
		if(!desc.isRangeEnabled()) {
			ret = "Always";
		}
		else {
			ret = desc.getStartTime().toString()
					+ " - "
					+ desc.getEndTime().toString();
		}
		
		return ret;
	}

	@Override
	/*
	 * The display title of a location trigger is the name 
	 * of the location. 
	 */
	public String getDisplayTitle(Context context, String trigDesc) {
		LocTrigDesc desc = new LocTrigDesc();
		
		if(!desc.loadString(trigDesc)) {
			return null;
		}
		
		return desc.getLocation();
	}

	@Override
	public int getIcon() {
		return R.drawable.map;
	}
	
	@Override
	public void launchTriggerCreateActivity(Context context, final String campaignUrn, boolean adminMode) {

		//Register a listener with the editor
		//activity to listen for the 'done' event
		LocTrigEditActivity.setOnExitListener(
					new LocTrigEditActivity.ExitListener() {
			
			@Override
			public void onDone(Context context, int trigId, 
							   String trigDesc) {
				
				//When done, save the trigger to the db
				addNewTrigger(context, campaignUrn, trigDesc);
			}
		});
		
		
		Intent i = new Intent(context, LocTrigEditActivity.class);
		i.putExtra(LocTrigEditActivity.KEY_ADMIN_MODE, adminMode);
		context.startActivity(i);
	}

	@Override
	public void launchSettingsEditActivity(Context context, boolean adminMode) {
		Intent i = new Intent(context, LocTrigSettingsActivity.class);
		i.putExtra(LocTrigSettingsActivity.KEY_ADMIN_MODE, adminMode);
		context.startActivity(i);
	}
	
	@Override
	public void resetSettings(Context context) {
		LocTrigDB.deleteDatabase(context);
		LocTrigTracingSettActivity.resetAllSettings(context);
	}

	@Override
	public void launchTriggerEditActivity(Context context, int trigId, 
										  String trigDesc, boolean adminMode) {
		
		//Register a listener with the editor
		//activity to listen for the 'done' event
		LocTrigEditActivity.setOnExitListener(
				new LocTrigEditActivity.ExitListener() {
		
			@Override
			public void onDone(Context context, int trigId, 
							   String trigDesc) {
				
				//Update the trigger in db when done
				updateTrigger(context, trigId, trigDesc);
			}
		});
	
	
		//Pass the trigger details to the editor
		Intent i = new Intent(context, LocTrigEditActivity.class);
		i.putExtra(LocTrigEditActivity.KEY_TRIG_ID, trigId);
		i.putExtra(LocTrigEditActivity.KEY_TRIG_DESC, trigDesc);
		i.putExtra(LocTrigEditActivity.KEY_ADMIN_MODE, adminMode);
		context.startActivity(i);	
	}

	@Override
	public void stopTrigger(Context context, int trigId, String trigDesc) {
		Log.i(DEBUG_TAG, "LocationTrigger: removeTrigger(" + trigId +
										", " + trigDesc + ")");
		
		//Tell the service to stop this trigger
		Intent i = new Intent(context, LocTrigService.class);
		i.setAction(LocTrigService.ACTION_REMOVE_TRIGGER);
		i.putExtra(LocTrigService.KEY_TRIG_ID, trigId);
		i.putExtra(LocTrigService.KEY_TRIG_DESC, trigDesc);
		context.startService(i);
	}

	@Override
	public void resetTrigger(Context context, int trigId, String trigDesc) {
		Log.i(DEBUG_TAG, "LocationTrigger: resetTrigger(" + trigId +
				", " + trigDesc + ")");
		
		//Tell the service to restart the trigger
		Intent i = new Intent(context, LocTrigService.class);
		i.setAction(LocTrigService.ACTION_RESET_TRIGGER);
		i.putExtra(LocTrigService.KEY_TRIG_ID, trigId);
		i.putExtra(LocTrigService.KEY_TRIG_DESC, trigDesc);
		context.startService(i);
	}

	@Override
	public void startTrigger(Context context, int trigId, String trigDesc) {
		Log.i(DEBUG_TAG, "LocationTrigger: startTrigger(" + trigId +
				", " + trigDesc + ")");
		
		//Tell the service to start the trigger
		Intent i = new Intent(context, LocTrigService.class);
		i.setAction(LocTrigService.ACTION_START_TRIGGER);
		i.putExtra(LocTrigService.KEY_TRIG_ID, trigId);
		i.putExtra(LocTrigService.KEY_TRIG_DESC, trigDesc);
		context.startService(i);
	}

	/*
	 * Helper function to serialize all the location settings to
	 * a JSON array.
	 */
	private JSONArray getLocations(Context context, int categId) {
	
		JSONArray jLocs = new JSONArray();
		
		LocTrigDB db = new LocTrigDB(context);
		db.open();
		
		Cursor cLocs = db.getLocations(categId);
		if(cLocs.moveToFirst()) {
			do {
				int lat = cLocs.getInt(
						  cLocs.getColumnIndexOrThrow(LocTrigDB.KEY_LAT));
				int lng = cLocs.getInt(
						  cLocs.getColumnIndexOrThrow(LocTrigDB.KEY_LONG));
				float radius = cLocs.getFloat(
							   cLocs.getColumnIndexOrThrow(LocTrigDB.KEY_RADIUS));
				
				JSONObject jLoc = new JSONObject();
				try {
					jLoc.put(KEY_LATITUDE, (double)(lat / 1E6));
					jLoc.put(KEY_LONGITUDE, (double)(lng / 1E6));
					jLoc.put(KEY_RADIUS, radius);
					
					jLocs.put(jLoc);
				} catch (JSONException e) {
					Log.e(DEBUG_TAG, "LocationTrigger: Error adding locations to " +
									 "preference JSON");
				}
				
			} while(cLocs.moveToNext());
		}
		
		cLocs.close();
		db.close();
		
		return jLocs;
	}
	
	@Override
	/*
	 * Returns a JSON object containing all the location 
	 * settings.
	 */
	public JSONObject getPreferences(Context context) {
		LocTrigDB db = new LocTrigDB(context);
		db.open();
		
		JSONArray jPlaces = new JSONArray();
		
		Cursor cCategs = db.getAllCategories();
		if(cCategs.moveToFirst()) {
			do {
				String categName = cCategs.getString(
								   cCategs.getColumnIndexOrThrow(LocTrigDB.KEY_NAME));
				int categId = cCategs.getInt(
						   	  cCategs.getColumnIndexOrThrow(LocTrigDB.KEY_ID));
				
				JSONObject jPlace = new JSONObject();
				try {
					jPlace.put(KEY_NAME, categName);
					jPlace.put(KEY_LOCATIONS, getLocations(context, categId));
					
					jPlaces.put(jPlace);
				} catch (JSONException e) {
					Log.e(DEBUG_TAG, "LocationTrigger: Error adding place to " +
					 				  "preference JSON");
				}
				
			} while(cCategs.moveToNext());
		}
		
		cCategs.close();
		db.close();
		
		JSONObject jPref = new JSONObject();
		
		try {
			jPref.put(KEY_PLACES, jPlaces);
		} catch (JSONException e) {
			Log.e(DEBUG_TAG, "LocationTrigger: Error adding places to " +
			 				 "preference JSON");
		}
		
		return jPref;
	}
}
