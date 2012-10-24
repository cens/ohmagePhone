
package org.ohmage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.ohmage.activity.LoginActivity;
import org.ohmage.activity.MobilityActivity;
import org.ohmage.activity.UploadQueueActivity;

import java.util.ArrayList;

public class NotificationHelper {

    private static final int AUTH_ERROR_ID = 0;
    private static final int NOTIFICATION_ID = 1;
    private static final int UPLOAD_ERROR_ID = 2;
    private static final int PROBE_UPLOAD_ERROR_ID = 3;
    private static final int RESPONSE_UPLOAD_ERROR_ID = 4;

    public static void showAuthNotification(Context context) {
        showNotification(context, AUTH_ERROR_ID, "Authentication error!",
                "Tap here to re-enter credentials.", new Intent(context, LoginActivity.class));
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

    public static void showProbeUploadErrorNotification(Context context, ArrayList<String> probes) {
        StringBuilder body = new StringBuilder("Error uploading probes: ");
        for (String probe : probes) {
            body.append(probe).append(" ");
        }
        showNotification(context, PROBE_UPLOAD_ERROR_ID, "Probe upload error!", body.toString(),
                new Intent(context, MobilityActivity.class));
    }

    public static void hideProbeUploadErrorNotification(Context context) {
        hideNotification(context, PROBE_UPLOAD_ERROR_ID);
    }

    public static void showResponseUploadErrorNotification(Context context, ArrayList<String> probes) {
        StringBuilder body = new StringBuilder("Error uploading responses: ");
        for (String probe : probes) {
            body.append(probe).append(" ");
        }
        showNotification(context, RESPONSE_UPLOAD_ERROR_ID, "Response upload error!",
                body.toString(),
                new Intent(context, MobilityActivity.class));
    }

    public static void hideResponseUploadErrorNotification(Context context) {
        hideNotification(context, RESPONSE_UPLOAD_ERROR_ID);
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
