package com.shubh.habittracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-arms reminders after phone restart or app update. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        ReminderScheduler.scheduleAll(c);
    }
}
