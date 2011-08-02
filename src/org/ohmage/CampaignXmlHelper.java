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
package org.ohmage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.ohmage.db.Campaign;
import org.ohmage.db.DbHelper;

import android.content.Context;
import android.os.Environment;
import org.ohmage.R;
import edu.ucla.cens.systemlog.Log;

public class CampaignXmlHelper {
	
	private static final String TAG = "CampaignXmlHelper";
	
	//public static final String DEFAULT_CAMPAIGN_SOURCE = "file";
	public static final String DEFAULT_CAMPAIGN_SOURCE = "resource";
	public static final int CAMPAIGN_XML_RESOURCE_ID = R.raw.advertisement;
	public static final String CAMPAIGN_XML_FILE_NAME = "campaign.xml";
	
	public static InputStream loadDefaultCampaign(Context context) {
		if ("file".equals(DEFAULT_CAMPAIGN_SOURCE)) {
			String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + CAMPAIGN_XML_FILE_NAME;
			return loadCampaignXmlFromSD(context, path);
		} else {
			return loadCampaignXmlFromResource(context, CAMPAIGN_XML_RESOURCE_ID);
		}
	}

	public static InputStream loadCampaignXmlFromSD(Context context, String path) {
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(new File(path)));
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Unable to open file: " + path, e);
		}
		return is;
	}
	
	public static InputStream loadCampaignXmlFromResource(Context context, int resourceId) {
		
		return context.getResources().openRawResource(resourceId);
	}	
	
	public static InputStream loadCampaignXmlFromDb(Context context, String campaignUrn) throws IOException {
		
		DbHelper dbHelper = new DbHelper(context);
		Campaign campaign = dbHelper.getCampaign(campaignUrn);
		
		return new ByteArrayInputStream(campaign.mXml.getBytes("UTF-8"));
	}
}
