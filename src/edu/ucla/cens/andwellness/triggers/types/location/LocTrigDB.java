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


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.maps.GeoPoint;

import edu.ucla.cens.andwellness.R;

/*
 * Database to store the location triggers settings. 
 * Implements two tables:
 * 	- Categories: The table of all places such as Home, Work etc
 * 	- Locations: The table of all markers (coordinates) 
 * 				 within each place 
 */

/*
 * TODO: update the where clauses with "=?" syntax
 */
public class LocTrigDB {
	
	private static final String DEBUG_TAG = "LocationTrigger";
	
	private static final String DATABASE_NAME = "location_triggers"; 
	private static final int DATABASE_VERSION = 1;

	
	/* Categories table */
	private static final String TABLE_CATEGORIES = "categories";
	/* Locations table */
	private static final String TABLE_LOCATIONS = "locations";
	
	//Value of an invalid timestamp
	public static final long TIME_STAMP_INVALID = -1;
	
	/* Keys */
	public static final String KEY_ID = "_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_CATEGORY_ID = "category_id";
	public static final String KEY_BUILT_IN = "built_in";
	public static final String KEY_TIMESTAMP = "time_stamp";
	public static final String KEY_LAT = "latitude";
	public static final String KEY_LONG = "longitude";
	public static final String KEY_RADIUS = "radius";
	
	private Context mContext;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	public LocTrigDB(Context context) {
		this.mContext = context;
	}
	
	//Delete the database file
	public static void deleteDatabase(Context context) {
		context.deleteDatabase(DATABASE_NAME);
	}
	
	/* Open the database */
	public boolean open() {
		Log.i(DEBUG_TAG, "DB: open");
		
		mDbHelper = new DatabaseHelper(mContext);
		
		try {
			mDb = mDbHelper.getWritableDatabase();
		}
		catch (SQLException e) {
			Log.e(DEBUG_TAG, "DB: " + e.toString());
			return false;
		}
		return true;
	}
	
	/* Close the database */
	public void close() {
		if(mDbHelper != null) {
			mDbHelper.close();
		}
	}
	
	/* Add a new category */
	public boolean addCategory(String name) {
		
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, name);
		values.put(KEY_BUILT_IN, 0);
		values.put(KEY_TIMESTAMP, TIME_STAMP_INVALID);
		
		if(mDb.insert(TABLE_CATEGORIES, null, values) == -1) {
			return false;
		}
		
