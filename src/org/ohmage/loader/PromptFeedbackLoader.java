package org.ohmage.loader;

import org.ohmage.NIHConfig;
import org.ohmage.db.DbContract.PromptResponses;
import org.ohmage.db.DbContract.Responses;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

import java.util.HashMap;
import java.util.LinkedList;

public class PromptFeedbackLoader extends CursorLoader{
	
	public static class FeedbackItem {
		public Double value;
		public long time;
	}

	private static String[] mProjection = new String[] {
		PromptResponses.PROMPT_RESPONSE_EXTRA_VALUE,
		Responses.RESPONSE_TIME,
		PromptResponses.PROMPT_ID
	};
	
	private static final int COLUMN_EXTRA_VALUE = 0;
	private static final int COLUMN_RESPONSE_TIME = 1;
	private static final int COLUMN_PROMPT = 2;

	private final HashMap<String, LinkedList<FeedbackItem>> mFeedbackItems;

	public PromptFeedbackLoader(Context context, String campaignUrn, String... promptSQL) {
		super(context, PromptResponses.getPromptsByCampaign(campaignUrn, promptSQL), mProjection,
				Responses.RESPONSE_TIME + " < " + System.currentTimeMillis(), null,
				Responses.RESPONSE_TIME + " DESC");
		mFeedbackItems = new HashMap<String, LinkedList<FeedbackItem>>();
	}

	@Override
    public Cursor loadInBackground() {
    	
        Cursor cursor = super.loadInBackground();
        if (cursor != null) {

        	LinkedList<FeedbackItem> data;
        	String prompt;
        	FeedbackItem point;
        	
			Double value;
			while(cursor.moveToNext()) {
				prompt = NIHConfig.getPrompt(cursor.getString(COLUMN_PROMPT));
				data = mFeedbackItems.get(prompt);
				if(data == null) {
					data = new LinkedList<FeedbackItem>();
					mFeedbackItems.put(prompt, data);
				}
				
				value = null;
				if(!cursor.isNull(COLUMN_EXTRA_VALUE)) {
					value = cursor.getDouble(COLUMN_EXTRA_VALUE);
				}

				if(value != null) {
					point = new FeedbackItem();
					point.value = value;
					point.time = cursor.getLong(COLUMN_RESPONSE_TIME);
					data.add(point);
				}
			}
        }
        return cursor;
    }
    
    public HashMap<String, LinkedList<FeedbackItem>> getFeedbackItems() {
    	return mFeedbackItems;
    }
}