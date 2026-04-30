package com.arwase.flowberryapp.notifications;

import static com.arwase.flowberryapp.models.CycleStats.DAY_MILLIS;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.activities.MainActivity;
import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.database.PeriodDao;
import com.arwase.flowberryapp.models.CycleStats;
import com.arwase.flowberryapp.models.Period;
import com.arwase.flowberryapp.receivers.ImportantNotificationReceiver;
import com.arwase.flowberryapp.utils.Converters;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public final class ImportantNotificationScheduler {

    public static final String PREFS_NAME = "flowberry_prefs";
    public static final String KEY_IMPORTANT_NOTIFICATIONS = "pref_important_notifications";

    private static final String KEY_LAST_NOTIFICATION_SIGNATURE = "pref_last_important_notification_signature";
    private static final String KEY_SHOW_FERTILITY_WARNINGS = "show_fertility_warnings";
    private static final String KEY_SHOW_OVULATION_AS_WANTED = "show_ovulation_as_wanted";

    private static final String CHANNEL_ID = "flowberry_important_notifications";
    private static final int NOTIFICATION_ID = 4001;
    private static final int ALARM_REQUEST_CODE = 4002;
    private static final int DEFAULT_CYCLE_LENGTH_DAYS = 28;
    private static final int DEFAULT_NOTIFICATION_HOUR = 9;
    private static final int ESTIMATED_LUTEAL_LENGTH_DAYS = 14;
    private static final int FERTILE_WINDOW_BEFORE_OVULATION_DAYS = 5;

    private ImportantNotificationScheduler() {
    }

    public static void syncScheduling(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_IMPORTANT_NOTIFICATIONS, false);
        syncScheduling(context, enabled);
    }

    public static void syncScheduling(Context context, boolean enabled) {
        if (enabled) {
            createNotificationChannel(context);
            scheduleDailyAlarm(context);
            dispatchTodayIfNeeded(context, true);
            return;
        }

        cancelDailyAlarm(context);
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }

    public static void dispatchTodayIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_IMPORTANT_NOTIFICATIONS, false);
        dispatchTodayIfNeeded(context, enabled);
    }

    private static void dispatchTodayIfNeeded(Context context, boolean enabled) {
        if (!enabled) {
            return;
        }

        NotificationEvent event = resolveTodayEvent(context);
        if (event == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String signature = event.type + ":" + event.dayStartMillis;
        String lastSignature = prefs.getString(KEY_LAST_NOTIFICATION_SIGNATURE, null);
        if (signature.equals(lastSignature)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        createNotificationChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_drop_blood)
                .setContentTitle(event.title)
                .setContentText(event.message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(event.message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        prefs.edit().putString(KEY_LAST_NOTIFICATION_SIGNATURE, signature).apply();
    }

    private static NotificationEvent resolveTodayEvent(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        PeriodDao periodDao = db.periodDao();
        List<Period> periods = periodDao.getAll();
        if (periods == null || periods.isEmpty()) {
            return null;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long todayStart = Converters.getStartOfDayMillis(new Date());
        Period currentPeriod = periodDao.findPeriodCovering(todayStart);

        long predictedPeriodStart = resolvePredictedPeriodStart(periods, todayStart);
        if (currentPeriod == null && predictedPeriodStart == todayStart) {
            return new NotificationEvent(
                    "period_start",
                    todayStart,
                    context.getString(R.string.notification_period_title),
                    context.getString(R.string.notification_period_message)
            );
        }

        boolean showFertilityWarnings = prefs.getBoolean(KEY_SHOW_FERTILITY_WARNINGS, true);
        boolean showOvulationAsWanted = prefs.getBoolean(KEY_SHOW_OVULATION_AS_WANTED, false);
        if (!showFertilityWarnings || showOvulationAsWanted || currentPeriod != null) {
            return null;
        }

        Period lastPeriod = periodDao.getLastPeriodBefore(todayStart);
        if (lastPeriod == null) {
            return null;
        }

        if (!isFertileWindowStartDay(lastPeriod, todayStart, periods)) {
            return null;
        }

        return new NotificationEvent(
                "fertility_start",
                todayStart,
                context.getString(R.string.notification_fertility_title),
                context.getString(R.string.notification_fertility_message)
        );
    }

    private static long resolvePredictedPeriodStart(List<Period> periods, long todayStart) {
        Period latestPeriod = null;
        for (Period period : periods) {
            if (period == null || period.startDateMillis == 0) {
                continue;
            }
            if (latestPeriod == null || period.startDateMillis > latestPeriod.startDateMillis) {
                latestPeriod = period;
            }
        }

        if (latestPeriod == null) {
            return -1L;
        }

        int cycleLengthDays = resolveCycleLengthDays(latestPeriod, periods);
        long predictedStart = Converters.getStartOfDayMillis(new Date(latestPeriod.startDateMillis));
        while (predictedStart < todayStart) {
            predictedStart += cycleLengthDays * DAY_MILLIS;
        }
        return predictedStart;
    }

    private static boolean isFertileWindowStartDay(Period lastPeriod,
                                                   long dayMillis,
                                                   List<Period> periods) {
        long cycleStart = Converters.getStartOfDayMillis(new Date(lastPeriod.startDateMillis));
        long diffMillis = dayMillis - cycleStart;
        if (diffMillis < 0) {
            return false;
        }

        int cycleLengthDays = resolveCycleLengthDays(lastPeriod, periods);
        int dayInCycle = (int) (diffMillis / DAY_MILLIS);
        if (dayInCycle >= cycleLengthDays) {
            dayInCycle = dayInCycle % cycleLengthDays;
        }

        int ovulationDay = Math.max(cycleLengthDays - ESTIMATED_LUTEAL_LENGTH_DAYS, 0);
        int fertileStartDay = Math.max(ovulationDay - FERTILE_WINDOW_BEFORE_OVULATION_DAYS, 0);
        return dayInCycle == fertileStartDay;
    }

    private static int resolveCycleLengthDays(Period referencePeriod, List<Period> periods) {
        int cycleLengthDays = referencePeriod != null ? referencePeriod.cycleLengthDays : 0;
        if (periods != null && periods.size() >= 2) {
            double averageCycleLength = CycleStats.getAverageCycleLength(periods);
            if (averageCycleLength > 0) {
                cycleLengthDays = Math.round((float) averageCycleLength);
            }
        }

        if (cycleLengthDays < 15 || cycleLengthDays > 60) {
            cycleLengthDays = DEFAULT_CYCLE_LENGTH_DAYS;
        }
        return cycleLengthDays;
    }

    private static void scheduleDailyAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildAlarmPendingIntent(context);
        alarmManager.cancel(pendingIntent);
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                computeNextTriggerMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    private static void cancelDailyAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildAlarmPendingIntent(context);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private static PendingIntent buildAlarmPendingIntent(Context context) {
        Intent intent = new Intent(context, ImportantNotificationReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static long computeNextTriggerMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, DEFAULT_NOTIFICATION_HOUR);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(context.getString(R.string.notification_channel_description));
        manager.createNotificationChannel(channel);
    }

    private static final class NotificationEvent {
        final String type;
        final long dayStartMillis;
        final String title;
        final String message;

        NotificationEvent(String type, long dayStartMillis, String title, String message) {
            this.type = type;
            this.dayStartMillis = dayStartMillis;
            this.title = title;
            this.message = message;
        }
    }
}
