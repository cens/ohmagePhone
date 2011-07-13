/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.ucla.cens.andwellness;

import java.io.File;

import android.content.Context;
import android.util.Log;
import edu.ucla.cens.andwellness.db.DbHelper;
import edu.ucla.cens.andwellness.prompt.multichoicecustom.MultiChoiceCustomDbAdapter;
import edu.ucla.cens.andwellness.prompt.photo.PhotoPrompt;
import edu.ucla.cens.andwellness.prompt.singlechoicecustom.SingleChoiceCustomDbAdapter;
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
		SingleChoiceCustomDbAdapter singleChoiceDbAdapter = new SingleChoiceCustomDbAdapter(context); 
		if (singleChoiceDbAdapter.open()) {
			singleChoiceDbAdapter.clearCampaign(urn);
			singleChoiceDbAdapter.close();
		}
		MultiChoiceCustomDbAdapter multiChoiceDbAdapter = new MultiChoiceCustomDbAdapter(context); 
		if (multiChoiceDbAdapter.open()) {
			multiChoiceDbAdapter.clearCampaign(urn);
			multiChoiceDbAdapter.close();
		}
		
		//clear preferences
		
	}
}
