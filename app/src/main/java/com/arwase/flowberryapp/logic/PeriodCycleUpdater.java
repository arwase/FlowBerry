package com.arwase.flowberryapp.logic;

import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.database.PeriodDao;
import com.arwase.flowberryapp.models.Period;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PeriodCycleUpdater {

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private PeriodCycleUpdater() {
    }

    public static void recomputeAllCycleLengths(AppDatabase db) {
        if (db == null) {
            return;
        }

        PeriodDao periodDao = db.periodDao();
        List<Period> periods = periodDao.getAll();
        if (periods == null || periods.isEmpty()) {
            return;
        }

        List<Period> sorted = new ArrayList<>(periods);
        Collections.sort(sorted, (a, b) -> Long.compare(a.startDateMillis, b.startDateMillis));

        for (int i = 0; i < sorted.size(); i++) {
            Period current = sorted.get(i);
            Period next = (i < sorted.size() - 1) ? sorted.get(i + 1) : null;

            int newCycleLength = 0;
            if (next != null) {
                long diffMillis = next.startDateMillis - current.startDateMillis;
                int diffDays = (int) Math.round(diffMillis / (double) DAY_MILLIS);
                if (diffDays >= 15 && diffDays <= 60) {
                    newCycleLength = diffDays;
                }
            }

            if (current.cycleLengthDays != newCycleLength) {
                current.cycleLengthDays = newCycleLength;
                periodDao.update(current);
            }
        }
    }
}
