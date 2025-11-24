package com.arwase.flowberry.models;

import com.arwase.flowberry.models.Period;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CycleStats {

    // Durée moyenne du cycle en jours
    public static double getAverageCycleLength(List<Period> periods) {
        List<Long> cycleLengths = new ArrayList<>();

        for (int i = 0; i < periods.size() - 1; i++) {
            long start1 = periods.get(i).startDateMillis;
            long start2 = periods.get(i + 1).startDateMillis;

            long diffDays = (start1 - start2) / (1000L * 60 * 60 * 24);
            if (diffDays > 0 && diffDays < 60) { // filtre de sécurité
                cycleLengths.add(diffDays);
            }
        }

        if (cycleLengths.isEmpty()) return 0;

        long sum = 0;
        for (Long d : cycleLengths) sum += d;

        return (double) sum / cycleLengths.size();
    }

    // Durée moyenne des règles en jours
    public static double getAveragePeriodLength(List<Period> periods) {
        List<Long> lengths = new ArrayList<>();

        for (Period p : periods) {
            if (p.endDateMillis == null) continue;

            long diff = p.endDateMillis - p.startDateMillis;
            long days = diff / (1000L * 60 * 60 * 24);

            if (days > 0 && days < 15) {
                lengths.add(days);
            }
        }

        if (lengths.isEmpty()) return 0;

        long sum = 0;
        for (Long d : lengths) sum += d;

        return (double) sum / lengths.size();
    }

    // Prochaine date probable des règles (prévision simple)
    public static long predictNextPeriodStart(List<Period> periods) {
        if (periods.size() < 2) return 0;

        double avgCycle = getAverageCycleLength(periods);
        if (avgCycle < 20) return 0;

        Period last = periods.get(0); // trié du plus récent au plus ancien
        return last.startDateMillis + (long)(avgCycle * 24 * 60 * 60 * 1000);
    }
}
