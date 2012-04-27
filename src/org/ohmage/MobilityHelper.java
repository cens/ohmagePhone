package org.ohmage;

import edu.ucla.cens.mobility.glue.IMobility;
import edu.ucla.cens.mobility.glue.MobilityInterface;

import org.ohmage.async.MobilityAggregateReadTask;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

public class MobilityHelper {

	/** The version number of the mobility release which has the username column **/
	public static final int VERSION_AGGREGATE_AND_USERNAME = 25;

	/** The version of mobility we require to show feedback **/
	public static final int REQUIRED_MOBILITY_VERSION = 25;

	private static final String TAG = "MobilityHelper";

	public static void setUsername(Context context, String username) {
		setUsername(context, username, -1);
	}

	public static void setUsername(Context context, String username, long loginTimestamp) {
		// If we are using an old version of mobility we can't set the username
		if(getMobilityVersion(context) < VERSION_AGGREGATE_AND_USERNAME)
			return;

		if(TextUtils.isEmpty(username)) {
			username = null;
		} else {
			username = getMobilityUsername(username);
		}
		Intent mobilityIntent = new Intent(IMobility.class.getName());
		mobilityIntent.setAction(MobilityInterface.ACTION_SET_USERNAME);
		mobilityIntent.putExtra(MobilityInterface.EXTRA_USERNAME, username);
		mobilityIntent.putExtra(MobilityInterface.EXTRA_BACKDATE, loginTimestamp);
		context.startService(mobilityIntent);
	}


	public static void recalculateAggregate(Context context, String username, Long startDate) {
		// If we are using an old version of mobility we can't calculate aggregates
		if(getMobilityVersion(context) < VERSION_AGGREGATE_AND_USERNAME)
			return;

		if(username == null)
			throw new IllegalArgumentException("Must specifiy username");

		Intent mobilityIntent = new Intent(IMobility.class.getName());
		mobilityIntent.setAction(MobilityInterface.ACTION_RECALCULATE_AGGREGATES);
		mobilityIntent.putExtra(MobilityInterface.EXTRA_USERNAME, getMobilityUsername(username));
		mobilityIntent.putExtra(MobilityInterface.EXTRA_BACKDATE, startDate);
		context.startService(mobilityIntent);
	}

	public static String getMobilityUsername(String username) {
		return "ohmage_"+username.hashCode();
	}

	/**
	 * Returns true if mobility and accelservice are installed
	 * @param context
	 * @return
	 */
	public static boolean isMobilityInstalled(Context context) {
		return getMobilityPackageInfo(context) != null;
	}

	/**
	 * Returns true if the mobility is newer than the minimum required version
	 * @param context
	 * @return
	 */
	public static boolean isMobilityVersionCorrect(Context context) {
		PackageInfo info = getMobilityPackageInfo(context);
		if(info != null)
			return info.versionCode >= REQUIRED_MOBILITY_VERSION;
			return false;
	}

	/**
	 * Returns the version of mobility if it exists, -1 otherwise
	 * @param context
	 * @return
	 */
	public static int getMobilityVersion(Context context) {
		PackageInfo info = getMobilityPackageInfo(context);
		if(info != null)
			return info.versionCode;
		return -1;
	}

	/**
	 * Returns the mobility package info if mobiliy and accelservice exist
	 * @param context
	 * @return the mobility package info
	 */
	private static PackageInfo getMobilityPackageInfo(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo("edu.ucla.cens.mobility", 0 );
			PackageInfo info2 = context.getPackageManager().getPackageInfo("edu.ucla.cens.accelservice", 0 );

			if (info != null && info2 != null) {
				return info;
			}
		} catch( PackageManager.NameNotFoundException e ) {
			// Don't do anything
		}
		return null;
	}


	/**
	 * Upgrades the data for mobility
	 * VERSION_AGGREGATE_AND_USERNAME queries the server and asks for aggregate data and sets the username with mobility
	 * @param context
	 * @return int version that mobility was upgraded to
	 */
	public static int upgradeMobilityData(Context context) {
		SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);

		// The user needs to be logged in before we can upgrade mobility data
		if(!prefs.isAuthenticated())
			return -1;

		int oldMobilityVersion = prefs.getLastMobilityVersion();
		int newMobilityVersion = getMobilityVersion(context);
		prefs.setMobilityVersion(newMobilityVersion);

		if(oldMobilityVersion < VERSION_AGGREGATE_AND_USERNAME && newMobilityVersion >= VERSION_AGGREGATE_AND_USERNAME) {
			setUsername(context, prefs.getUsername(), prefs.getLoginTimestamp());

			// If we are upgrading into a version of ohmage which has aggregates,
			// we should make a call to the server to get the aggregate data
			MobilityAggregateReadTask task = new MobilityAggregateReadTask(context);
			task.setCredentials();
			task.forceLoad();
			return newMobilityVersion;
		}

		return -1;
	}
}
