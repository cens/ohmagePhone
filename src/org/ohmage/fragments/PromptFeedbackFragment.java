
package org.ohmage.fragments;

import org.ohmage.loader.PromptFeedbackLoaderCallbacks;
import org.ohmage.loader.PromptFeedbackLoaderCallbacks.PromptFeedbackLoaderCallbacksListener;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

public abstract class PromptFeedbackFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>, PromptFeedbackLoaderCallbacksListener {

	private PromptFeedbackLoaderCallbacks mPromptFeedbackHelper;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mPromptFeedbackHelper = new PromptFeedbackLoaderCallbacks(this);
		mPromptFeedbackHelper.setOnPromptReadFinishedListener(this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return mPromptFeedbackHelper.onCreateLoader(id, args);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mPromptFeedbackHelper.onLoadFinished(loader, cursor);
	}

	protected void startCampaignRead() {
		mPromptFeedbackHelper.start();
	}

	@Override
	public String[] getPromptList() {
		return null;
	}
}
