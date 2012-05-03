package org.ohmage.loader;

import edu.ucla.cens.mobility.glue.MobilityInterface;

import org.ohmage.MobilityHelper;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

public class MobilityAggregateLoader extends CursorLoader {

	private long mTimeActive = 0;

	/**
	 * Calculates the average aggregate time for this user
	 * @param context
	 * @param startTime inclusive
	 * @param endTime inclusive
	 * @param username
	 */
	public MobilityAggregateLoader(Context context, long startTime, long endTime, String username) {
		super(context, MobilityInterface.AGGREGATES_URI,
				new String[] { MobilityInterface.KEY_MODE, "AVG(" + MobilityInterface.KEY_DURATION + ")", MobilityInterface.KEY_DAY },
				MobilityInterface.KEY_DAY + " >= date('" + startTime/1000 + "', 'unixepoch', 'localtime')"
						+ " AND " + MobilityInterface.KEY_DAY + " <= date('" + endTime/1000 + "', 'unixepoch', 'localtime')"
						+ " AND " + MobilityInterface.KEY_USERNAME + "=?", new String[] {
				MobilityHelper.getMobilityUsername(username)
		}, MobilityInterface.KEY_DAY + " DESC");
	}

	@Override
	public Cursor loadInBackground() {
		mTimeActive = 0;

		Cursor data = super.loadInBackground();
		if (data != null) {
			while(data.moveToNext()) {
				String mode = data.getString(0);
				if(MobilityInterface.WALK.equals(mode) || MobilityInterface.RUN.equals(mode) || MobilityInterface.BIKE.equals(mode))
					mTimeActive += data.getLong(1);
			}
		}
		return data;
	}

	@Override
	public void onReset() {
		super.onReset();
		mTimeActive = 0;
	}

	public long getTimeActive() {
		return mTimeActive ;
	}
}