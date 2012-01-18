package org.ohmage.async;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * A custom Loader that can be paused so it doesn't go to the network
 */
public abstract class PauseableTaskLoader<T> extends AsyncTaskLoader<T> {

	private boolean mPause;

	public PauseableTaskLoader(Context context) {
		super(context);
	}

	@Override
	protected void onStartLoading() {
		if(!mPause)
			forceLoad();
	}

	public void pause(boolean pause) {
		mPause = pause;
	}

	public boolean isPaused() {
		return mPause;
	}
}