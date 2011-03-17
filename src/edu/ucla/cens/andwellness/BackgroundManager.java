package edu.ucla.cens.andwellness;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import edu.ucla.cens.andwellness.prompts.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.service.UploadReceiver;
import edu.ucla.cens.andwellness.storagemonitor.StorageMonitorService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.os.SystemClock;
//import android.util.Log;
import edu.ucla.cens.systemlog.Log;
import android.widget.Toast;

public class BackgroundManager {
	
	private static final String TAG = "BACKGROUND_MANAGER";

	public static void initComponents(Context context) {
		
		Log.i(TAG, "initializing application components");
		
		Context appContext = context.getApplicationContext();
		
		new File(PhotoPrompt.IMAGE_PATH).mkdirs();
		
		boolean parsedCampaign = false;
		
		SharedPreferencesHelper prefs = new SharedPreferencesHelper(appContext);
		try {
			Map<String, String> map = PromptXmlParser.parseCampaignInfo(appContext.getResources().openRawResource(SharedPreferencesHelper.CAMPAIGN_XML_RESOURCE_ID));
			if (map.containsKey("campaign_name") && map.containsKey("campaign_version")) {
				prefs.putCampaignName(map.get("campaign_name"));
				prefs.putCampaignVersion(map.get("campaign_version"));
				parsedCampaign = true;
			} 
			if (map.containsKey("server_url")) {
				prefs.putServerUrl(map.get("server_url"));
			}
		} catch (NotFoundException e) {
			Log.e(TAG, "Error parsing xml for campaign info", e);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Error parsing xml for campaign info", e);
		} catch (IOException e) {
			Log.e(TAG, "Error parsing xml for campaign info", e);
		}
		
		if (!parsedCampaign) {
			Log.e(TAG, "Unable to set campaign name and version!");
			Toast.makeText(appContext, "Unable to set campaign name and version!", Toast.LENGTH_LONG).show();
		}
		
		//uploadservice
		AlarmManager alarms = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);
		Intent intentToFire = new Intent(UploadReceiver.ACTION_UPLOAD_ALARM);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intentToFire, 0);
		//alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), pendingIntent);
		alarms.cancel(pendingIntent);
		alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_HOUR, pendingIntent);
		Log.i(TAG, "UploadReceiver repeating alarm set");
		
		//storagemonitor
		appContext.startService(new Intent(appContext, StorageMonitorService.class));
		Log.i(TAG, "started storage monitor service");
		
		
	}
}
