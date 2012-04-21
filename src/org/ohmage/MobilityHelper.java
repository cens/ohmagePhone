package org.ohmage;

import edu.ucla.cens.mobility.glue.IMobility;
import edu.ucla.cens.mobility.glue.MobilityInterface;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class MobilityHelper {

	public static void setUsername(Context context, String username) {
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

}
