package edu.ucla.cens.andwellness.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import edu.ucla.cens.andwellness.campaign.Campaign;
import edu.ucla.cens.andwellness.service.SurveyGeotagService;
import edu.ucla.cens.systemlog.Log;

public class DbHelper extends SQLiteOpenHelper{
	
	private static final String TAG = "DbHelper";
	
	private static final String DB_NAME = "andwellness.db";
	private static final int DB_VERSION = 1;
	private static final String TABLE_RESPONSES = "responses";
	private static final String TABLE_CAMPAIGNS = "campaigns";
	
	
	private static boolean isDbOpen = false;
	private static Object dbLock = new Object();

	public DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {

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
		
		db.execSQL("CREATE TABLE " + TABLE_CAMPAIGNS + " ("
				+ Campaign._ID + " INTEGER PRIMARY KEY, "
				+ Campaign.URN + " TEXT, "
				+ Campaign.NAME + " TEXT, "
				+ Campaign.CREATION_TIMESTAMP + " TEXT, "
				+ Campaign.DOWNLOAD_TIMESTAMP + " TEXT, "
				+ Campaign.CONFIGURATION_XML + " TEXT "
				+ ");");
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESPONSES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAMPAIGNS);
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
	
	public void clearAll() {
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return;
		}
		
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESPONSES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAMPAIGNS);
		onCreate(db);
	}

	public long addResponseRow(String campaignUrn, String username, String date, long time, String timezone, String locationStatus, double locationLatitude, double locationLongitude, String locationProvider, float locationAccuracy, long locationTime, String surveyId, String surveyLaunchContext, String response) {
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return -1;
		}
		
		ContentValues values = new ContentValues();
		values.put(Response.CAMPAIGN_URN, campaignUrn);
		values.put(Response.USERNAME, username);
		values.put(Response.DATE, date);
		values.put(Response.TIME, time);
		values.put(Response.TIMEZONE, timezone);
		values.put(Response.LOCATION_STATUS, locationStatus);
		values.put(Response.LOCATION_LATITUDE, locationLatitude);
		values.put(Response.LOCATION_LONGITUDE, locationLongitude);
		values.put(Response.LOCATION_PROVIDER, locationProvider);
		values.put(Response.LOCATION_ACCURACY, locationAccuracy);
		values.put(Response.LOCATION_TIME, locationTime);
		values.put(Response.SURVEY_ID, surveyId);
		values.put(Response.SURVEY_LAUNCH_CONTEXT, surveyLaunchContext);
		values.put(Response.RESPONSE, response);
		
		long rowId = db.insert(TABLE_RESPONSES, null, values);
		
		closeDb(db);
		
		return rowId;
	}
	
	public long addResponseRowWithoutLocation(String campaignUrn, String username, String date, long time, String timezone, String surveyId, String surveyLaunchContext, String response) {
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return -1;
		}
		
		ContentValues values = new ContentValues();
		values.put(Response.CAMPAIGN_URN, campaignUrn);
		values.put(Response.USERNAME, username);
		values.put(Response.DATE, date);
		values.put(Response.TIME, time);
		values.put(Response.TIMEZONE, timezone);
		values.put(Response.LOCATION_STATUS, SurveyGeotagService.LOCATION_UNAVAILABLE);
		/*values.put(Response.LOCATION_LATITUDE, null);
		values.put(Response.LOCATION_LONGITUDE, null);
		values.put(Response.LOCATION_PROVIDER, null);
		values.put(Response.LOCATION_ACCURACY, null);
		values.put(Response.LOCATION_TIME, null);*/
		values.put(Response.SURVEY_ID, surveyId);
		values.put(Response.SURVEY_LAUNCH_CONTEXT, surveyLaunchContext);
		values.put(Response.RESPONSE, response);
		
		long rowId = db.insert(TABLE_RESPONSES, null, values);
		
		closeDb(db);
		
		return rowId;
	}
	
	public boolean removeResponseRow(long _id) {
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return false;
		}
		
		int count = db.delete(TABLE_RESPONSES, Response._ID + "=" + _id, null);
		
		closeDb(db);
		
		return count > 0;
	}
	
	public boolean removeResponseRows(String campaignUrn) {
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return false;
		}
		
		int count = db.delete(TABLE_RESPONSES, Response.CAMPAIGN_URN + "='" + campaignUrn + "'", null);
		
		closeDb(db);
		
		return count > 0;
	}
	
	public void getResponseRow() {
		
	}
	
	public List<Response> getSurveyResponses(String campaignUrn) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		Cursor cursor = db.query(TABLE_RESPONSES, null, Response.CAMPAIGN_URN + "='" + campaignUrn + "'", null, null, null, null);
		
		List<Response> responses = readResponseRows(cursor); 
			
		closeDb(db);
		
		return responses;
	}
	
