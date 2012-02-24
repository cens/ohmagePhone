
package org.ohmage.db.test;

import org.ohmage.db.DbContract;

import android.app.Application;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;

import java.util.ArrayList;

public class DelegatingMockContentProvider extends MockContentProvider {

	private final ContentProviderClient mProvider;

	public DelegatingMockContentProvider(Application application, String name) {
		mProvider = application.getContentResolver().acquireContentProviderClient(name);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		try {
			return mProvider.delete(uri, selection, selectionArgs);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		try {
			return mProvider.getType(uri);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		try {
			return mProvider.insert(uri, values);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		try {
			return mProvider.query(uri, projection, selection, selectionArgs, sortOrder);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		try {
			return mProvider.update(uri, values, selection, selectionArgs);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * If you're reluctant to implement this manually, please just call
	 * super.bulkInsert().
	 */
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		try {
			mProvider.bulkInsert(uri, values);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) {
		try {
			return mProvider.applyBatch(operations);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void addToContentResolver(MockContentResolver resolver) {
		resolver.addProvider(DbContract.CONTENT_AUTHORITY, this);
	}
}
