package org.ohmage.db.test;

import org.ohmage.db.DbContract;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;

public class MockContentProviderContext extends RenamingDelegatingContext {
	private final Context mContext;

	public MockContentProviderContext(Context context) {
		super(context, "tmp");
		mContext = context;
	}

	private MockContentResolver mResolver;

	@Override
	public ContentResolver getContentResolver() {
		if(mResolver == null) {

			mResolver = new MockContentResolver();

			mResolver.addProvider(DbContract.CONTENT_AUTHORITY, new MockContentProvider(mContext) {

				@Override
				public int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
					return MockContentProviderContext.this.update(uri, values, where, selectionArgs);
				}

				@Override
				public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
					return MockContentProviderContext.this.query(uri, projection, selection, selectionArgs, sortOrder);
				}
			});
		}
		return mResolver;
	}

	protected int update(Uri uri, ContentValues values, String where, String[] selectionArgs) {
		return 0;
	}

	protected Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return new EmptyMockCursor();
	}
}