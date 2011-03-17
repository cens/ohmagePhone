package edu.ucla.cens.systemlog;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;



public class Log {
	
	private static final String TAG = "LOG";
	
	private static final String DEFAULT_APP_NAME = "default";

	private static String ACTION_LOG_MESSAGE = 
			"edu.ucla.cens.systemlog.log_message";
  
	private static final String KEY_TAG = 
			"edu.ucla.cens.systemlog.key_tag";
	private static final String KEY_MSG = 
			"edu.ucla.cens.systemlog.key_msg";
	private static final String KEY_APP_NAME = 
			"edu.ucla.cens.systemlog.key_app_name";
	private static final String KEY_LOG_LEVEL = 
			"edu.ucla.cens.systemlog.key_log_level";
	
    private static final String ERROR_LOGLEVEL = "error";
    private static final String WARNING_LOGLEVEL = "warning";
    private static final String INFO_LOGLEVEL = "info";
    private static final String DEBUG_LOGLEVEL = "debug";
    private static final String VERBOSE_LOGLEVEL = "verbose";
	
    private static String mAppName = DEFAULT_APP_NAME;
    private static Context mContext = null;
    private static boolean mPackageInstalled;

    public static void initialize(Context context, String appName) {
    	mContext = context.getApplicationContext();
    	mAppName = appName;
    	
    	try {
			mContext.getPackageManager().getPackageInfo("edu.ucla.cens.systemlog", 0);
			mPackageInstalled = true;
		} catch (NameNotFoundException e) {
			android.util.Log.e(TAG, "SystemLog not installed");
			mPackageInstalled = false;
		}
    }
    
    private static boolean logMessage(String logLevel, String tag, String msg) {
    	if(mContext == null || mAppName == null) {
    		android.util.Log.e(TAG, "SystemLog not initialized");
    		return false;
    	}
    	
    	//if systemlog is uninstalled after the initialize call, no logcat logging will happen
    	if (!mPackageInstalled) {
    		try {
    			mContext.getPackageManager().getPackageInfo("edu.ucla.cens.systemlog", 0);
    			mPackageInstalled = true;
    		} catch (NameNotFoundException e) {
    			mPackageInstalled = false;
    			return false;
    		}
    	}
    	
    	Intent i = new Intent(ACTION_LOG_MESSAGE);

		i.putExtra(KEY_LOG_LEVEL, logLevel);
		i.putExtra(KEY_APP_NAME, mAppName);
		i.putExtra(KEY_TAG, tag);
		i.putExtra(KEY_MSG, msg);
		
		mContext.startService(i);

    	return true;
    }
	
    public static void i(String tag, String message) {
    	
    	if(!logMessage(INFO_LOGLEVEL, tag, message)) {
    		android.util.Log.i(tag, message);
    	}
    }
    
    public static void d(String tag, String message) {
    	
    	if(!logMessage(DEBUG_LOGLEVEL, tag, message)) {
    		android.util.Log.d(tag, message);
    	}
    }
    
    public static void e(String tag, String message, Exception e) {
    	
    	if(!logMessage(ERROR_LOGLEVEL, tag, message + e.getMessage())) {
    		 android.util.Log.e(tag, message, e);
    	}
    }

    public static void e(String tag, String message) {
    	
    	if(!logMessage(ERROR_LOGLEVEL, tag, message)) {
    		android.util.Log.e(tag, message);
    	}
    }

    public static void v(String tag, String message) {
    	
    	if(!logMessage(VERBOSE_LOGLEVEL, tag, message)) {
    		android.util.Log.v(tag, message);
    	}
    }

    public static void w(String tag, String message) {
    	
    	if(!logMessage(WARNING_LOGLEVEL, tag, message)) {
    		android.util.Log.w(tag, message);
    	}
    }
}
