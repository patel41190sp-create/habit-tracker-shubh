package com.shubh.habittracker;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/** Offline daily reminders using Android AlarmManager (works when app is closed). */
public class ReminderScheduler {

    static final String CHANNEL_ID = "habit_reminders";
    private static final String PREFS = "reminders";
    private static final String KEY_JSON = "json";
    private static final String KEY_COUNT = "count";

    /** Called from the web UI whenever reminders change. */
    public static void sync(Context c, String json) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cancelAll(c);
        sp.edit().putString(KEY_JSON, json).apply();
        scheduleAll(c);
    }

    /** Re-arm all alarms from saved data (used after reboot / app update). */
    public static void scheduleAll(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_JSON, null);
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                boolean on = o.optInt("on", o.optBoolean("on", false) ? 1 : 0) == 1;
                String t = o.optString("t", "");
                if (!on || !t.matches("\\d{1,2}:\\d{2}")) continue;
                String[] hm = t.split(":");
                scheduleOne(c, i, Integer.parseInt(hm[0]), Integer.parseInt(hm[1]),
                        o.optString("e", "⏰"), o.optString("l", "Habit reminder"),
                        o.optString("m", ""));
            }
            sp.edit().putInt(KEY_COUNT, arr.length()).apply();
        } catch (Exception ignored) {
        }
    }

    /** Schedules the next occurrence (today if still ahead, otherwise tomorrow). */
    static void scheduleOne(Context c, int id, int hour, int min,
                            String emoji, String label, String msg) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 1000) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        Intent in = new Intent(c, ReminderReceiver.class);
        in.putExtra("id", id);
        in.putExtra("h", hour);
        in.putExtra("m", min);
        in.putExtra("e", emoji);
        in.putExtra("l", label);
        in.putExtra("msg", msg);
        PendingIntent pi = PendingIntent.getBroadcast(c, id, in,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
    }

    static void cancelAll(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int n = Math.max(sp.getInt(KEY_COUNT, 0), 30);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < n; i++) {
            Intent in = new Intent(c, ReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(c, i, in,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null && am != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                        "Habit reminders", NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Daily habit reminders");
                nm.createNotificationChannel(ch);
            }
        }
    }
}
