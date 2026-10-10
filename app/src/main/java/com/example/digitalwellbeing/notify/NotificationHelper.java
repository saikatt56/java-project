package com.example.digitalwellbeing.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.digitalwellbeing.R;
import com.example.digitalwellbeing.ui.MainActivity;

/** Overuse alerts and daily/weekly summaries (proposal 3.4). */
public final class NotificationHelper {

    public static final String CHANNEL_LIMITS = "limits";
    public static final String CHANNEL_SUMMARY = "summary";

    private NotificationHelper() {
    }

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel limits = new NotificationChannel(
                CHANNEL_LIMITS,
                context.getString(R.string.channel_limits),
                NotificationManager.IMPORTANCE_HIGH);
        limits.setDescription(context.getString(R.string.channel_limits_desc));

        NotificationChannel summary = new NotificationChannel(
                CHANNEL_SUMMARY,
                context.getString(R.string.channel_summary),
                NotificationManager.IMPORTANCE_LOW);

        manager.createNotificationChannel(limits);
        manager.createNotificationChannel(summary);
    }

    public static void showLimitWarning(Context context, String appLabel, int percentUsed) {
        String title = percentUsed >= 100
                ? context.getString(R.string.notif_limit_reached, appLabel)
                : context.getString(R.string.notif_limit_near, appLabel, percentUsed);
        notify(context, CHANNEL_LIMITS, appLabel.hashCode(), title, null);
    }

    public static void showSummary(Context context, String title, String body) {
        notify(context, CHANNEL_SUMMARY, 1001, title, body);
    }

    private static void notify(Context context, String channel, int id, String title, String body) {
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                context, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_stat_wellbeing)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(body == null ? null : new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();

        try {
            NotificationManagerCompat.from(context).notify(id, notification);
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS not granted on Android 13+. Silently skip:
            // the dashboard still shows the same information.
        }
    }
}
