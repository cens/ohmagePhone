
package org.ohmage.db.test;


public abstract class MockArrayCursor<T> extends EmptyMockCursor {

	protected static final int COLUMN_IGNORE = -1;

	private final T[] mObjects;
	private int mPointer = -1;

	private String[] mProjection;

	public MockArrayCursor(String[] projection, T... objects) {
		mProjection = projection;
		mObjects = objects;
	}

	@Override
	public int getCount() {
		return mObjects.length;
	}

	@Override
	public boolean moveToFirst() {
		mPointer = 0;
		return true;
	}

	@Override
	public boolean moveToNext() {
		mPointer++;
		return pointerValid();
	}

	@Override
	public boolean isAfterLast() {
		return mPointer >= getCount();
	}

	@Override
	public boolean moveToPosition(int position) {
		mPointer = position;
		return pointerValid();
	}

	private boolean pointerValid() {
		return mPointer < getCount() && mPointer >= 0;
	}

	public void setProjection(String[] projection) {
		mProjection = projection;
	}

	protected T getObject() {
		if(pointerValid())
			return mObjects[mPointer];
		return null;
	}

	@Override
	public final int getColumnIndex(String columnName) {
		if(mProjection == null)
			return getLocalColumnIndex(columnName);
		for(int i=0;i<mProjection.length;i++)
			if(mProjection[i].equals(columnName))
				return i;
		return COLUMN_IGNORE;
	}

	/**
	 * Gets the column index without a projection
	 * @param columnName
	 * @return
	 */
	protected abstract int getLocalColumnIndex(String columnName);

	/**
	 * If there is a projection set, this function maps the column index to
	 * the index that I expect if there is no projection
	 * @param columnIndex
	 * @return
	 */
	private int mapColumnIndex(int columnIndex) {
		if(mProjection != null)
			return getLocalColumnIndex(mProjection[columnIndex]);
		return columnIndex;
	}

	@Override
	public long getLong(int columnIndex) {
		columnIndex = mapColumnIndex(columnIndex);
		if(getObject() != null) {
			return getLongValid(columnIndex);
		}

		return getLongDefault(columnIndex);
	}

	protected abstract long getLongValid(int columnIndex);

	protected long getLongDefault(int columnIndex) {
		return 0;
	}


	@Override
	public double getDouble(int columnIndex) {
		columnIndex = mapColumnIndex(columnIndex);
		if(getObject() != null)
			return getDoubleValid(columnIndex);

		return getDoubleDefault(columnIndex);
	}

	protected abstract double getDoubleValid(int columnIndex);

	protected double getDoubleDefault(int columnIndex) {
		return 0.0;
	}

	@Override
	public String getString(int columnIndex) {
		columnIndex = mapColumnIndex(columnIndex);
		if(getObject() != null)
			return getStringValid(columnIndex);

		return getStringDefault(columnIndex);
	}

	protected abstract String getStringValid(int columnIndex);

	protected String getStringDefault(int columnIndex) {
		return "";
	}

	@Override
	public float getFloat(int columnIndex) {
		columnIndex = mapColumnIndex(columnIndex);
		if(getObject() != null)
			return getFloatValid(columnIndex);

		return getFloatDefault(columnIndex);
	}

	protected abstract float getFloatValid(int columnIndex);

	protected float getFloatDefault(int columnIndex) {
		return 0.0f;
	}

	@Override
	public int getInt(int columnIndex) {
		columnIndex = mapColumnIndex(columnIndex);
		if(getObject() != null)
			return getIntValid(columnIndex);

		return getIntDefault(columnIndex);
	}

	protected abstract int getIntValid(int columnIndex);

	protected int getIntDefault(int columnIndex) {
		return 0;
	}
}
