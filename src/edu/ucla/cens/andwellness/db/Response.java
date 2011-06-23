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
