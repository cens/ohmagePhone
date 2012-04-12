package org.ohmage.loader;

import org.ohmage.NIHConfig;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

import java.util.Calendar;
import java.util.HashMap;

public class AggregateLoader extends CursorLoader {

	private static String[] mProjection = new String[] {
		PromptResponses.PROMPT_ID, 
		PromptResponses.AggregateTypes.SUM.toString(),
		PromptResponses.AggregateTypes.COUNT.toString()
	};

	private static final int COLUMN_PROMPT_ID = 0;
	private static final int COLUMN_SUM = 1;
	private static final int COLUMN_COUNT = 2;

	public static final int BASELINE = 0;
	public static final int LAST_WEEK = 1;
	public static final int THIS_WEEK = 2;

	private HashMap<String, Double> mAverages;

	public AggregateLoader(Context context, String campaignUrn, int select) {
		super(context, PromptResponses.buildPromptsAggregatesUri(campaignUrn), mProjection,
				getSelect(context, select), null, null);
	}

	@Override
	public Cursor loadInBackground() {
		Cursor cursor = super.loadInBackground();
		if (cursor != null) {

			HashMap<String, Double> sum = new HashMap<String, Double>();
			HashMap<String, Double> counts = new HashMap<String, Double>();

			while(cursor.moveToNext()) {
				String prompt = NIHConfig.getPrompt(cursor.getString(COLUMN_PROMPT_ID));
				if(prompt != null) {
					Double s = sum.get(prompt);
					if(s == null) {
						s = 0.0;
					}
					Double count = counts.get(prompt);
					if(count == null) {
						count = 0.0;
					}

					s += cursor.getDouble(COLUMN_SUM);
					count += cursor.getDouble(COLUMN_COUNT);

					sum.put(prompt, s);
					counts.put(prompt, count);
				}
			}
			cursor.close();

			mAverages = new HashMap<String, Double>();
			for(String k : sum.keySet()) {
				mAverages.put(k, sum.get(k) / counts.get(k));
			}
		}
		return cursor;
	}

	public HashMap<String, Double> getAverages() {
		return mAverages;
	}

	/**
	 * Calculates the select statements for this week, last week, or baseline
	 * @param context
	 * @param select
	 * @return
	 */
	public static String getSelect(Context context, int select) {
		switch(select) {
			case BASELINE: {
				return Responses.RESPONSE_TIME + " < " + UserPreferencesHelper.getBaseLineEndTime(context);
			} case LAST_WEEK: {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, -cal.get(Calendar.DAY_OF_WEEK) + 1);
				Utilities.clearTime(cal);
				long end = cal.getTimeInMillis();
				cal.add(Calendar.DATE, -7);
				return Responses.RESPONSE_TIME + " > " + cal.getTimeInMillis() + " AND " + Responses.RESPONSE_TIME + " < " + end;
			} case THIS_WEEK: {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, -cal.get(Calendar.DAY_OF_WEEK) + 1);
				Utilities.clearTime(cal);
				return Responses.RESPONSE_TIME + " > " + cal.getTimeInMillis();
			}
		}
		return null;
	}
}