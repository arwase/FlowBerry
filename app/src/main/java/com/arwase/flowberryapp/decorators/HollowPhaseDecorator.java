package com.arwase.flowberryapp.decorators;

import android.graphics.drawable.Drawable;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;
import java.util.Set;

public class HollowPhaseDecorator implements DayViewDecorator {

    private final HashSet<CalendarDay> dates;
    private final Drawable background;

    public HollowPhaseDecorator(Set<CalendarDay> dates, Drawable background) {
        this.dates = new HashSet<>(dates);
        this.background = background;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setBackgroundDrawable(background);
        // On NE change PAS la couleur du texte
        // (contrairement à PhaseDecorator qui met le texte en blanc)
    }
}
