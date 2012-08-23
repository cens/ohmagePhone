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
package edu.ucla.cens.systemlog;

import org.ohmage.Config;

import android.content.Context;
import android.content.Intent;

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

	public enum Loglevel {
		ERROR,
		WARNING,
		INFO,
		DEBUG,
		VERBOSE
	}

	private static String mAppName = DEFAULT_APP_NAME;
	private static Context mContext = null;
	private static boolean mPackageInstalled;

	public static void initialize(Context context, String appName) {
		mContext = context.getApplicationContext();
		mAppName = appName;
	}

	/**
	 * Checks to see if we want to log this level of messages
	 * @param logLevel
	 * @return
	 */
	private static boolean shouldLogMessage(Loglevel logLevel) {
		try {
			return logLevel.compareTo(Loglevel.valueOf(Config.LOG_LEVEL.toUpperCase())) <= 0;
		} catch(IllegalArgumentException e) {
			return false;
		}
	}
	
	private static boolean logMessage(Loglevel logLevel, String tag, String msg) {
		return logMessage(logLevel, tag, msg, false);
	}

	private static boolean logMessage(Loglevel logLevel, String tag, String msg, boolean force) {
		if(mContext == null || mAppName == null) {
			android.util.Log.e(TAG, "SystemLog not initialized");
			return false;
		}

		// If we aren't logging these messages, return
		if(!force && !shouldLogMessage(logLevel))
			return false;

		Intent i = new Intent(ACTION_LOG_MESSAGE);

		i.putExtra(KEY_LOG_LEVEL, logLevel.toString().toLowerCase());
		i.putExtra(KEY_APP_NAME, mAppName);
		i.putExtra(KEY_TAG, tag);
		i.putExtra(KEY_MSG, msg);

		if (mContext.startService(i) == null ) {
			return false;
		}

		return true;
	}

	public static void i(String tag, String message) {

		if(!logMessage(Loglevel.INFO, tag, message)) {
			android.util.Log.i(tag, message);
		}
	}

	public static void d(String tag, String message) {

		if(!logMessage(Loglevel.DEBUG, tag, message)) {
			android.util.Log.d(tag, message);
		}
	}

	public static void e(String tag, String message, Exception e) {

		if(!logMessage(Loglevel.ERROR, tag, message + e.getMessage())) {
			android.util.Log.e(tag, message, e);
		}
	}

	public static void e(String tag, String message) {

		if(!logMessage(Loglevel.ERROR, tag, message)) {
			android.util.Log.e(tag, message);
		}
	}

    public static void e(String tag, String msg, Throwable tr) {
        e(tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
    }

	public static void v(String tag, String message) {

		if(!logMessage(Loglevel.VERBOSE, tag, message)) {
			android.util.Log.v(tag, message);
		}
	}

	public static void w(String tag, String message) {

		if(!logMessage(Loglevel.WARNING, tag, message)) {
			android.util.Log.w(tag, message);
		}
	}

	public static void analytic(String tag, String message) {

		if(Config.LOG_ANALYTICS) {
			if(!logMessage(Loglevel.INFO, tag, message, true)) {
				android.util.Log.v(tag, message);
			}
		}
	}
}
