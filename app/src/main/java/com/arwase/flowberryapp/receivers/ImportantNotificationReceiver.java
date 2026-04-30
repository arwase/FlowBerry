package com.arwase.flowberryapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.arwase.flowberryapp.notifications.ImportantNotificationScheduler;

public class ImportantNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ImportantNotificationScheduler.dispatchTodayIfNeeded(context);
    }
}
