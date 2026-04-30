package com.arwase.flowberryapp.decorators;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.Nullable;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;
import java.util.Set;

public class HighlightedDayDecorator implements DayViewDecorator {

    private final HashSet<CalendarDay> dates;
    @Nullable
    private final Integer outerFillColor;
    @Nullable
    private final Integer outerStrokeColor;
    private final int outerStrokeWidthPx;
    @Nullable
    private final Integer innerFillColor;
    @Nullable
    private final Integer innerStrokeColor;
    private final int innerStrokeWidthPx;
    @Nullable
    private final Integer textColor;
    private final float cornerRadiusPx;
    private final int insetPx;

    public HighlightedDayDecorator(Set<CalendarDay> dates,
                                   @Nullable Integer outerFillColor,
                                   @Nullable Integer outerStrokeColor,
                                   int outerStrokeWidthPx,
                                   @Nullable Integer innerFillColor,
                                   @Nullable Integer innerStrokeColor,
                                   int innerStrokeWidthPx,
                                   @Nullable Integer textColor,
                                   float cornerRadiusPx,
                                   int insetPx) {
        this.dates = new HashSet<>(dates);
        this.outerFillColor = outerFillColor;
        this.outerStrokeColor = outerStrokeColor;
        this.outerStrokeWidthPx = outerStrokeWidthPx;
        this.innerFillColor = innerFillColor;
        this.innerStrokeColor = innerStrokeColor;
        this.innerStrokeWidthPx = innerStrokeWidthPx;
        this.textColor = textColor;
        this.cornerRadiusPx = cornerRadiusPx;
        this.insetPx = insetPx;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        GradientDrawable outer = new GradientDrawable();
        outer.setShape(GradientDrawable.RECTANGLE);
        outer.setColor(outerFillColor != null ? outerFillColor : Color.TRANSPARENT);
        outer.setCornerRadius(cornerRadiusPx);
        if (outerStrokeColor != null && outerStrokeWidthPx > 0) {
            outer.setStroke(outerStrokeWidthPx, outerStrokeColor);
        }

        GradientDrawable inner = new GradientDrawable();
        inner.setShape(GradientDrawable.RECTANGLE);
        inner.setColor(innerFillColor != null ? innerFillColor : Color.TRANSPARENT);
        inner.setCornerRadius(Math.max(0f, cornerRadiusPx - insetPx));
        if (innerStrokeColor != null && innerStrokeWidthPx > 0) {
            inner.setStroke(innerStrokeWidthPx, innerStrokeColor);
        }

        InsetDrawable insetInner = new InsetDrawable(inner, insetPx);
        LayerDrawable layeredBackground = new LayerDrawable(
                new android.graphics.drawable.Drawable[]{outer, insetInner}
        );
        view.setBackgroundDrawable(layeredBackground);

        if (textColor != null) {
            view.addSpan(new ForegroundColorSpan(textColor));
        }
    }
}