		return true;
	}
	
	/* Rename an existing category */
	public boolean renameCategory(int categId, String newName) {
		
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, newName);
		
		if(mDb.update(TABLE_CATEGORIES, values,
					  "" + KEY_ID + "=" + categId, null) != 1) {
			return false;
		}
		
		return true;
	}
	
	/* Return the list of all categories */
	public Cursor getAllCategories() {
		return mDb.query(TABLE_CATEGORIES, new String[] {KEY_ID, KEY_NAME, KEY_BUILT_IN},
			null, null, null, null, null);
	}
	
	/* Get a single category from category id */
	public Cursor getCategory(int categoryId) {
		return mDb.query(TABLE_CATEGORIES, null, 
					"" + KEY_ID + "=" + categoryId, 
					null, null, null, null);
	}
	
	/* Get a single category from category name */
	public Cursor getCategory(String categName) {
		return mDb.query(TABLE_CATEGORIES, null, 
					KEY_NAME + "=?", new String[] {categName}, 
					null, null, null);
	}
	
	/* Get category name corresponding to a category id */
	public String getCategoryName(int categId) {
		String name = null;
		
		Cursor c = mDb.query(TABLE_CATEGORIES, new String[] {KEY_NAME}, 
							"" + KEY_ID + "=" + categId, 
							null, null, null, null);
		
		if(c.getCount() == 1) {
			c.moveToFirst();
			name = c.getString(c.getColumnIndexOrThrow(KEY_NAME));
		}
		
		c.close();
		return name;
	}
	
	/* Delete a category.
	 * Returns the number of locations associated
	 */
	public int removeCategory(int categoryId) {
		
		mDb.delete(TABLE_CATEGORIES, "" + KEY_ID + " = " + categoryId , null);
		int locs = mDb.delete(TABLE_LOCATIONS, "" + KEY_CATEGORY_ID + " = " + categoryId , null);
		
		return locs;
	}
	
	/*
	 * Get the time stamp of a category. 
	 */
	public long getCategoryTimeStamp(int categId) {
		Cursor c = mDb.query(TABLE_CATEGORIES, new String[] {KEY_TIMESTAMP}, 
				 				KEY_ID + "=?", new String[] {String.valueOf(categId)}, 
				 				null, null, null);
		
		long ret = TIME_STAMP_INVALID;
		if(c.moveToFirst()) {
			ret = c.getLong(c.getColumnIndexOrThrow(LocTrigDB.KEY_TIMESTAMP));
		}
		
		c.close();
		return ret;
	}
	
	/*
	 * Set the time stamp of a category.
	 */
	public void setCategoryTimeStamp(int categId, long timeStamp) {
		ContentValues values = new ContentValues();
		values.put(KEY_TIMESTAMP, timeStamp);
		
		mDb.update(TABLE_CATEGORIES, values, 
					  KEY_ID + "=?", 
					  new String[]{ String.valueOf(categId)});
	}
	
	/*
	 * Remove the time stamps from all teh categories 
	 * in the categories table.
	 */
	public boolean removeAllCategoryTimeStamps() {
		
		ContentValues values = new ContentValues();
		values.put(KEY_TIMESTAMP, TIME_STAMP_INVALID);
		
		mDb.update(TABLE_CATEGORIES, values, null, null);
		return true;
	}
	
	/* Add a new location */
	public int addLocation(int categoryId, GeoPoint loc, float radius) {
		
		ContentValues values = new ContentValues();
		values.put(KEY_LAT, loc.getLatitudeE6());
		values.put(KEY_LONG, loc.getLongitudeE6());
		values.put(KEY_CATEGORY_ID, categoryId);
		values.put(KEY_RADIUS, radius);
		
		return (int) mDb.insert(TABLE_LOCATIONS, null, values);
	}
	
	/* Delete an existing location */
	public boolean removeLocation(int locId) {
	
		if(mDb.delete(TABLE_LOCATIONS, "" + KEY_ID + " = " + locId , null) != 1) {
			return false;
		}
		
		return true;
	}
	
	/* Update an existing location */
	public boolean updateLocationRadius(int locId, float radius) {
	
		ContentValues values = new ContentValues();
		values.put(KEY_RADIUS, radius);
		
		if(mDb.update(TABLE_LOCATIONS, values,
					  "" + KEY_ID + "=" + locId, null) != 1) {
			return false;
		}
		
		return true;
	}
	
	/* Get all locations corresponding to a category id */
	public Cursor getLocations(int categoryId) {		
		return mDb.rawQuery("SELECT "+ KEY_ID + ", " + KEY_LAT + ", " + KEY_LONG + ", " + KEY_RADIUS
				+ " FROM " + TABLE_LOCATIONS 
				+ " WHERE " + KEY_CATEGORY_ID + " = " + categoryId,
				null);
	}
	
	
	/* Get all locations */
	public Cursor getAllLocations() {
		return mDb.query(TABLE_LOCATIONS, new String[] {KEY_ID, KEY_LAT, KEY_LONG, KEY_RADIUS, 
						KEY_CATEGORY_ID}, null, null, null, null, null);
	}

	/************************* INNER CLASSES *************************/
	
	/* Database helper inner class */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		private Context context;
		
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			this.context = context;
		}

		@Override
		public void onCreate(SQLiteDatabase mDb) {
			Log.i("LocationTriggers", "SQLiteOpenHelper: onCreate");
		
			//Category table
			final String QUERY_CREATE_CATEGORY_TB = 
				"create table " + TABLE_CATEGORIES + " ("
				 + KEY_ID + " integer primary key autoincrement, "
				 + KEY_NAME + " text not null, "
				 + KEY_TIMESTAMP + " long, "
				 + KEY_BUILT_IN + " integer)";
			
			//Location table
			final String QUERY_CREATE_LOCATION_TB = 
				"create table " + TABLE_LOCATIONS + " ("
				 + KEY_ID + " integer primary key autoincrement, "
				 + KEY_LAT + " integer, " + KEY_LONG + " integer, "
				 + KEY_RADIUS + " float, "
				 + KEY_NAME + " text, " + KEY_CATEGORY_ID + " integer)";
			
			//Create the tables
			mDb.execSQL(QUERY_CREATE_CATEGORY_TB);
			mDb.execSQL(QUERY_CREATE_LOCATION_TB);
			
			//Add the built-in categories
			String[] builtinCategories = 
				context.getResources().getStringArray(
									R.array.trigger_builtin_places);
			
			for(int i = 0; i < builtinCategories.length; i++) {
				
				ContentValues values = new ContentValues();
				values.put(KEY_NAME, builtinCategories[i]);
				values.put(KEY_BUILT_IN, 1);
				values.put(KEY_TIMESTAMP, TIME_STAMP_INVALID);
				
				mDb.insertOrThrow(TABLE_CATEGORIES, null, values);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, 
				int newVersion) {
		}
	}
	
}
