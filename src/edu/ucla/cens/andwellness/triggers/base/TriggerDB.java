package edu.ucla.cens.andwellness.triggers.base;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*
 * The main db to store triggers
 */
public class TriggerDB {

	private static final String DEBUG_TAG = "TriggerFramework";
	
	private static final String DATABASE_NAME = "trigger_framework"; 
	private static final int DATABASE_VERSION = 1;
	
	private static final String TABLE_TRIGGERS = "triggers";
	
	public static final String KEY_ID = "_id";
	public static final String KEY_TRIG_TYPE = "trigger_type";
	public static final String KEY_TRIG_DESCRIPT = "trig_descript";
	public static final String KEY_TRIG_ACTION_DESCRIPT = "trig_action_descript";
	public static final String KEY_NOTIF_DESCRIPT = "notif_descript";
	public static final String KEY_RUNTIME_DESCRIPT = "runtime_descript";
	
	private Context mContext;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	public TriggerDB(Context context) {
		this.mContext = context;
	}
	
	/* Open the database */
	public boolean open() {
		Log.i(DEBUG_TAG, "DB: open");
	
		
		mDbHelper = new DatabaseHelper(mContext);
		
		try {
			mDb = mDbHelper.getWritableDatabase();
		}
		catch (SQLException e) {
			Log.e(DEBUG_TAG, e.toString());
			return false;
		}
		return true;
	}
	
	/* Close the database */
	public void close() {
		Log.i(DEBUG_TAG, "DB: close");
		
		if(mDbHelper != null) {
			mDbHelper.close();
		}
	}
	
	public long addTrigger(String trigType, 
						  String trigDescript,
						  String trigActDesc,
					      String notifDescript, 
					      String rtDescript) {
		
		Log.i(DEBUG_TAG, "DB: addTrigger(" + trigType +
										 ", " + trigDescript + 
										 ", " + trigActDesc + 
										 ", " + notifDescript + 
										 ", " + rtDescript + ")");
		
		ContentValues values = new ContentValues();
		values.put(KEY_TRIG_TYPE, trigType);
		values.put(KEY_TRIG_DESCRIPT, trigDescript);
		values.put(KEY_TRIG_ACTION_DESCRIPT, trigActDesc);
		values.put(KEY_NOTIF_DESCRIPT, notifDescript);
		values.put(KEY_RUNTIME_DESCRIPT, rtDescript);

		return mDb.insert(TABLE_TRIGGERS, null, values);
	}
	
	public Cursor getTrigger(int trigId) {
		Log.i(DEBUG_TAG, "DB: getTrigger(" + trigId + ")");
		
		return mDb.query(TABLE_TRIGGERS, null, 
						 KEY_ID + "=?", new String[] {String.valueOf(trigId)},  
						 null, null, null);
	}
	
	public Cursor getTriggers(String trigType) {
		Log.i(DEBUG_TAG, "DB: getTriggers(" + trigType + ")");
		
		return mDb.query(TABLE_TRIGGERS, null, 
						 KEY_TRIG_TYPE + "=?", 
						 new String[] {String.valueOf(trigType)}, 
						 null, null, null);
	}
	
	public Cursor getAllTriggers() {
		Log.i(DEBUG_TAG, "DB: getAllTriggers");
		
		return mDb.query(TABLE_TRIGGERS, null, null, 
				null, null, null, null);
	}
	
	public String getNotifDescription(int trigId) {
		Log.i(DEBUG_TAG, "DB: getNotifDescription(" + trigId + ")");
		
		Cursor c = mDb.query(TABLE_TRIGGERS, new String[] {KEY_NOTIF_DESCRIPT}, 
							 KEY_ID + "=?", new String[] {String.valueOf(trigId)}, 
							 null, null, null);
	
		String notifDesc = null;
		if(c.moveToFirst()) {
			notifDesc = c.getString(
					       c.getColumnIndexOrThrow(KEY_NOTIF_DESCRIPT));
		}
		c.close();
		return notifDesc;
	}
	
	public String getTriggerType(int trigId) {
		Log.i(DEBUG_TAG, "DB: getTriggerType(" + trigId + ")");
		
		Cursor c = mDb.query(TABLE_TRIGGERS, new String[] {KEY_TRIG_TYPE}, 
							 KEY_ID + "=?", new String[] {String.valueOf(trigId)}, 
							 null, null, null);
	
		String trigType = null;
		if(c.moveToFirst()) {
			trigType = c.getString(
								c.getColumnIndexOrThrow(KEY_TRIG_TYPE));
		}
		c.close();
		return trigType;
	}
	
