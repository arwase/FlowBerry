package com.arwase.flowberryapp.logic;

import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.database.PeriodDao;
import com.arwase.flowberryapp.database.SymptomDao;
import com.arwase.flowberryapp.database.SymptomPhasePatternDao;
import com.arwase.flowberryapp.models.CyclePhase;
import com.arwase.flowberryapp.models.CycleStats;
import com.arwase.flowberryapp.models.Period;
import com.arwase.flowberryapp.models.Symptom;
import com.arwase.flowberryapp.models.SymptomPhasePattern;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SymptomPatternTrainer {

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private SymptomPatternTrainer() {
    }

    public static void recordRealSymptom(AppDatabase db, Symptom symptom) {
        if (db == null || symptom == null || symptom.anticipated) {
            return;
        }

        PeriodDao periodDao = db.periodDao();
        SymptomDao symptomDao = db.symptomDao();
        SymptomPhasePatternDao patternDao = db.symptomPhasePatternDao();

        List<Period> allPeriods = periodDao.getAll();
        if (allPeriods == null || allPeriods.isEmpty()) {
            return;
        }

        Period cyclePeriod = periodDao.getLastPeriodBefore(symptom.dateMillis);
        if (cyclePeriod == null) {
            return;
        }

        CyclePhase phase = resolvePhase(periodDao, allPeriods, cyclePeriod, symptom.dateMillis);
        String patternPhaseName = resolvePatternPhaseName(phase, cyclePeriod, symptom.dateMillis, allPeriods);
        long cycleEndInclusive = resolveCycleEndInclusive(periodDao, allPeriods, cyclePeriod, symptom.dateMillis);

        int countInCycle = symptomDao.countTypeInRange(
                symptom.type,
                false,
                cyclePeriod.startDateMillis,
                cycleEndInclusive
        );

        SymptomPhasePattern pattern = patternDao.getOne(patternPhaseName, symptom.type);
        if (pattern == null) {
            pattern = new SymptomPhasePattern();
            pattern.phase = patternPhaseName;
            pattern.symptomType = symptom.type;
            pattern.typicalIntensity = symptom.intensity;
            pattern.cyclesWithSymptom = 0;
        } else {
            pattern.typicalIntensity = (pattern.typicalIntensity + symptom.intensity) / 2;
        }

        if (countInCycle == 1) {
            pattern.cyclesWithSymptom++;
        }

        if (pattern.cyclesWithSymptom > 0) {
            patternDao.insertOrUpdate(pattern);
        }
    }

    public static void removeRealSymptom(AppDatabase db, Symptom symptom) {
        if (db == null || symptom == null || symptom.anticipated) {
            return;
        }

        PeriodDao periodDao = db.periodDao();
        SymptomDao symptomDao = db.symptomDao();
        SymptomPhasePatternDao patternDao = db.symptomPhasePatternDao();

        List<Period> allPeriods = periodDao.getAll();
        if (allPeriods == null || allPeriods.isEmpty()) {
            return;
        }

        Period cyclePeriod = periodDao.getLastPeriodBefore(symptom.dateMillis);
        if (cyclePeriod == null) {
            return;
        }

        CyclePhase phase = resolvePhase(periodDao, allPeriods, cyclePeriod, symptom.dateMillis);
        String patternPhaseName = resolvePatternPhaseName(phase, cyclePeriod, symptom.dateMillis, allPeriods);
        long cycleEndInclusive = resolveCycleEndInclusive(periodDao, allPeriods, cyclePeriod, symptom.dateMillis);

        int remainingCount = symptomDao.countTypeInRange(
                symptom.type,
                false,
                cyclePeriod.startDateMillis,
                cycleEndInclusive
        );

        SymptomPhasePattern pattern = patternDao.getOne(patternPhaseName, symptom.type);
        if (pattern == null
                && CyclePhase.LUTEAL.name().equals(patternPhaseName)
                && CycleCalculator.isPremenstrualDay(cyclePeriod, symptom.dateMillis, allPeriods)) {
            pattern = patternDao.getOne(CyclePhase.PMS.name(), symptom.type);
        }
        if (pattern == null || remainingCount > 0) {
            return;
        }

        pattern.cyclesWithSymptom--;
        if (pattern.cyclesWithSymptom <= 0) {
            patternDao.delete(pattern);
        } else {
            patternDao.insertOrUpdate(pattern);
        }
    }

    public static void rebuildAllPatterns(AppDatabase db) {
        if (db == null) {
            return;
        }

        PeriodDao periodDao = db.periodDao();
        SymptomDao symptomDao = db.symptomDao();
        SymptomPhasePatternDao patternDao = db.symptomPhasePatternDao();

        List<Period> allPeriods = periodDao.getAll();
        List<Symptom> realSymptoms = symptomDao.getAllReal();

        patternDao.deleteAll();

        if (allPeriods == null || allPeriods.isEmpty() || realSymptoms == null || realSymptoms.isEmpty()) {
            return;
        }

        Map<String, Aggregate> aggregates = new LinkedHashMap<>();
        Map<String, Set<Long>> cycleHits = new LinkedHashMap<>();

        for (Symptom symptom : realSymptoms) {
            Period cyclePeriod = periodDao.getLastPeriodBefore(symptom.dateMillis);
            if (cyclePeriod == null) {
                continue;
            }

            CyclePhase phase = resolvePhase(periodDao, allPeriods, cyclePeriod, symptom.dateMillis);
            String phaseName = resolvePatternPhaseName(phase, cyclePeriod, symptom.dateMillis, allPeriods);
            String key = phaseName + "|" + symptom.type;

            Aggregate aggregate = aggregates.get(key);
            if (aggregate == null) {
                aggregate = new Aggregate(phaseName, symptom.type);
                aggregates.put(key, aggregate);
                cycleHits.put(key, new HashSet<>());
            }

            aggregate.totalIntensity += symptom.intensity;
            aggregate.occurrenceCount++;

            Set<Long> hitCycles = cycleHits.get(key);
            if (hitCycles.add(cyclePeriod.startDateMillis)) {
                aggregate.cyclesWithSymptom++;
            }
        }

        for (Aggregate aggregate : aggregates.values()) {
            if (aggregate.occurrenceCount <= 0 || aggregate.cyclesWithSymptom <= 0) {
                continue;
            }

            SymptomPhasePattern pattern = new SymptomPhasePattern();
            pattern.phase = aggregate.phaseName;
            pattern.symptomType = aggregate.symptomType;
            pattern.typicalIntensity = Math.round((float) aggregate.totalIntensity / aggregate.occurrenceCount);
            pattern.cyclesWithSymptom = aggregate.cyclesWithSymptom;
            patternDao.insertOrUpdate(pattern);
        }
    }

    private static CyclePhase resolvePhase(PeriodDao periodDao,
                                           List<Period> allPeriods,
                                           Period cyclePeriod,
                                           long dayMillis) {
        Period menstruationPeriod = periodDao.findPeriodCovering(dayMillis);
        if (menstruationPeriod != null) {
            return CyclePhase.FOLLICULAR;
        }

        return CycleCalculator.getPhaseForDate(cyclePeriod, dayMillis, allPeriods);
    }

    private static String resolvePatternPhaseName(CyclePhase phase,
                                                  Period cyclePeriod,
                                                  long dayMillis,
                                                  List<Period> allPeriods) {
        if (phase == CyclePhase.LUTEAL
                && CycleCalculator.isPremenstrualDay(cyclePeriod, dayMillis, allPeriods)) {
            return CyclePhase.LUTEAL.name();
        }
        return phase.name();
    }

    private static long resolveCycleEndInclusive(PeriodDao periodDao,
                                                 List<Period> allPeriods,
                                                 Period cyclePeriod,
                                                 long fallbackDayMillis) {
        Period nextPeriod = periodDao.getNextPeriodAfter(cyclePeriod.startDateMillis);
        if (nextPeriod != null) {
            return nextPeriod.startDateMillis - 1L;
        }

        int cycleLengthDays = cyclePeriod.cycleLengthDays;
        if (cycleLengthDays <= 0) {
            double avg = CycleStats.getAverageCycleLength(allPeriods);
            cycleLengthDays = (avg > 0) ? Math.round((float) avg) : 28;
        }

        if (cycleLengthDays < 15 || cycleLengthDays > 60) {
            cycleLengthDays = 28;
        }

        long estimatedEnd = cyclePeriod.startDateMillis + cycleLengthDays * DAY_MILLIS - 1L;
        return Math.max(estimatedEnd, fallbackDayMillis);
    }

    private static final class Aggregate {
        final String phaseName;
        final String symptomType;
        int totalIntensity;
        int occurrenceCount;
        int cyclesWithSymptom;

        Aggregate(String phaseName, String symptomType) {
            this.phaseName = phaseName;
            this.symptomType = symptomType;
        }
    }
}
