
package org.ohmage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.mobilizingcs.R;

import org.ohmage.activity.LoginActivity;
import org.ohmage.activity.MobilityActivity;
import org.ohmage.activity.UploadQueueActivity;

public class NotificationHelper {

    private static final int AUTH_ERROR_ID = 0;
    private static final int NOTIFICATION_ID = 1;
    private static final int UPLOAD_ERROR_ID = 2;

    public static void showAuthNotification(Context context, String username) {
        showNotification(context, AUTH_ERROR_ID, "Authentication error!",
                "Tap here to re-enter credentials.",
                new Intent(context, LoginActivity.class).putExtra(LoginActivity.PARAM_USERNAME,
                        username));
    }

    public static void hideAuthNotification(Context context) {
        hideNotification(context, AUTH_ERROR_ID);
    }

    public static void showGeneralNotification(Context context, String title, String message,
            Intent intent) {
        showNotification(context, NOTIFICATION_ID, title, message, intent);
    }

    public static void showUploadErrorNotification(Context context) {
        showNotification(context, UPLOAD_ERROR_ID, "Upload error!",
                "An error occurred while trying to upload survey responses.", new Intent(context,
                        UploadQueueActivity.class));
    }

    public static void hideUploadErrorNotification(Context context) {
        hideNotification(context, UPLOAD_ERROR_ID);
    }

    public static void showProbeUploadErrorNotification(Context context, String probe) {
        showNotification(context, probe.hashCode(), "Probe upload error!",
                "Error uploading probes: " + probe, new Intent(context, MobilityActivity.class));
    }

    public static void showResponseUploadErrorNotification(Context context, String response) {
        showNotification(context, response.hashCode(), "Response upload error!",
                "Error uploading responses: " + response, new Intent(context,
                        MobilityActivity.class));
    }

    private static void showNotification(Context context, int id, String title, String message,
            Intent intent) {
        NotificationManager noteManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification note = new Notification();

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        note.icon = R.drawable.ic_stat_warning;
        note.tickerText = title;
        note.defaults |= Notification.DEFAULT_ALL;
        note.when = System.currentTimeMillis();
        note.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
        note.setLatestEventInfo(context, title, message, pendingIntent);
        noteManager.notify(id, note);
    }

    private static void hideNotification(Context context, int id) {
        NotificationManager notifMan = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notifMan.cancel(id);
    }
}
