package edu.ucla.cens.andwellness.triggers.glue;

import android.content.Context;
import android.content.Intent;
import edu.ucla.cens.andwellness.triggers.types.location.LocTrigTracingSettActivity;

/*
 * APIs exported from location triggers. This class
 * exports interfaces which can be used by the clients
 * to directly interact with location based triggers 
 * (without going through the framework).
 */
public class LocationTriggerAPI {
	
	/*
	 * Launch the activity with the location tracing settings. 
	 */
	public static void launchLocationTracingSettingsActivity(Context context) {
		
		context.startActivity(new Intent(context, 
							      LocTrigTracingSettActivity.class));
	}
}
