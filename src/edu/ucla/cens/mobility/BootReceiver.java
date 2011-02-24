package edu.ucla.cens.mobility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver
{

	public static final String TAG = "BOOT_RECEIVER";

	@Override
	public void onReceive(final Context context, Intent intent)
	{
		Log.i(TAG, "onReceive");
		
		// start components
		SharedPreferences settings = context.getSharedPreferences(Mobility.MOBILITY, Context.MODE_PRIVATE);
		if (settings.getBoolean(MobilityControl.MOBILITY_ON, false))
		{
//			Intent MobilityServiceIntent = new Intent(context, MobilityService.class);
//			context.stopService(MobilityServiceIntent);
//			context.startService(MobilityServiceIntent);
			Mobility.start(context);
		}
		
		
	}

}
