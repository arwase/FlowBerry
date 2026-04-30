package com.arwase.flowberryapp.logic;

import android.content.Context;

import androidx.annotation.Nullable;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.database.PeriodDao;
import com.arwase.flowberryapp.models.Period;
import com.arwase.flowberryapp.utils.Converters;

import java.util.Date;

public final class PeriodRecorder {

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private PeriodRecorder() {
    }

    public static void recordPeriodStart(AppDatabase db, long dateMillis) {
        recordPeriodStart(null, db, dateMillis);
    }

    public static void recordPeriodStart(@Nullable Context context, AppDatabase db, long dateMillis) {
        if (db == null) {
            throw new IllegalStateException(resolveMessage(context, R.string.period_recorder_db_unavailable, "Database unavailable"));
        }

        PeriodDao periodDao = db.periodDao();
        long dayStart = Converters.getStartOfDayMillis(new Date(dateMillis));

        if (periodDao.getLastOpenPeriod() != null) {
            throw new IllegalStateException(resolveMessage(context, R.string.period_recorder_already_open, "A period is already ongoing"));
        }

        if (periodDao.findPeriodCovering(dayStart) != null) {
            throw new IllegalStateException(resolveMessage(context, R.string.period_recorder_overlapping_date, "This date already belongs to a period"));
        }

        Period period = new Period();
        period.startDateMillis = dayStart;
        period.endDateMillis = null;
        period.periodLengthDays = 0;
        period.cycleLengthDays = 0;
        periodDao.insert(period);

        PeriodCycleUpdater.recomputeAllCycleLengths(db);
    }

    public static void recordPeriodEnd(AppDatabase db, long dateMillis) {
        recordPeriodEnd(null, db, dateMillis);
    }

    public static void recordPeriodEnd(@Nullable Context context, AppDatabase db, long dateMillis) {
        if (db == null) {
            throw new IllegalStateException(resolveMessage(context, R.string.period_recorder_db_unavailable, "Database unavailable"));
        }

        PeriodDao periodDao = db.periodDao();
        long dayStart = Converters.getStartOfDayMillis(new Date(dateMillis));
        Period lastOpen = periodDao.getLastOpenPeriod();

        if (lastOpen == null) {
            throw new IllegalStateException(resolveMessage(context, R.string.period_recorder_no_open_period, "No ongoing period found"));
        }

        if (dayStart < lastOpen.startDateMillis) {
            throw new IllegalStateException(resolveMessage(context, R.string.period_recorder_end_before_start, "The end cannot be before the start"));
        }

        lastOpen.endDateMillis = dayStart;
        int periodLen = (int) ((lastOpen.endDateMillis - lastOpen.startDateMillis) / DAY_MILLIS) + 1;
        lastOpen.periodLengthDays = Math.max(1, periodLen);

        periodDao.update(lastOpen);
        PeriodCycleUpdater.recomputeAllCycleLengths(db);
    }

    private static String resolveMessage(@Nullable Context context, int resId, String fallback) {
        return context != null ? context.getString(resId) : fallback;
    }
}
