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
	private final CursorLoader mCursorLoader;

	public LoaderHelper(FragmentActivity context, LoaderManager.LoaderCallbacks<Cursor> loaderCallback, InstrumentationTestCase tester) {
		mContext = context;
		mLoader = loaderCallback;
		mTester = tester;
		mCursorLoader = (CursorLoader) mLoader.onCreateLoader(0, null);
	}

	public LoaderHelper(FragmentActivity context, InstrumentationTestCase tester) {
		this(context, (LoaderCallbacks<Cursor>) context, tester);
	}
	
	public void setEntityContentValues(ContentValues values) {
		setEntityContentValues(values, false);
	}
	
	/**
	 * Sets the contentvalues for an entity. Won't update the db if the values don't cause a change.
	 * @param values
	 * @param force
	 */
	public void setEntityContentValues(ContentValues values, boolean force) {
		if(!force) {
			Cursor entity = getEntity();
			if(entity.moveToFirst()) {
				for(int i=0;i<entity.getColumnCount();i++) {
					String key =  entity.getColumnName(i);
					if(values.containsKey(key) && !values.getAsString(key).equals(entity.getString(entity.getColumnIndex(key)))) {
						force = true;
						break;
					}
				}
			}

		}

		if(force) {
			mContext.getContentResolver().update(mCursorLoader.getUri(), values, mCursorLoader.getSelection(), mCursorLoader.getSelectionArgs());

			// Wait for the activity to be idle so we know its not processing other loader requests.
			mTester.getInstrumentation().waitForIdleSync();

			// Then wait for the loader
			waitForLoader();
		}
	}

	public Cursor getEntity() {
		return mContext.getContentResolver().query(mCursorLoader.getUri(), null, mCursorLoader.getSelection(), mCursorLoader.getSelectionArgs(), mCursorLoader.getSortOrder());
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
