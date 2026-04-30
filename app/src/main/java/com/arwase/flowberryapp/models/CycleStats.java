package com.arwase.flowberryapp.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CycleStats {
    public static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;
    // Durée moyenne du cycle (en jours)
    public static double getAverageCycleLength(List<Period> periods) {
        if (periods == null || periods.size() < 2) return 0;

        // ⚠️ on travaille sur une COPIE pour éviter ConcurrentModification
        List<Period> sorted = new ArrayList<>(periods);
        Collections.sort(sorted, (a, b) -> Long.compare(a.startDateMillis, b.startDateMillis));

        List<Long> lengths = new ArrayList<>();

        for (int i = 0; i < sorted.size() - 1; i++) {
            long start1 = sorted.get(i).startDateMillis;
            long start2 = sorted.get(i + 1).startDateMillis;

            long diffDays = (start2 - start1) / DAY_MILLIS; // ordre corrigé
            if (diffDays >= 15 && diffDays <= 60) {
                lengths.add(diffDays);
            }
        }

        if (lengths.isEmpty()) return 28;

        long sum = 0;
        for (long d : lengths) sum += d;

        return sum * 1.0 / lengths.size();
    }
    // Durée moyenne des règles (en jours, inclusif)
    public static double getAveragePeriodLength(List<Period> periods) {
        if (periods == null || periods.isEmpty()) return 0;

        List<Long> lengths = new ArrayList<>();

        for (Period p : periods) {
            if (p.startDateMillis == 0 || p.endDateMillis == null) continue;

            long diff = p.endDateMillis - p.startDateMillis;
            long days = (diff / DAY_MILLIS) + 1; // +1 pour inclure début & fin

            if (days >= 1 && days <= 15) {
                lengths.add(days);
            }
        }

        if (lengths.isEmpty()) return 5;

        long sum = 0;
        for (long d : lengths) sum += d;

        return sum * 1.0 / lengths.size();
    }
    // Prochaine date probable
    public static long predictNextPeriodStart(List<Period> periods) {
        if (periods == null || periods.size() < 2) return 0;

        // ⚠️ copie triée (récent en dernier ou premier selon ton choix)
        List<Period> sorted = new ArrayList<>(periods);
        Collections.sort(sorted, (a, b) -> Long.compare(a.startDateMillis, b.startDateMillis));

        Period last = sorted.get(sorted.size() - 1); // le plus récent

        double avg = getAverageCycleLength(sorted);
        if (avg <= 0) return 0;

        long avgMillis = Math.round(avg) * DAY_MILLIS;
        return last.startDateMillis + avgMillis;
    }


    public static int getCycleLength(Period current, Period next) {
        if (current == null) return 0;

        // si la durée du cycle est renseignée → on la prend
        if (current.cycleLengthDays > 0)
            return current.cycleLengthDays;

        // sinon on dérive de la période suivante
        if (next != null) {
            long diff = next.startDateMillis - current.startDateMillis;
            return (int)(diff / DAY_MILLIS);
        }

        // si c’est le dernier cycle et pas encore assez de données → inconnu
        return 0;
    }
    public static int getCycleLengthForPosition(List<Period> periods, int position) {
        if (periods == null || position < 0 || position >= periods.size()) return 0;

        Period current = periods.get(position);
        Period next = (position > 0) ? periods.get(position - 1) : null;

        return getCycleLength(current, next);
    }

}
