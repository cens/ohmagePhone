
package org.ohmage;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class MobilityHelper {

    private static final String TAG = "MobilityHelper";

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
}
