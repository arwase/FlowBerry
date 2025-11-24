package com.arwase.flowberry.decorators;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.style.ForegroundColorSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Collection;
import java.util.HashSet;

public class PhaseDecorator implements DayViewDecorator {

    private final HashSet<CalendarDay> dates;
    private final int color;

    public PhaseDecorator(Collection<CalendarDay> dates, int color) {
        this.dates = new HashSet<>(dates);
        this.color = color;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        // Fond coloré arrondi
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(color);
        background.setCornerRadius(48f); // plus ou moins rond selon ton goût

        view.setBackgroundDrawable(background);

        // Texte en blanc pour être lisible
        view.addSpan(new ForegroundColorSpan(Color.WHITE));
    }
}
