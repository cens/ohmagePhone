
package org.ohmage.db.test;

import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.Models.Survey;

public class SurveyCursor extends MockArrayCursor<Survey> {

	private static final int COLUMN_SURVEY_TITLE = 0;
	private static final int COLUMN_SURVEY_ID = 1;

	public SurveyCursor(String[] projection, Survey... surveys) {
		super(projection, surveys);
	}

	@Override
	protected int getLocalColumnIndex(String columnName) {
		if (Surveys.SURVEY_TITLE.equals(columnName))
			return COLUMN_SURVEY_TITLE;
		else if(Surveys.SURVEY_ID.equals(columnName))
			return COLUMN_SURVEY_ID;
		return COLUMN_IGNORE;
	}

	@Override
	protected String getStringValid(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_SURVEY_TITLE:
				return getObject().mTitle;
			case COLUMN_SURVEY_ID:
				return getObject().mSurveyID;
			default:
				return "";
		}
	}

	@Override
	protected String getStringDefault(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_SURVEY_TITLE:
				return "Fake Survey";
			default:
				return "";
		}
	}

	@Override
	protected long getLongValid(int columnIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected double getDoubleValid(int columnIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected float getFloatValid(int columnIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getIntValid(int columnIndex) {
		// TODO Auto-generated method stub
		return 0;
	}
}
