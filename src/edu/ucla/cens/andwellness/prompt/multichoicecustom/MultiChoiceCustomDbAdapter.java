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
package edu.ucla.cens.andwellness.prompt.multichoicecustom;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MultiChoiceCustomDbAdapter {
	
	private static final String TAG = "MultiChoiceCustomDbAdapter";
	
	private static final String DB_NAME = "multichoicecustom.db";
	private static final int DB_VERSION = 1;
	private static final String TABLE_CHOICES = "custom_choices";
	
	public static final String KEY_ID = "_id";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_CAMPAIGN_URN = "campaign_urn";
	public static final String KEY_SURVEY_ID = "survey_id";
	public static final String KEY_PROMPT_ID = "prompt_id";
	public static final String KEY_CHOICE_ID = "choice_id";
	public static final String KEY_CHOICE_VALUE = "choice_value";
	
	private Context mContext;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	public MultiChoiceCustomDbAdapter(Context context) {
		this.mContext = context;
	}
	
	/* Open the database */
	public boolean open() {
		Log.i(TAG, "DB: open");
	
		
		mDbHelper = new DatabaseHelper(mContext);
		
		try {
			mDb = mDbHelper.getWritableDatabase();
		}
		catch (SQLException e) {
			Log.e(TAG, e.toString());
			return false;
		}
		return true;
	}
	
	/* Close the database */
	public void close() {
		Log.i(TAG, "DB: close");
		
		if(mDbHelper != null) {
			mDbHelper.close();
		}
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_CHOICES + " ("
					+ KEY_ID + " INTEGER PRIMARY KEY, "					
					+ KEY_USERNAME + " TEXT, "
					+ KEY_CAMPAIGN_URN + " TEXT, "
					+ KEY_SURVEY_ID + " TEXT, "
					+ KEY_PROMPT_ID + " TEXT, "
					+ KEY_CHOICE_ID + " INTEGER, "
					+ KEY_CHOICE_VALUE + " TEXT "
					+ ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHOICES);
			onCreate(db);
		}
	}
	
	public long addCustomChoice(int choiceId, String choiceValue, String username, String campaignUrn, String surveyId, String promptId) {
		
		if (mDb == null) {
			return -1;
		}
		
		ContentValues values = new ContentValues();
		values.put(KEY_CHOICE_ID, choiceId);
		values.put(KEY_CHOICE_VALUE, choiceValue);
		values.put(KEY_USERNAME, username);
		values.put(KEY_CAMPAIGN_URN, campaignUrn);
		values.put(KEY_SURVEY_ID, surveyId);
		values.put(KEY_PROMPT_ID, promptId);
		
		return mDb.insert(TABLE_CHOICES, null, values);
	}
	
	public boolean removeCustomChoice(long _id) {
		
		if (mDb == null) {
			return false;
		}
		
		return mDb.delete(TABLE_CHOICES, KEY_ID + "=?", new String [] {String.valueOf(_id)}) == 1 ? true : false;
	}
	
	public boolean updateCustomChoice(long _id, int choiceId, String choiceValue) {
		
		if (mDb == null) {
			return false;
		}
		
		ContentValues values = new ContentValues();
		values.put(KEY_CHOICE_ID, choiceId);
		values.put(KEY_CHOICE_VALUE, choiceValue);
		
		return mDb.update(TABLE_CHOICES, values, KEY_ID + "=?", new String [] {String.valueOf(_id)}) == 1 ? true : false;
	}
	
	public Cursor getCustomChoices(String username, String campaignUrn, String surveyId, String promptId) {
		
		if (mDb == null) {
			return null;
		}
		
		return mDb.query(TABLE_CHOICES, new String [] {KEY_ID, KEY_CHOICE_ID, KEY_CHOICE_VALUE}, KEY_USERNAME + "=? AND " + KEY_CAMPAIGN_URN + "=? AND " + KEY_SURVEY_ID + "=? AND " + KEY_PROMPT_ID + "=?" , new String [] {username, campaignUrn, surveyId, promptId}, null, null, null);
	}
	
	public long clearCampaign(String campaignUrn) {
		if (mDb == null) {
			return -1;
		}
		
		return mDb.delete(TABLE_CHOICES, KEY_CAMPAIGN_URN + "=?", new String [] {campaignUrn});
	}
	
	public void clearAll() {
		
		if (mDb == null) {
			return;
		}
		
		mDb.execSQL("DROP TABLE IF EXISTS " + TABLE_CHOICES);
		mDbHelper.onCreate(mDb);
	}
}
