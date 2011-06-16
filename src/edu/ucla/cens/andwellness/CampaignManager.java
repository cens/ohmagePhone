package edu.ucla.cens.andwellness;

import java.io.File;

import android.content.Context;
import android.util.Log;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.triggers.glue.TriggerFramework;

public class CampaignManager {
	
	private static final String TAG = "CampaignManager";

	public static void removeCampaign(Context context, String urn) {
		DbHelper dbHelper = new DbHelper(context);
		
		//remove triggers
		TriggerFramework.resetTriggerSettings(context, urn);
		
		//remove campaign
		dbHelper.removeCampaign(urn);
		
		//remove responses
		dbHelper.removeResponseRows(urn);
		
		//remove images
		File imageDir = new File(PhotoPrompt.IMAGE_PATH + "/" + urn.replace(':', '_'));
		if (imageDir.exists()) {
			File [] files = imageDir.listFiles();
			
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					files[i].delete();
				}
			}
			
			imageDir.delete();
		} else {
			Log.e(TAG, PhotoPrompt.IMAGE_PATH + "/" + urn.replace(':', '_') + " does not exist.");
		}
		
		//clear custom type dbs
		
		//clear preferences
	}
}
