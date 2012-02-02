package org.ohmage.db.test;

import android.database.ContentObserver;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.mock.MockContentResolver;

public class NotifyingMockContentResolver  extends MockContentResolver {

	private final ActivityInstrumentationTestCase2<? extends FragmentActivity> mTest;
	private final int[] mLoaderIds;

	public NotifyingMockContentResolver(ActivityInstrumentationTestCase2<? extends FragmentActivity> test, int... loaderIds) {
		mTest = test;
		mLoaderIds = loaderIds;
	}

	public NotifyingMockContentResolver(ActivityInstrumentationTestCase2<? extends FragmentActivity> test) {
		this(test, new int[] { 0 });
	}

	@Override
	public void notifyChange(Uri uri, ContentObserver observer, boolean syncToNetwork) {
		for(int id : mLoaderIds)
			mTest.getActivity().getSupportLoaderManager().getLoader(id).onContentChanged();
	}
}