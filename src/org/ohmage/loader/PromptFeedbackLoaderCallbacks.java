
package org.ohmage.loader;

import org.ohmage.NIHConfig;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.loader.PromptFeedbackLoader.FeedbackItem;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import java.util.HashMap;
import java.util.LinkedList;

public class PromptFeedbackLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

	/**
	 * The first step is reading the campaign that we want to show data for
	 */
	protected static final int CAMPAIGN_URN_READ = 100;

	/**
	 * Then we can read prompt data
	 */
	protected static final int PROMPT_DATA_READ = 101;

	private static final String EXTRA_CAMPAIGN_URN = "extra_campaign_urn";

	private static final String EXTRA_PROMT_ID = "extra_prompt_id";

	public static interface PromptFeedbackLoaderCallbacksListener {
		void onPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems);
		String[] getPromptList();	
	}

	private PromptFeedbackLoaderCallbacksListener mListener;

	private final Fragment mFragment;
	
	public PromptFeedbackLoaderCallbacks(Fragment fragment) {
		mFragment = fragment;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
			case CAMPAIGN_URN_READ:
				return new CursorLoader(mFragment.getActivity(), Responses.CONTENT_URI,
						new String[] { Campaigns.CAMPAIGN_URN },
						null, null, Responses.RESPONSE_TIME + " DESC");
			case PROMPT_DATA_READ:
				return new PromptFeedbackLoader(mFragment.getActivity(), args.getString(EXTRA_CAMPAIGN_URN), args.getStringArray(EXTRA_PROMT_ID));
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		switch (loader.getId()) {
			case CAMPAIGN_URN_READ:
				if (!cursor.moveToFirst())
					// No campaign found?
					return;

				String campaignUrn = cursor.getString(0);

				Bundle bundle = new Bundle();
				bundle.putString(EXTRA_CAMPAIGN_URN, campaignUrn);
				bundle.putStringArray(EXTRA_PROMT_ID, getPromptList());
				mFragment.getLoaderManager().initLoader(PROMPT_DATA_READ, bundle, this);
				break;
			case PROMPT_DATA_READ:
				dispatchOnPromptReadFinished(((PromptFeedbackLoader) loader).getFeedbackItems());
				break;
		}
	}

	public void setOnPromptReadFinishedListener(PromptFeedbackLoaderCallbacksListener listener) {
		mListener = listener;
	}
	
	private void dispatchOnPromptReadFinished(HashMap<String, LinkedList<FeedbackItem>> feedbackItems) {
		if(mListener != null)
			mListener.onPromptReadFinished(feedbackItems);
	}
	
	private String[] getPromptList() {
		String[] list = null;
		if(mListener != null)
			list = mListener.getPromptList();
		return (list == null) ? NIHConfig.PROMPT_LIST : list;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

	}

	public void start() {
		mFragment.getLoaderManager().initLoader(CAMPAIGN_URN_READ, null, this);
	}
}