	public String getTriggerDescription(int trigId) {
		Log.i(DEBUG_TAG, "DB: getTriggerDescription(" + trigId + ")");
		
		Cursor c = mDb.query(TABLE_TRIGGERS, new String[] {KEY_TRIG_DESCRIPT}, 
							 KEY_ID + "=?", new String[] {String.valueOf(trigId)}, 
							 null, null, null);
	
		String trigDesc = null;
		if(c.moveToFirst()) {
			trigDesc = c.getString(
					       	c.getColumnIndexOrThrow(KEY_TRIG_DESCRIPT));
		}
		c.close();
		return trigDesc;
	}

	public String getActionDescription(int trigId) {
		Log.i(DEBUG_TAG, "DB: getActionDescription(" + trigId + ")");
		
		Cursor c = mDb.query(TABLE_TRIGGERS, new String[] {KEY_TRIG_ACTION_DESCRIPT}, 
							 KEY_ID + "=?", new String[] {String.valueOf(trigId)}, 
							 null, null, null);
	
		String actDesc = null;
		if(c.moveToFirst()) {
			actDesc = c.getString(
					     	c.getColumnIndexOrThrow(KEY_TRIG_ACTION_DESCRIPT));
		}
		c.close();
		return actDesc;
	}
	
	public String getRunTimeDescription(int trigId) {
		Log.i(DEBUG_TAG, "DB: getRunTimeDescription(" + trigId + ")");
		
		Cursor c = mDb.query(TABLE_TRIGGERS, new String[] {KEY_RUNTIME_DESCRIPT}, 
							 KEY_ID + "=?", new String[] {String.valueOf(trigId)}, 
							 null, null, null);
	
		String rtDesc = null;
		if(c.moveToFirst()) {
			rtDesc = c.getString(
					 c.getColumnIndexOrThrow(KEY_RUNTIME_DESCRIPT));
		}
		c.close();
		return rtDesc;
	}
	
	public boolean updateTriggerDescription(int trigId, String newDesc) {
		Log.i(DEBUG_TAG, "DB: updateTriggerDescription(" + trigId + 
													   ", " + newDesc + ")");
		
		ContentValues values = new ContentValues();
		values.put(KEY_TRIG_DESCRIPT, newDesc);
		
		if(mDb.update(TABLE_TRIGGERS, values, 
					  KEY_ID + "=?", 
					  new String[]{ String.valueOf(trigId)}) != 1) {
			return false;
		}
		
		return true;
	}
	
	public boolean updateActionDescription(int trigId, String newDesc) {
		Log.i(DEBUG_TAG, "DB: updateActionDescription(" + trigId + 
				   ", " + newDesc + ")");
		
		ContentValues values = new ContentValues();
		values.put(KEY_TRIG_ACTION_DESCRIPT, newDesc);
		
		if(mDb.update(TABLE_TRIGGERS, values, 
					  KEY_ID + "=?", 
					  new String[]{ String.valueOf(trigId)}) != 1) {
			return false;
		}
		
		return true;
	}

	public boolean updateRunTimeDescription(int trigId, String newDesc) {
		Log.i(DEBUG_TAG, "DB: updateRunTimeDescription(" + trigId + 
				   ", " + newDesc + ")");
		
		ContentValues values = new ContentValues();
		values.put(KEY_RUNTIME_DESCRIPT, newDesc);
		
		if(mDb.update(TABLE_TRIGGERS, values, 
					  KEY_ID + "=?", 
					  new String[]{ String.valueOf(trigId)}) != 1) {
			return false;
		}
		
		return true;
	}
	
	public boolean updateAllNotificationDescriptions(String newDesc) {
		Log.i(DEBUG_TAG, "DB: updateAllNotificationDescriptions(" + newDesc + ")");
		
		ContentValues values = new ContentValues();
		values.put(KEY_NOTIF_DESCRIPT, newDesc);
		
		mDb.update(TABLE_TRIGGERS, values, null, null);
		return true;
	}
	
	public boolean deleteTrigger(int trigId) {
		Log.i(DEBUG_TAG, "DB: deleteTrigger(" + trigId + ")");
		
		mDb.delete(TABLE_TRIGGERS, KEY_ID + "=?", 
				   new String[] {String.valueOf(trigId)});

		return true;
	}
	
	
	/* Database helper inner class */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase mDb) {
			Log.i(DEBUG_TAG, "DB: SQLiteOpenHelper.onCreate");
			
			final String QUERY_CREATE_TRIGGERS_TB = 
				"create table " + TABLE_TRIGGERS + " ("
				 + KEY_ID + " integer primary key autoincrement, "
				 + KEY_TRIG_TYPE + " text not null, "
				 + KEY_TRIG_DESCRIPT + " text, "
				 + KEY_TRIG_ACTION_DESCRIPT + " text, "
				 + KEY_NOTIF_DESCRIPT + " text, "
				 + KEY_RUNTIME_DESCRIPT + " text)";
			
			//Create the table
			mDb.execSQL(QUERY_CREATE_TRIGGERS_TB);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, 
				int newVersion) {
		}
	}
}
