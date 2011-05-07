package edu.ucla.cens.andwellness.db;

public class Response {

	public static final String _ID = "_id";
	public static final String CAMPAIGN_URN = "campaign_urn";
	public static final String USERNAME = "username";
	public static final String DATE = "date";
	public static final String TIME = "time";
	public static final String TIMEZONE = "timezone";
	public static final String LOCATION_STATUS ="location_status";
	public static final String LOCATION_LATITUDE = "location_latitude";
	public static final String LOCATION_LONGITUDE = "location_longitude";
	public static final String LOCATION_PROVIDER = "location_provider";
	public static final String LOCATION_ACCURACY = "location_accuracy";
	public static final String LOCATION_TIME = "location_time";
	public static final String SURVEY_ID = "survey_id";
	public static final String SURVEY_LAUNCH_CONTEXT = "survey_launch_context";
	public static final String RESPONSE = "response";
	
	public long _id;
	public String campaignUrn;
	public String username;
	public String date;
	public long time;
	public String timezone;
	public String locationStatus;
	public double locationLatitude;
	public double locationLongitude;
	public String locationProvider;
	public float locationAccuracy;
	public long locationTime;
	public String surveyId;
	public String surveyLaunchContext;
	public String response;
	
}
