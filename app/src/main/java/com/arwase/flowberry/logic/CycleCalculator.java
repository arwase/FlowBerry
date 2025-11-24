package com.arwase.flowberry.logic;

import com.arwase.flowberry.models.CyclePhase;
import com.arwase.flowberry.models.Period;

public class CycleCalculator {

    /**
     * Retourne la phase du cycle pour une date donnée (en millis)
     *
     * @param lastPeriod  La dernière période de règles avant la date (ou null)
     * @param dateMillis  La date à analyser (timestamp début de journée)
     * @return CyclePhase
     */
    public static CyclePhase getPhaseForDate(Period lastPeriod, long dateMillis) {

        if (lastPeriod == null || lastPeriod.startDateMillis == 0) {
            return CyclePhase.UNKNOWN;
        }

        long cycleLength = lastPeriod.cycleLengthDays > 0 ? lastPeriod.cycleLengthDays : 28;
        long periodLength = lastPeriod.periodLengthDays > 0 ? lastPeriod.periodLengthDays : 5;

        // ⚠️ Différence en jours depuis le début des règles
        long diffMillis = dateMillis - lastPeriod.startDateMillis;
        long diffDays = diffMillis / (1000L * 60L * 60L * 24L);

        if (diffDays < 0) {
            return CyclePhase.UNKNOWN;
        }

        // Jour du cycle (0 = 1er jour)
        long dayInCycle = diffDays % cycleLength;

        // Règles : jour 1 → dayInCycle < periodLength
        if (dayInCycle < periodLength) {
            return CyclePhase.MENSTRUATION;
        }

        // Ovulation approximative à la moitié du cycle
        long ovulationDay = cycleLength / 2;

        if (Math.abs(dayInCycle - ovulationDay) <= 1) {
            return CyclePhase.OVULATION;
        }

        // PMS : les 5 derniers jours du cycle
        if (dayInCycle >= cycleLength - 5) {
            return CyclePhase.PMS;
        }

        // Folliculaire avant ovulation
        if (dayInCycle < ovulationDay) {
            return CyclePhase.FOLLICULAR;
        }

        // Sinon phase lutéale
        return CyclePhase.LUTEAL;
    }
}
