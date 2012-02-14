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

import org.apache.http.client.methods.HttpPost;
import org.ohmage.OhmageApi.Result;
import org.ohmage.OhmageApplication;
import org.ohmage.SharedPreferencesHelper;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import java.net.URI;

/**
 * We use system log to log analytics of an android app. System log
 * automatically logs the timestamp and the imei so I ignore collecting those.
 * <p>
 * there are 4 types of messages that we will collect which are described by the
 * following CFG.
 * </p>
 * <p>
 * Message -> ActivityMessage | WidgetMessage | ServiceMessage | NetworkMessage
 * </p>
 * <p>
 * ActivityMessage -> analytics_activity activity_name Status<br />
 * WidgetMessage -> analytics_widget activity_name widget_id widget_description
 * <br />
 * ServiceMessage -> analytics_service service_name Status<br />
 * NetworkMessage -> analytics_network context resource_url NetworkStatus
 * amount<br />
 * </p>
 * <p>
 * Status -> on | off<br />
 * NetworkStatus -> upload | download<br />
 * </p>
 * <p>
 * For brevity I have omitted some rules and added some variables which are
 * limited by the application. The variables are activity_name, widget_id,
 * widget_description, service_name, resource_url, and amount. Everything that
 * is lowercase except for the variables are terminals. Everything that is
 * uppercase is nonterminal.
 * </p>
 * 
 * @author cketcham
 */
public class Analytics {

	/**
	 * Log information about if an activity is being shown or hidden. This call
	 * should be made from {@link Activity#onPause()} and
	 * {@link Activity#onResume()}
	 * 
	 * @param activity
	 * @param status
	 */
	public static void activity(Activity activity, final Status status) {
		log(activity, "activity", status.toString());
	}

	/**
	 * Log information about a view being interacted with
	 * 
	 * @param view
	 * @param name Human readable name for widget
	 */
	public static void widget(View view, String name) {
		StringBuilder builder = new StringBuilder();
		builder.append(view.getId());
		if (name != null)
			builder.append(" ").append(name);
		else if (!TextUtils.isEmpty(view.getContentDescription()))
			builder.append(" ").append(view.getContentDescription());
		log(view.getContext(), "widget", builder);
	}

	/**
	 * For the case that we want to log some widget action but don't have access
	 * to the view
	 * 
	 * @param context
	 * @param string
	 */
	public static void widget(Context context, String string) {
		StringBuilder builder = new StringBuilder();
		builder.append("-1");
		if (string != null)
			builder.append(" ").append(string);
		log(context, "widget", builder);
	}

	/**
	 * Log information about a view being interacted with
	 * 
	 * @param view
	 */
	public static void widget(View view) {
		widget(view, null);
	}

	/**
	 * Log information about if a service is being started or stopped. This call
	 * should be made from {@link Service#onCreate()} and
	 * {@link Service#onDestroy()}
	 * 
	 * @param service
	 * @param status
	 */
	public static void service(Service service, Status status) {
		log(service, "service", status.toString());
	}

	/**
	 * Log network traffic
	 * 
	 * @param context
	 * @param resource
	 * @param networkState
	 * @param length
	 */
	private static void network(Context context, String resource, String networkState, long length) {
		log(context, "network", new StringBuilder().append(resource).append(" ")
				.append(networkState).append(" ")
				.append(length));
	}

	/**
	 * Log information about network traffic uploads
	 * 
	 * @param context
	 * @param httpPost
	 */
	public static void network(Context context, HttpPost httpPost) {
		network(context, httpPost.getURI().getPath(), "upload", httpPost.getEntity()
				.getContentLength());
	}

	/**
	 * Log information about network traffic downloads
	 * 
	 * @param context
	 * @param url
	 * @param length
	 */
	public static void network(Context context, String url, long length) {
		network(context, URI.create(url).getPath(), "download", length);
	}

	/**
	 * Log network errors
	 * 
	 * @param context
	 * @param url
	 * @param result
	 */
	public static void network(Context context, String url, Result result) {
		if (result != Result.SUCCESS) {
			log(context, "network",
					new StringBuilder().append(URI.create(url).getPath()).append(" ")
							.append("error").append(" ")
							.append(result));
		}
	}

	/**
	 * Creates the entire log message including the tag and preamble
	 * 
	 * @param context
	 * @param tag
	 * @param message
	 */
	protected static void log(Context context, String tag, StringBuilder message) {
		log(context, tag, message.toString());
	}

	/**
	 * Creates the entire log message including the tag and preamble
	 * 
	 * @param context
	 * @param tag
	 * @param message
	 */
	protected static void log(Context context, String tag, String message) {
		Log.i("analytics_" + tag, preamble(context).append(message).toString());
	}

	/**
	 * Appends the beginning of the log message.
	 * <p>
	 * 'login-name class-name'
	 * </p>
	 * 
	 * @param context
	 * @param builder
	 * @return
	 */
	private static StringBuilder appendPreamble(Context context, StringBuilder builder) {
		return builder.append(loginName()).append(" ")
				.append(context.getClass().getSimpleName()).append(" ");
	}

	/**
	 * Creates the beginning of the log message
	 * 
	 * @param context
	 * @return
	 * @see #appendPreamble(Context, StringBuilder)
	 */
	protected static StringBuilder preamble(Context context) {
		return appendPreamble(context, new StringBuilder());
	}

	/**
	 * Retrieve the login name
	 * 
	 * @return
	 */
	private static String loginName() {
		SharedPreferencesHelper helper = new SharedPreferencesHelper(OhmageApplication.getContext());
		return helper.getUsername();
	}

	/**
	 * Used as the ON/OFF indicator for activity messages
	 * 
	 * @author cketcham
	 */
	public enum Status {
		ON,
		OFF
	}
}
