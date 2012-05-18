package org.ohmage.async;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * A custom Loader that can be paused so it doesn't go to the network
 */
public abstract class PauseableTaskLoader<T> extends AsyncTaskLoader<T> {

	private boolean mPause;
	protected long startTime;

	public PauseableTaskLoader(Context context) {
		super(context);
		startTime = System.currentTimeMillis();
	}

	@Override
	protected T onLoadInBackground() {
		startTime = System.currentTimeMillis();
		return super.onLoadInBackground();
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