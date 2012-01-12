package org.ohmage;

import org.ohmage.activity.LoginActivity;
import org.ohmage.activity.MobilityActivity;
import org.ohmage.activity.UploadQueueActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationHelper {

	public static void showAuthNotification(Context context) {
		NotificationManager noteManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification note = new Notification();
		
		Intent intentToLaunch = new Intent(context, LoginActivity.class);
		intentToLaunch.putExtra(LoginActivity.EXTRA_UPDATE_CREDENTIALS, true);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentToLaunch, 0);
		String title = "Authentication error!";
		String body = "Tap here to re-enter credentials.";
		note.icon = android.R.drawable.stat_notify_error;
		note.tickerText = "Authentication error!";
		note.defaults |= Notification.DEFAULT_ALL;
		note.when = System.currentTimeMillis();
		note.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
		note.setLatestEventInfo(context, title, body, pendingIntent);
		noteManager.notify(1, note);
	}

	public static void hideAuthNotification(Context context) {
		NotificationManager notifMan = (NotificationManager)context.getSystemService(
										Context.NOTIFICATION_SERVICE);
		notifMan.cancel(1);
	}

	public static void showUploadErrorNotification(Context context) {
		NotificationManager noteManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification note = new Notification();
		
		Intent intentToLaunch = new Intent(context, UploadQueueActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentToLaunch, 0);
		String title = "Upload error!";
		String body = "An error occurred while trying to upload survey responses.";
		note.icon = android.R.drawable.stat_notify_error;
		note.tickerText = "Upload error!";
		note.defaults |= Notification.DEFAULT_ALL;
		note.when = System.currentTimeMillis();
		note.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
		note.setLatestEventInfo(context, title, body, pendingIntent);
		noteManager.notify(2, note);
	}
	
	public static void showMobilityErrorNotification(Context context) {
		NotificationManager noteManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification note = new Notification();
		
		Intent intentToLaunch = new Intent(context, MobilityActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentToLaunch, 0);
		String title = "Mobility upload error!";
		String body = "An error occurred while trying to upload mobility data points.";
		note.icon = android.R.drawable.stat_notify_error;
		note.tickerText = "Mobility upload error!";
		note.defaults |= Notification.DEFAULT_ALL;
		note.when = System.currentTimeMillis();
		note.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
		note.setLatestEventInfo(context, title, body, pendingIntent);
		noteManager.notify(3, note);
	}

	public static void showNotification(Context context, String title, String message) {
		NotificationManager noteManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification note = new Notification();

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
		note.icon = android.R.drawable.stat_notify_error;
		note.tickerText = title;
		note.defaults |= Notification.DEFAULT_ALL;
		note.when = System.currentTimeMillis();
		note.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
		note.setLatestEventInfo(context, title, message, pendingIntent);
		noteManager.notify(4, note);
	}
}
