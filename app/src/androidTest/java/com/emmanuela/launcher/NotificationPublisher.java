package com.emmanuela.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;

/** Runs in the test APK's own UID without depending on the target APK's Kotlin runtime. */
public final class NotificationPublisher extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        String operation = intent.getStringExtra("operation");
        if ("post".equals(operation)) {
            manager.createNotificationChannel(new NotificationChannel("test_filter", "Filter integration test", NotificationManager.IMPORTANCE_LOW));
            manager.notify(intent.getIntExtra("id", 6100), new Notification.Builder(context, "test_filter")
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(intent.getStringExtra("text"))
                .setCategory(intent.getStringExtra("category")).build());
        } else if ("clear".equals(operation)) {
            manager.cancelAll();
        }
        StringBuilder ids = new StringBuilder("|");
        for (StatusBarNotification row : manager.getActiveNotifications()) ids.append(row.getId()).append('|');
        setResultData(ids.toString());
    }
}
