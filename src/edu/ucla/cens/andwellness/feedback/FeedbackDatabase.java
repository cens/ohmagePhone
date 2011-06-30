package edu.ucla.cens.andwellness.feedback;

import edu.ucla.cens.andwellness.PromptResponse;
import edu.ucla.cens.andwellness.db.Response;
import edu.ucla.cens.systemlog.Log;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class FeedbackDatabase extends SQLiteOpenHelper {
	private static final String TAG = "FeedbackDatabase";
	
	private static final String DB_NAME = "andw.feedback.db";
	private static final int DB_VERSION = 1;
	private static final String TABLE_RESPONSES = "responses";

	private static final String TABLE_PROMPT_RESPONSES = "prompt_responses";
	
	private static boolean isDbOpen = false;
	private static Object dbLock = new Object();

	public FeedbackDatabase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// create our caching table for responses
		db.execSQL("CREATE TABLE " + TABLE_RESPONSES + " ("
				+ Response._ID + " INTEGER PRIMARY KEY, "
				+ Response.CAMPAIGN_URN + " TEXT, "
				+ Response.USERNAME + " TEXT, "
				+ Response.DATE + " TEXT, "
				+ Response.TIME + " INTEGER, "
				+ Response.TIMEZONE + " TEXT, "
				+ Response.LOCATION_STATUS + " TEXT, "
				+ Response.LOCATION_LATITUDE + " REAL, "
				+ Response.LOCATION_LONGITUDE + " REAL, "
				+ Response.LOCATION_PROVIDER + " TEXT, "
				+ Response.LOCATION_ACCURACY + " REAL, "
				+ Response.LOCATION_TIME + " INTEGER, "
				+ Response.SURVEY_ID + " TEXT, "
				+ Response.SURVEY_LAUNCH_CONTEXT + " TEXT, "
				+ Response.RESPONSE + " TEXT"
				+ ");");
		
		// create a "flat" table of prompt responses so we
		// can easily compute aggregates across multiple
		// survey responses (and potentially prompts)
		// NOTE: be sure to delete all entries from this table
		// whose "response_id" matches the Response._id column when
		// deleting from the TABLE_RESPONSES table
		db.execSQL("CREATE TABLE " + TABLE_PROMPT_RESPONSES + " ("
				+ "response_id INTEGER, "
				+ "prompt_id INTEGER, "
				+ "prompt_value TEXT"
				+ ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// destroy and recreate our tables
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESPONSES);
		onCreate(db);
	}

	private SQLiteDatabase openDb() {
		synchronized(dbLock)
		{
			while (isDbOpen)
			{
				try
				{
					dbLock.wait();
				}
				catch (InterruptedException e){}

			}
			isDbOpen = true;
			try {
				return getWritableDatabase();
			} catch (SQLiteException e) {
				Log.e(TAG, "Error opening database: " + DB_NAME);
				isDbOpen = false;
				return null;
			}
		}
	}
	
	private void closeDb(SQLiteDatabase db) {
		synchronized(dbLock)
		{
			db.close();
			isDbOpen = false;
			dbLock.notify();
		}		
	}
}
