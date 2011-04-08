package edu.ucla.cens.andwellness;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import edu.ucla.cens.systemlog.Log;

import android.content.Context;
import android.os.Environment;

public class CampaignXmlHelper {
	
	private static final String TAG = "CampaignXmlHelper";
	
	//public static final String DEFAULT_CAMPAIGN_SOURCE = "file";
	public static final String DEFAULT_CAMPAIGN_SOURCE = "resource";
	public static final int CAMPAIGN_XML_RESOURCE_ID = R.raw.nih_all;
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
}
