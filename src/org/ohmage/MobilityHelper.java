
package org.ohmage;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import org.ohmage.async.MobilityAggregateReadTask;
import org.ohmage.mobility.glue.IMobility;
import org.ohmage.mobility.glue.MobilityInterface;

public class MobilityHelper {

    private static final String TAG = "MobilityHelper";

    public static void setUsername(Context context, String username) {
        setUsername(context, username, -1);
    }

    public static void setUsername(Context context, String username, long loginTimestamp) {
        if (TextUtils.isEmpty(username)) {
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
        // If we are using an old version of mobility we can't calculate
        // aggregates
        if (username == null)
            throw new IllegalArgumentException("Must specifiy username");

        Intent mobilityIntent = new Intent(IMobility.class.getName());
        mobilityIntent.setAction(MobilityInterface.ACTION_RECALCULATE_AGGREGATES);
        mobilityIntent.putExtra(MobilityInterface.EXTRA_USERNAME, getMobilityUsername(username));
        mobilityIntent.putExtra(MobilityInterface.EXTRA_BACKDATE, startDate);
        context.startService(mobilityIntent);
    }

    public static String getMobilityUsername(String username) {
        return "ohmage_" + username.hashCode();
    }

    /**
     * Returns true if mobility and accelservice are installed
     * 
     * @param context
     * @return
     */
    public static boolean isMobilityInstalled(Context context) {
        return getMobilityPackageInfo(context) != null;
    }

    /**
     * Returns the version of mobility if it exists, -1 otherwise
     * 
     * @param context
     * @return
     */
    public static int getMobilityVersion(Context context) {
        PackageInfo info = getMobilityPackageInfo(context);
        if (info != null)
            return info.versionCode;
        return -1;
    }

    /**
     * Returns the mobility package info if mobiliy and accelservice exist
     * 
     * @param context
     * @return the mobility package info
     */
    private static PackageInfo getMobilityPackageInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getPackageInfo("org.ohmage.mobility", 0);
        } catch (PackageManager.NameNotFoundException e) {
            // Don't do anything
        }
        return null;
    }

    /**
     * Upgrades the data for mobility VERSION_AGGREGATE_AND_USERNAME queries the
     * server and asks for aggregate data and sets the username with mobility
     * 
     * @param context
     * @return int version that mobility was upgraded to
     */
    public static int ensureMobilityData(Context context) {
        UserPreferencesHelper prefs = new UserPreferencesHelper(context);

        // The user needs to be logged in before we can upgrade mobility data
        if (!prefs.isAuthenticated())
            return -1;
        setUsername(context, prefs.getUsername(), prefs.getLoginTimestamp());

        ConfigHelper appPrefs = new ConfigHelper(context);

        int oldMobilityVersion = appPrefs.getLastMobilityVersion();
        int newMobilityVersion = getMobilityVersion(context);
        appPrefs.setMobilityVersion(newMobilityVersion);

        if (oldMobilityVersion == -1 && isMobilityInstalled(context)) {

            // If we are upgrading into a version of ohmage which has
            // aggregates,
            // we should make a call to the server to get the aggregate data
            MobilityAggregateReadTask task = new MobilityAggregateReadTask(context);
            task.setCredentials();
            task.forceLoad();
            return newMobilityVersion;
        }

        return -1;
    }

    /**
     * Starts downloading aggregate data if mobility is new enough
     * 
     * @param context
     */
    public static void downloadAggregate(Context context) {
        MobilityAggregateReadTask task = new MobilityAggregateReadTask(context);
        task.setCredentials();
        task.forceLoad();
    }
}
