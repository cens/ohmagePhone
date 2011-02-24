package edu.ucla.cens.andwellness.triggers.types.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LocTrigTimeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent i) {
		if(i.getAction().equals(Intent.ACTION_TIME_CHANGED) || 
		   i.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {

			//TODO enable this later
			
//			LocTrigDB db = new LocTrigDB(context);
//			db.open();
//			db.removeAllCategoryTimeStamps();
//			db.close();
		}
	}
}
