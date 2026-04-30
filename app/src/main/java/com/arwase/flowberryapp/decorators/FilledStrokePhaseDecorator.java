package com.arwase.flowberryapp.decorators;

import android.graphics.drawable.GradientDrawable;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.Nullable;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Collection;
import java.util.HashSet;

public class FilledStrokePhaseDecorator implements DayViewDecorator {

    private final HashSet<CalendarDay> dates;
    @Nullable
    private final Integer fillColor;
    private final int strokeColor;
    private final int strokeWidthPx;
    @Nullable
    private final Integer textColor;

    public FilledStrokePhaseDecorator(Collection<CalendarDay> dates,
                                      @Nullable Integer fillColor,
                                      int strokeColor,
                                      int strokeWidthPx,
                                      @Nullable Integer textColor) {
        this.dates = new HashSet<>(dates);
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.strokeWidthPx = strokeWidthPx;
        this.textColor = textColor;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(fillColor != null ? fillColor : android.graphics.Color.TRANSPARENT);
        background.setCornerRadius(48f);
        background.setStroke(strokeWidthPx, strokeColor);
        view.setBackgroundDrawable(background);

        if (textColor != null) {
            view.addSpan(new ForegroundColorSpan(textColor));
        }
    }
}
