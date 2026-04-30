package com.arwase.flowberryapp.logic;

import static com.arwase.flowberryapp.models.CycleStats.DAY_MILLIS;

import androidx.annotation.Nullable;

import com.arwase.flowberryapp.models.CyclePhase;
import com.arwase.flowberryapp.models.CycleStats;
import com.arwase.flowberryapp.models.Period;
import com.arwase.flowberryapp.models.Symptom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CycleCalculator {

    private static final int ESTIMATED_LUTEAL_LENGTH_DAYS = 14;
    private static final int DEFAULT_PMS_LENGTH_DAYS = 3;
    private static final int MAX_PMS_LENGTH_DAYS = 5;
    private static final int OVULATION_WINDOW_DAYS = 1;
    private static final int FERTILE_WINDOW_BEFORE_OVULATION_DAYS = 5;
    private static final int FERTILE_WINDOW_AFTER_OVULATION_DAYS = 1;

    public static CyclePhase getPhaseForDate(Period lastPeriod, long dateMillis) {
        return getPhaseForDate(lastPeriod, dateMillis, null);
    }

    public static CyclePhase getPhaseForDate(Period lastPeriod,
                                             long dateMillis,
                                             @Nullable List<Period> allPeriods) {
        CyclePosition cyclePosition = resolveCyclePosition(lastPeriod, dateMillis, allPeriods);
        if (cyclePosition == null) {
            return CyclePhase.UNKNOWN;
        }

        int ovulationDay = resolveOvulationDay(cyclePosition.cycleLength);
        if (Math.abs(cyclePosition.dayInCycle - ovulationDay) <= OVULATION_WINDOW_DAYS) {
            return CyclePhase.OVULATION;
        }

        if (cyclePosition.dayInCycle < ovulationDay) {
            return CyclePhase.FOLLICULAR;
        }

        return CyclePhase.LUTEAL;
    }

    public static boolean isPremenstrualDay(Period lastPeriod,
                                            long dateMillis,
                                            @Nullable List<Period> allPeriods) {
        return isPremenstrualDay(lastPeriod, dateMillis, allPeriods, DEFAULT_PMS_LENGTH_DAYS);
    }

    public static boolean isPremenstrualDay(Period lastPeriod,
                                            long dateMillis,
                                            @Nullable List<Period> allPeriods,
                                            int pmsLengthDays) {
        CyclePosition cyclePosition = resolveCyclePosition(lastPeriod, dateMillis, allPeriods);
        if (cyclePosition == null) {
            return false;
        }

        int safePmsLengthDays = clampPmsLengthDays(pmsLengthDays);
        int pmsStart = Math.max(cyclePosition.cycleLength - safePmsLengthDays, 0);
        return cyclePosition.dayInCycle >= pmsStart;
    }

    public static boolean isFertileWindowDay(Period lastPeriod,
                                             long dateMillis,
                                             @Nullable List<Period> allPeriods) {
        CyclePosition cyclePosition = resolveCyclePosition(lastPeriod, dateMillis, allPeriods);
        if (cyclePosition == null) {
            return false;
        }

        int ovulationDay = resolveOvulationDay(cyclePosition.cycleLength);
        int fertileStart = Math.max(ovulationDay - FERTILE_WINDOW_BEFORE_OVULATION_DAYS, 0);
        int fertileEnd = Math.min(
                ovulationDay + FERTILE_WINDOW_AFTER_OVULATION_DAYS,
                cyclePosition.cycleLength - 1
        );
        return cyclePosition.dayInCycle >= fertileStart
                && cyclePosition.dayInCycle <= fertileEnd;
    }

    public static int estimatePremenstrualLengthDays(@Nullable List<Period> allPeriods,
                                                     @Nullable List<Symptom> realSymptoms) {
        if (allPeriods == null || allPeriods.isEmpty()
                || realSymptoms == null || realSymptoms.isEmpty()) {
            return DEFAULT_PMS_LENGTH_DAYS;
        }

        Map<Long, Integer> observedLengthsByCycle = new HashMap<>();

        for (Symptom symptom : realSymptoms) {
            if (symptom == null || symptom.anticipated) {
                continue;
            }

            Period cyclePeriod = findLastPeriodBefore(allPeriods, symptom.dateMillis);
            if (cyclePeriod == null || isInPeriod(allPeriods, symptom.dateMillis)) {
                continue;
            }

            Period nextPeriod = findNextPeriodAfter(allPeriods, cyclePeriod.startDateMillis);
            if (nextPeriod == null) {
                continue;
            }

            CyclePhase phase = getPhaseForDate(cyclePeriod, symptom.dateMillis, allPeriods);
            if (phase != CyclePhase.LUTEAL) {
                continue;
            }

            int daysBeforeNextPeriod =
                    (int) ((nextPeriod.startDateMillis - symptom.dateMillis) / DAY_MILLIS);
            if (daysBeforeNextPeriod < 1 || daysBeforeNextPeriod > MAX_PMS_LENGTH_DAYS) {
                continue;
            }

            Integer currentLength = observedLengthsByCycle.get(cyclePeriod.startDateMillis);
            if (currentLength == null || daysBeforeNextPeriod > currentLength) {
                observedLengthsByCycle.put(cyclePeriod.startDateMillis, daysBeforeNextPeriod);
            }
        }

        if (observedLengthsByCycle.isEmpty()) {
            return DEFAULT_PMS_LENGTH_DAYS;
        }

        int totalLength = 0;
        for (int observedLength : observedLengthsByCycle.values()) {
            totalLength += observedLength;
        }

        int averageLength = Math.round((float) totalLength / observedLengthsByCycle.size());
        return clampPmsLengthDays(Math.max(DEFAULT_PMS_LENGTH_DAYS, averageLength));
    }

    @Nullable
    private static CyclePosition resolveCyclePosition(Period lastPeriod,
                                                      long dateMillis,
                                                      @Nullable List<Period> allPeriods) {
        if (lastPeriod == null || lastPeriod.startDateMillis == 0) {
            return null;
        }

        long diffMillis = dateMillis - lastPeriod.startDateMillis;
        if (diffMillis < 0) {
            return null;
        }

        boolean isFuture = dateMillis > System.currentTimeMillis();

        int cycleLengthFromPeriod = lastPeriod.cycleLengthDays > 0 ? lastPeriod.cycleLengthDays : 0;
        int avgCycleLen = 0;
        if (allPeriods != null && allPeriods.size() >= 2) {
            double avg = CycleStats.getAverageCycleLength(allPeriods);
            if (avg > 0) {
                avgCycleLen = Math.round((float) avg);
            }
        }

        int cycleLength;
        if (!isFuture && cycleLengthFromPeriod > 0) {
            cycleLength = cycleLengthFromPeriod;
        } else if (avgCycleLen > 0) {
            cycleLength = avgCycleLen;
        } else if (cycleLengthFromPeriod > 0) {
            cycleLength = cycleLengthFromPeriod;
        } else {
            cycleLength = 28;
        }

        if (cycleLength < 15 || cycleLength > 60) {
            cycleLength = 28;
        }

        int dayInCycle = (int) (diffMillis / DAY_MILLIS);
        if (dayInCycle < 0) {
            return null;
        }
        if (dayInCycle >= cycleLength) {
            dayInCycle = dayInCycle % cycleLength;
        }

        return new CyclePosition(dayInCycle, cycleLength);
    }

    private static final class CyclePosition {
        final int dayInCycle;
        final int cycleLength;

        CyclePosition(int dayInCycle, int cycleLength) {
            this.dayInCycle = dayInCycle;
            this.cycleLength = cycleLength;
        }
    }

    @Nullable
    private static Period findLastPeriodBefore(List<Period> periods, long timeMillis) {
        Period result = null;
        for (Period period : periods) {
            if (period.startDateMillis <= timeMillis
                    && (result == null || period.startDateMillis > result.startDateMillis)) {
                result = period;
            }
        }
        return result;
    }

    @Nullable
    private static Period findNextPeriodAfter(List<Period> periods, long timeMillis) {
        Period result = null;
        for (Period period : periods) {
            if (period.startDateMillis > timeMillis
                    && (result == null || period.startDateMillis < result.startDateMillis)) {
                result = period;
            }
        }
        return result;
    }

    private static boolean isInPeriod(List<Period> periods, long dayMillis) {
        for (Period period : periods) {
            if (period.startDateMillis > dayMillis) {
                continue;
            }

            if (period.endDateMillis == null || period.endDateMillis >= dayMillis) {
                return true;
            }
        }
        return false;
    }

    private static int clampPmsLengthDays(int pmsLengthDays) {
        if (pmsLengthDays < 1) {
            return DEFAULT_PMS_LENGTH_DAYS;
        }
        return Math.min(pmsLengthDays, MAX_PMS_LENGTH_DAYS);
    }

    private static int resolveOvulationDay(int cycleLength) {
        return Math.max(cycleLength - ESTIMATED_LUTEAL_LENGTH_DAYS, 0);
    }
}
