package org.ohmage.test.helper;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.test.InstrumentationTestCase;

public class LoaderHelper {

	private final FragmentActivity mContext;
	private final LoaderCallbacks<Cursor> mLoader;
	private final InstrumentationTestCase mTester;

	public LoaderHelper(FragmentActivity context, LoaderManager.LoaderCallbacks<Cursor> loaderCallback, InstrumentationTestCase tester) {
		mContext = context;
		mLoader = loaderCallback;
		mTester = tester;
	}

	public LoaderHelper(FragmentActivity context, InstrumentationTestCase tester) {
		this(context, (LoaderCallbacks<Cursor>) context, tester);
	}

	public void setEntityContentValues(final ContentValues values) {
		final CursorLoader loader = (CursorLoader) mLoader.onCreateLoader(0, null);
		mContext.getContentResolver().update(loader.getUri(), values, loader.getSelection(), loader.getSelectionArgs());

		// Wait for the activity to be idle so we know its not processing other loader requests.
		mTester.getInstrumentation().waitForIdleSync();

		// Then wait for the loader
		waitForLoader();
	}

	public Cursor getEntity() {
		CursorLoader loader = (CursorLoader) mLoader.onCreateLoader(0, null);
		return mContext.getContentResolver().query(loader.getUri(), null, loader.getSelection(), loader.getSelectionArgs(), loader.getSortOrder());
	}

	public void restartLoader() {
		mContext.getSupportLoaderManager().restartLoader(0, null, mLoader);
	}

	public AsyncTaskLoader<Object> getDataLoader() {
		return (AsyncTaskLoader<Object>) mContext.getSupportLoaderManager().getLoader(0);
	}

	public void waitForLoader() {
		startLoading();
		getDataLoader().waitForLoader();
	}

	public void startLoading() {
		getDataLoader().startLoading();
	}

	public void stopLoading() {
		getDataLoader().stopLoading();
	}
}
