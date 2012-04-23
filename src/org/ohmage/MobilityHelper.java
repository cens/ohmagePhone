package org.ohmage;

import edu.ucla.cens.mobility.glue.IMobility;
import edu.ucla.cens.mobility.glue.MobilityInterface;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

public class MobilityHelper {

	/** The version number of the mobility release which has the aggregate table **/
	public static final int VERSION_AGGREGATES = 25;

	/** The version number of the mobility release which has the username column **/
	public static final int VERSION_USERNAME = 26;

	/** The version of mobility we require to show feedback **/
	public static final int REQUIRED_MOBILITY_VERSION = 26;

	public static void setUsername(Context context, String username) {
		// If we are using an old version of mobility we can't set the username
		if(getMobilityVersion(context) < 26)
			return;

		if(TextUtils.isEmpty(username)) {
			username = null;
		} else {
			username = getMobilityUsername(username);
		}
		Intent mobilityIntent = new Intent(IMobility.class.getName());
		mobilityIntent.setAction(MobilityInterface.ACTION_SET_USERNAME);
		mobilityIntent.putExtra(MobilityInterface.EXTRA_USERNAME, username);
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
}