//	public List<Response> getSurveyResponsesBefore(long cutoffTime) {
//		
//		SQLiteDatabase db = openDb();
//		
//		if (db == null) {
//			return null;
//		}
//		
//		Cursor cursor = db.query(TABLE_RESPONSES, null, Response.TIME + " < " + Long.toString(cutoffTime), null, null, null, null);
//		
//		List<Response> responses = readResponseRows(cursor); 
//		
//		closeDb(db);
//		
//		return responses;
//	}
	
	public List<Response> getSurveyResponsesBefore(String campaignUrn, long cutoffTime) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		Cursor cursor = db.query(TABLE_RESPONSES, null, Response.CAMPAIGN_URN + "='" + campaignUrn + "' AND " + Response.TIME + " < " + Long.toString(cutoffTime), null, null, null, null);
		
		List<Response> responses = readResponseRows(cursor); 
		
		closeDb(db);
		
		return responses;
	}
	
	private List<Response> readResponseRows(Cursor cursor) {
		
		ArrayList<Response> responses = new ArrayList<Response>();
		
		cursor.moveToFirst();
		
		for (int i = 0; i < cursor.getCount(); i++) {
			
			Response r = new Response();
			r._id = cursor.getLong(cursor.getColumnIndex(Response._ID));
			r.campaignUrn = cursor.getString(cursor.getColumnIndex(Response.CAMPAIGN_URN));
			r.username = cursor.getString(cursor.getColumnIndex(Response.USERNAME));
			r.date = cursor.getString(cursor.getColumnIndex(Response.DATE));
			r.time = cursor.getLong(cursor.getColumnIndex(Response.TIME));
			r.timezone = cursor.getString(cursor.getColumnIndex(Response.TIMEZONE));
			r.locationStatus = cursor.getString(cursor.getColumnIndex(Response.LOCATION_STATUS));
			if (! r.locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE)) {
				
				r.locationLatitude = cursor.getDouble(cursor.getColumnIndex(Response.LOCATION_LATITUDE));
				r.locationLongitude = cursor.getDouble(cursor.getColumnIndex(Response.LOCATION_LONGITUDE));
				r.locationProvider = cursor.getString(cursor.getColumnIndex(Response.LOCATION_PROVIDER));
				r.locationAccuracy = cursor.getFloat(cursor.getColumnIndex(Response.LOCATION_ACCURACY));
				r.locationTime = cursor.getLong(cursor.getColumnIndex(Response.LOCATION_TIME));
			}
			r.surveyId = cursor.getString(cursor.getColumnIndex(Response.SURVEY_ID));
			r.surveyLaunchContext = cursor.getString(cursor.getColumnIndex(Response.SURVEY_LAUNCH_CONTEXT));
			r.response = cursor.getString(cursor.getColumnIndex(Response.RESPONSE));
			responses.add(r);
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		return responses; 
	}
	
	/*public List<Response> getResponseRows() {
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		ArrayList<Response> responses = new ArrayList<Response>();
		
		Cursor cursor = db.query(TABLE_RESPONSES, null, null, null, null, null, null);
		
		cursor.moveToFirst();
		
		for (int i = 0; i < cursor.getCount(); i++) {
			
			Response r = new Response();
			r._id = cursor.getLong(cursor.getColumnIndex(Response._ID));
			r.campaign = cursor.getString(cursor.getColumnIndex(Response.CAMPAIGN));
			r.campaignVersion = cursor.getString(cursor.getColumnIndex(Response.CAMPAIGN_VERSION));
			r.username = cursor.getString(cursor.getColumnIndex(Response.USERNAME));
			r.date = cursor.getString(cursor.getColumnIndex(Response.DATE));
			r.time = cursor.getLong(cursor.getColumnIndex(Response.TIME));
			r.timezone = cursor.getString(cursor.getColumnIndex(Response.TIMEZONE));
			r.locationStatus = cursor.getString(cursor.getColumnIndex(Response.LOCATION_STATUS));
			if (! r.locationStatus.equals(SurveyLocationService.LOCATION_UNAVAILABLE)) {
				
				r.locationLatitude = cursor.getDouble(cursor.getColumnIndex(Response.LOCATION_LATITUDE));
				r.locationLongitude = cursor.getDouble(cursor.getColumnIndex(Response.LOCATION_LONGITUDE));
				r.locationProvider = cursor.getString(cursor.getColumnIndex(Response.LOCATION_PROVIDER));
				r.locationAccuracy = cursor.getFloat(cursor.getColumnIndex(Response.LOCATION_ACCURACY));
				r.locationTime = cursor.getLong(cursor.getColumnIndex(Response.LOCATION_TIME));
			}
			r.surveyId = cursor.getString(cursor.getColumnIndex(Response.SURVEY_ID));
			r.surveyLaunchContext = cursor.getString(cursor.getColumnIndex(Response.SURVEY_LAUNCH_CONTEXT));
			r.response = cursor.getString(cursor.getColumnIndex(Response.RESPONSE));
			responses.add(r);
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		closeDb(db);
		
		return responses;
	}*/
	
	public void updateResponseRow() {
		
	}

	public int updateRecentRowLocations(String locationStatus, double locationLatitude, double locationLongitude, String locationProvider, float locationAccuracy, long locationTime) {
		if (locationStatus.equals(SurveyGeotagService.LOCATION_UNAVAILABLE)) return -1;
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return -1;
		}
		
		ContentValues vals = new ContentValues();
		vals.put(Response.LOCATION_STATUS, locationStatus);
		vals.put(Response.LOCATION_LATITUDE, locationLatitude);
		vals.put(Response.LOCATION_LONGITUDE, locationLongitude);
		vals.put(Response.LOCATION_PROVIDER, locationProvider);
		vals.put(Response.LOCATION_ACCURACY, locationAccuracy);
		vals.put(Response.LOCATION_TIME, locationTime);
		
		
		long earliestTimestampToUpdate = locationTime - SurveyGeotagService.LOCATION_STALENESS_LIMIT;
		
		int count = db.update(TABLE_RESPONSES, vals, Response.LOCATION_STATUS + " = '" + SurveyGeotagService.LOCATION_UNAVAILABLE + "' AND " + Response.TIME + " > " + earliestTimestampToUpdate, null);
		
		closeDb(db);
		
		return count;
	}
	
	public long addCampaign(String campaignUrn, String campaignName, String creationTimestamp, String downloadTimestamp, String configurationXml) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return -1;
		}
		
		ContentValues values = new ContentValues();
		values.put(Campaign.URN, campaignUrn);
		values.put(Campaign.NAME, campaignName);
		values.put(Campaign.CREATION_TIMESTAMP, creationTimestamp);
		values.put(Campaign.DOWNLOAD_TIMESTAMP, downloadTimestamp);
		values.put(Campaign.CONFIGURATION_XML, configurationXml);
		
		long rowId = db.insert(TABLE_CAMPAIGNS, null, values);
		
		closeDb(db);
		
		return rowId;
	}
	
	public boolean removeCampaign(long _id) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return false;
		}
		
		int count = db.delete(TABLE_CAMPAIGNS, Campaign._ID + "=" + _id, null);
		
		closeDb(db);
		
		return count > 0;
	}
	
	public boolean removeCampaign(String urn) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return false;
		}
		
		int count = db.delete(TABLE_CAMPAIGNS, Campaign.URN + "='" + urn +"'", null);
		
		closeDb(db);
		
		return count > 0;
	}
	
	public Campaign getCampaign(long _id) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		Cursor cursor = db.query(TABLE_CAMPAIGNS, null, Campaign._ID + "=" + _id, null, null, null, null);
		
		cursor.moveToFirst();
		
		Campaign c = new Campaign();
		c._id = cursor.getLong(cursor.getColumnIndex(Campaign._ID));
		c.mUrn = cursor.getString(cursor.getColumnIndex(Campaign.URN));
		c.mName = cursor.getString(cursor.getColumnIndex(Campaign.NAME));
		c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.CREATION_TIMESTAMP));
		c.mDownloadTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.DOWNLOAD_TIMESTAMP));
		c.mXml = cursor.getString(cursor.getColumnIndex(Campaign.CONFIGURATION_XML));
		
		cursor.close();
		
		closeDb(db);
		
		return c;
	}
	
	public Campaign getCampaign(String urn) {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		Cursor cursor = db.query(TABLE_CAMPAIGNS, null, Campaign.URN + "= '" + urn + "'", null, null, null, null);
		
		cursor.moveToFirst();
		
		Campaign c = new Campaign();
		c._id = cursor.getLong(cursor.getColumnIndex(Campaign._ID));
		c.mUrn = cursor.getString(cursor.getColumnIndex(Campaign.URN));
		c.mName = cursor.getString(cursor.getColumnIndex(Campaign.NAME));
		c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.CREATION_TIMESTAMP));
		c.mDownloadTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.DOWNLOAD_TIMESTAMP));
		c.mXml = cursor.getString(cursor.getColumnIndex(Campaign.CONFIGURATION_XML));
		
		cursor.close();
		
		closeDb(db);
		
		return c;
	}
	
	public List<Campaign> getCampaigns() {
		
		SQLiteDatabase db = openDb();
		
		if (db == null) {
			return null;
		}
		
		List<Campaign> campaigns = new ArrayList<Campaign>();
		
		Cursor cursor = db.query(TABLE_CAMPAIGNS, null, null, null, null, null, null);
		
		cursor.moveToFirst();
		
		for (int i = 0; i < cursor.getCount(); i++) {
			
			Campaign c = new Campaign();
			c._id = cursor.getLong(cursor.getColumnIndex(Campaign._ID));
			c.mUrn = cursor.getString(cursor.getColumnIndex(Campaign.URN));
			c.mName = cursor.getString(cursor.getColumnIndex(Campaign.NAME));
			c.mCreationTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.CREATION_TIMESTAMP));
			c.mDownloadTimestamp = cursor.getString(cursor.getColumnIndex(Campaign.DOWNLOAD_TIMESTAMP));
			c.mXml = cursor.getString(cursor.getColumnIndex(Campaign.CONFIGURATION_XML));
			campaigns.add(c);
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		closeDb(db);
		
		return campaigns; 
	}
}
