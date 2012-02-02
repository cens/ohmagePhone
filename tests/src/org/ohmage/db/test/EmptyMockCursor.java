package org.ohmage.db.test;

import android.database.ContentObserver;
import android.test.mock.MockCursor;

public class EmptyMockCursor extends MockCursor {

	@Override
	public int getCount() {
		return 0;
	}

	@Override
	public boolean moveToNext() {
		return false;
	}

	@Override
	public boolean moveToFirst() {
		return false;
	}

	@Override
	public boolean isAfterLast() {
		return true;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void close() {
		return;
	}

	@Override
	public void registerContentObserver(ContentObserver observer) {
	}

	@Override
	public int getColumnIndex(String columnName) {
		return 0;
	}

	@Override
	public int getColumnIndexOrThrow(String columnName) {
		return getColumnIndex(columnName);
	}
}