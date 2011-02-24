package edu.ucla.cens.andwellness.service;

import edu.ucla.cens.andwellness.activity.SurveyListActivity;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TriggerNotificationReceiver extends BroadcastReceiver {
	
	private static final String TAG = "TriggerNotificationReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		
		String action = intent.getAction();

		Log.i(TAG, "Broadcast received: " + action);

		if (TriggerFramework.ACTION_TRIGGER_NOTIFICATION.equals(action)) {
			Intent i = new Intent(context, SurveyListActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // need to fix this so it doesn't start new activity if already on screen
			context.startActivity(i);
		}
	}
}
