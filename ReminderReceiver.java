package com.shubh.habittracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent in) {
        int id = in.getIntExtra("id", 0);
        String emoji = in.getStringExtra("e");
        String label = in.getStringExtra("l");
        String msg = in.getStringExtra("msg");

        show(c, id, (emoji == null ? "" : emoji + " ") + (label == null ? "Habit reminder" : label),
                msg == null ? "" : msg);

        // Repeat every day
        ReminderScheduler.scheduleOne(c, id, in.getIntExtra("h", 8), in.getIntExtra("m", 0),
                emoji == null ? "" : emoji, label == null ? "" : label, msg == null ? "" : msg);
    }

    static void show(Context c, int id, String title, String text) {
        ReminderScheduler.ensureChannel(c);
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 1000 + id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, ReminderScheduler.CHANNEL_ID)
                : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT < 26) {
            b.setPriority(Notification.PRIORITY_HIGH);
            b.setDefaults(Notification.DEFAULT_ALL);
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(id, b.build());
    }
}
