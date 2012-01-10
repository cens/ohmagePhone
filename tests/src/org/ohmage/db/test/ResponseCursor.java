
package org.ohmage.db.test;

import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Response;
import org.ohmage.service.SurveyGeotagService;

public class ResponseCursor extends MockArrayCursor<Response> {

	public static final String MOCK_CAMPAIGN_URN = "urn:mock:campaign";

	private static final int COLUMN_ID = 0;
	private static final int COLUMN_LOCATION_STATUS = 1;
	private static final int COLUMN_SURVEY_LAUNCH_CONTEXT = 2;
	private static final int COLUMN_RESPONSE_JSON = 3;
	private static final int COLUMN_CAMPAIGN_URN = 4;
	private static final int COLUMN_TIMEZONE = 5;
	private static final int COLUMN_SURVEY_ID = 6;
	private static final int COLUMN_TIME = 7;

	public ResponseCursor(String[] projection, Response... responses) {
		super(projection, responses);
	}

	@Override
	protected int getLocalColumnIndex(String columnName) {
		if (Responses._ID.equals(columnName))
			return COLUMN_ID;
		else if (Responses.RESPONSE_LOCATION_STATUS.equals(columnName))
			return COLUMN_LOCATION_STATUS;
		else if (Responses.RESPONSE_SURVEY_LAUNCH_CONTEXT.equals(columnName))
			return COLUMN_SURVEY_LAUNCH_CONTEXT;
		else if (Responses.RESPONSE_JSON.equals(columnName))
			return COLUMN_RESPONSE_JSON;
		else if (Responses.CAMPAIGN_URN.equals(columnName))
			return COLUMN_CAMPAIGN_URN;
		else if (Responses.RESPONSE_TIMEZONE.equals(columnName))
			return COLUMN_TIMEZONE;
		else if (Responses.SURVEY_ID.equals(columnName))
			return COLUMN_SURVEY_ID;
		else if (Responses.RESPONSE_TIME.equals(columnName))
			return COLUMN_TIME;
		return COLUMN_IGNORE;
	}

	@Override
	protected double getDoubleValid(int columnIndex) {
		return 0.0;
	}

	@Override
	protected String getStringValid(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_LOCATION_STATUS:
				return getObject().locationStatus;
			case COLUMN_SURVEY_LAUNCH_CONTEXT:
				return getObject().surveyLaunchContext;
			case COLUMN_RESPONSE_JSON:
				return getObject().response;
			case COLUMN_CAMPAIGN_URN:
				return getObject().campaignUrn;
			case COLUMN_TIMEZONE:
				return getObject().timezone;
			case COLUMN_SURVEY_ID:
				return getObject().surveyId;
			default:
				return "";
		}
	}

	@Override
	protected String getStringDefault(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_LOCATION_STATUS:
				return SurveyGeotagService.LOCATION_UNAVAILABLE;
			case COLUMN_SURVEY_LAUNCH_CONTEXT:
				return "{}";
			case COLUMN_RESPONSE_JSON:
				return "[]";
			case COLUMN_CAMPAIGN_URN:
				return MOCK_CAMPAIGN_URN;
			case COLUMN_ID:
				return "1";
			default:
				return "";
		}
	}

	@Override
	protected float getFloatValid(int columnIndex) {
		return 0.0f;
	}

	@Override
	protected long getLongValid(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_TIME:
				return getObject().time;
			default:
				return 0;
		}
	}

	@Override
	protected int getIntValid(int columnIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

}
