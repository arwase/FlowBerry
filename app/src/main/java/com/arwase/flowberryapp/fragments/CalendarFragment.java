package com.arwase.flowberryapp.fragments;

import static com.arwase.flowberryapp.models.CycleStats.DAY_MILLIS;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.activities.EditPeriodActivity;
import com.arwase.flowberryapp.activities.EditSymptomActivity;
import com.arwase.flowberryapp.adapters.SymptomAdapter;
import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.database.PeriodDao;
import com.arwase.flowberryapp.database.SymptomDao;
import com.arwase.flowberryapp.database.SymptomPhasePatternDao;
import com.arwase.flowberryapp.decorators.FilledStrokePhaseDecorator;
import com.arwase.flowberryapp.decorators.HighlightedDayDecorator;
import com.arwase.flowberryapp.decorators.HollowPhaseDecorator;
import com.arwase.flowberryapp.decorators.PhaseDecorator;
import com.arwase.flowberryapp.logic.CycleCalculator;
import com.arwase.flowberryapp.logic.PeriodRecorder;
import com.arwase.flowberryapp.logic.SymptomPatternTrainer;
import com.arwase.flowberryapp.models.CyclePhase;
import com.arwase.flowberryapp.models.CycleStats;
import com.arwase.flowberryapp.models.Period;
import com.arwase.flowberryapp.models.Symptom;
import com.arwase.flowberryapp.models.SymptomPhasePattern;
import com.arwase.flowberryapp.utils.Converters;
import com.google.android.material.button.MaterialButton;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import org.threeten.bp.LocalDate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarFragment extends Fragment {

    private static final int QUICK_ACTION_NONE = 0;
    private static final int QUICK_ACTION_START_PERIOD = 1;
    private static final int QUICK_ACTION_END_PERIOD = 2;

    private MaterialCalendarView calendarView;
    private TextView textSelectedDate;
    private TextView textPhase;
    private TextView textFertilityHint;
    private TextView textPredictionWarning;
    private MaterialButton buttonQuickPeriodAction;
    private RecyclerView recyclerSymptoms;
    private android.content.SharedPreferences prefs;
    private AppDatabase db;
    private PeriodDao periodDao;
    private SymptomDao symptomDao;
    private SymptomAdapter symptomAdapter;
    private Date currentSelectedDate;
    private int currentQuickPeriodAction = QUICK_ACTION_NONE;
    private boolean initialPeriodPromptOpen = false;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public CalendarFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        checkInitialPeriod();
        updateCalendarDecorators(view);
        updateSelectedDateUI();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getInstance(requireContext());
        periodDao = db.periodDao();
        symptomDao = db.symptomDao();

        calendarView = view.findViewById(R.id.calendarView);
        textSelectedDate = view.findViewById(R.id.textSelectedDate);
        textPhase = view.findViewById(R.id.textPhase);
        textFertilityHint = view.findViewById(R.id.textFertilityHint);
        textPredictionWarning = view.findViewById(R.id.textPredictionWarning);
        buttonQuickPeriodAction = view.findViewById(R.id.buttonQuickPeriodAction);
        recyclerSymptoms = view.findViewById(R.id.recyclerSymptoms);

        prefs = requireContext().getSharedPreferences("flowberry_prefs", Context.MODE_PRIVATE);

        Calendar cal = Calendar.getInstance();
        currentSelectedDate = cal.getTime();

        recyclerSymptoms.setLayoutManager(new LinearLayoutManager(requireContext()));
        symptomAdapter = new SymptomAdapter(
                new ArrayList<>(),
                new SymptomAdapter.SymptomListener() {
                    @Override
                    public void onEdit(Symptom symptom) {
                        Intent intent = new Intent(requireContext(), EditSymptomActivity.class);
                        intent.putExtra("symptom_id", symptom.id);
                        startActivity(intent);
                    }

                    @Override
                    public void onDelete(Symptom symptom) {
                        AppDatabase db = AppDatabase.getInstance(requireContext());
                        db.symptomDao().delete(symptom);
                        SymptomPatternTrainer.removeRealSymptom(db, symptom);
                        Toast.makeText(
                                getContext(),
                                R.string.calendar_symptom_deleted,
                                Toast.LENGTH_SHORT
                        ).show();
                        updateSelectedDateUI();
                    }

                    @Override
                    public void onValidate(Symptom symptom) {
                        Symptom real = new Symptom();
                        real.dateMillis = symptom.dateMillis;
                        real.type = symptom.type;
                        real.intensity = symptom.intensity;
                        real.notes = getString(R.string.calendar_confirmed_symptom_note);
                        real.anticipated = false;
                        real.validated = true;

                        symptomDao.insert(real);
                        SymptomPatternTrainer.recordRealSymptom(db, real);

                        Toast.makeText(
                                getContext(),
                                R.string.calendar_symptom_validated,
                                Toast.LENGTH_SHORT
                        ).show();
                        updateSelectedDateUI();
                    }
                }
        );
        recyclerSymptoms.setAdapter(symptomAdapter);

        if (buttonQuickPeriodAction != null) {
            buttonQuickPeriodAction.setOnClickListener(v -> handleQuickPeriodAction());
        }

        LocalDate localDate = LocalDate.of(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
        );
        calendarView.setSelectedDate(CalendarDay.from(localDate));
        updateSelectedDateUI();

        checkInitialPeriod();
        updateCalendarDecorators(view);

        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(MaterialCalendarView widget, CalendarDay date, boolean selected) {
                Calendar c = Calendar.getInstance();
                c.set(date.getYear(), date.getMonth() - 1, date.getDay());
                currentSelectedDate = c.getTime();
                updateCalendarDecorators(getView());
                updateSelectedDateUI();
            }
        });
    }

    private void checkInitialPeriod() {
        if (initialPeriodPromptOpen) {
            return;
        }
        Period last = periodDao.getLastPeriod();
        if (last == null) {
            initialPeriodPromptOpen = true;
            Intent intent = new Intent(requireContext(), EditPeriodActivity.class);
            intent.putExtra("mode_initial", true);
            startActivity(intent);
        }
    }

    private MenstruationWindows computeMenstruationWindows(List<Period> periods) {
        MenstruationWindows result = new MenstruationWindows();
        if (periods == null || periods.isEmpty()) {
            return result;
        }

        long todayStart = Converters.getStartOfDayMillis(new Date());
        long oneYearAhead = todayStart + 365L * DAY_MILLIS;

        double avgCycle = CycleStats.getAverageCycleLength(periods);
        double avgPeriod = CycleStats.getAveragePeriodLength(periods);

        int avgCycleLen = (avgCycle > 0) ? Math.round((float) avgCycle) : 28;
        int avgPeriodLen = (avgPeriod > 0) ? Math.round((float) avgPeriod) : 5;

        if (avgCycleLen < 15 || avgCycleLen > 60) {
            avgCycleLen = 28;
        }
        if (avgPeriodLen < 1 || avgPeriodLen > 15) {
            avgPeriodLen = 5;
        }

        for (Period period : periods) {
            if (period.startDateMillis == 0) {
                continue;
            }

            long start = Converters.getStartOfDayMillis(new Date(period.startDateMillis));
            if (period.endDateMillis != null) {
                long end = Converters.getStartOfDayMillis(new Date(period.endDateMillis));
                if (end >= start) {
                    result.confirmed.add(new long[]{start, end});
                }
                continue;
            }

            long theoreticalEnd = start + (avgPeriodLen - 1L) * DAY_MILLIS;
            if (theoreticalEnd <= todayStart) {
                if (theoreticalEnd >= start) {
                    result.confirmed.add(new long[]{start, theoreticalEnd});
                }
                continue;
            }

            long confirmedEnd = Math.min(todayStart, theoreticalEnd);
            if (confirmedEnd >= start) {
                result.confirmed.add(new long[]{start, confirmedEnd});
            }

            long anticipatedStart = Math.max(todayStart + DAY_MILLIS, start);
            if (theoreticalEnd >= anticipatedStart) {
                result.anticipated.add(new long[]{anticipatedStart, theoreticalEnd});
            }
        }

        Period last = periods.get(0);
        for (Period period : periods) {
            if (period.startDateMillis > last.startDateMillis) {
                last = period;
            }
        }

        long predictedStart = Converters.getStartOfDayMillis(new Date(last.startDateMillis));
        while (true) {
            predictedStart += avgCycleLen * DAY_MILLIS;
            if (predictedStart > oneYearAhead) {
                break;
            }

            long predictedEnd = predictedStart + (avgPeriodLen - 1L) * DAY_MILLIS;
            if (predictedEnd >= todayStart) {
                result.anticipated.add(new long[]{predictedStart, predictedEnd});
            }
        }

        return result;
    }

    private boolean isInWindows(long dayMillis, List<long[]> windows) {
        for (long[] window : windows) {
            if (dayMillis >= window[0] && dayMillis <= window[1]) {
                return true;
            }
        }
        return false;
    }

    private CyclePhase getFullPhaseForDate(long dayMillis) {
        List<Period> periods = periodDao.getAll();
        if (periods == null || periods.isEmpty()) {
            return CyclePhase.UNKNOWN;
        }

        MenstruationWindows windows = computeMenstruationWindows(periods);
        long dayStart = Converters.getStartOfDayMillis(new Date(dayMillis));
        long todayStart = Converters.getStartOfDayMillis(new Date());

        boolean inConfirmed = isInWindows(dayStart, windows.confirmed);
        boolean inAnticipated = !inConfirmed
                && isInWindows(dayStart, windows.anticipated)
                && dayStart >= todayStart;

        if (inConfirmed || inAnticipated) {
            return CyclePhase.FOLLICULAR;
        }

        Period lastPeriod = periodDao.getLastPeriodBefore(dayMillis);
        return CycleCalculator.getPhaseForDate(lastPeriod, dayMillis, periods);
    }

    private void updateCalendarDecorators(View view) {
        if (view == null) {
            return;
        }

        boolean showPms = prefs.getBoolean("show_pms", true);
        boolean showLuteal = prefs.getBoolean("show_luteal", true);
        boolean showFollicular = prefs.getBoolean("show_follicular", true);

        List<Period> allPeriodsForPms = periodDao.getAll();
        List<Symptom> realSymptoms = symptomDao.getAllReal();
        int pmsLengthDays = CycleCalculator.estimatePremenstrualLengthDays(allPeriodsForPms, realSymptoms);

        View layoutLegendFollicular = view.findViewById(R.id.layoutLegendFollicular);
        View layoutLegendLuteal = view.findViewById(R.id.layoutLegendLuteal);
        View layoutLegendPms = view.findViewById(R.id.layoutLegendPMS);

        List<Period> periods = periodDao.getAll();
        if (periods == null || periods.isEmpty()) {
            calendarView.removeDecorators();
            return;
        }

        if (layoutLegendFollicular != null) {
            layoutLegendFollicular.setVisibility(showFollicular ? View.VISIBLE : View.GONE);
        }
        if (layoutLegendLuteal != null) {
            layoutLegendLuteal.setVisibility(showLuteal ? View.VISIBLE : View.GONE);
        }
        if (layoutLegendPms != null) {
            layoutLegendPms.setVisibility(showPms ? View.VISIBLE : View.GONE);
        }

        Collections.sort(periods, (p1, p2) -> Long.compare(p1.startDateMillis, p2.startDateMillis));

        MenstruationWindows windows = computeMenstruationWindows(periods);

        Set<CalendarDay> menstruationConfirmedDays = new HashSet<>();
        Set<CalendarDay> menstruationAnticipatedDays = new HashSet<>();
        Set<CalendarDay> follicularDays = new HashSet<>();
        Set<CalendarDay> ovulationDays = new HashSet<>();
        Set<CalendarDay> lutealDays = new HashSet<>();
        Set<CalendarDay> pmsDays = new HashSet<>();

        long todayStart = Converters.getStartOfDayMillis(new Date());
        int avgCycleLen = Math.round((float) CycleStats.getAverageCycleLength(periods));

        for (Period period : periods) {
            long startMillis = period.startDateMillis;
            if (startMillis == 0) {
                continue;
            }

            int cycleLength = period.cycleLengthDays > 0 ? period.cycleLengthDays : avgCycleLen;
            LocalDate startLocal = Converters.toLocalDate(startMillis);

            for (int i = 0; i < cycleLength; i++) {
                long dayMillis = startMillis + i * DAY_MILLIS;
                CalendarDay calDay = CalendarDay.from(startLocal.plusDays(i));

                CyclePhase phase = getFullPhaseForDate(dayMillis);
                boolean isPremenstrualDay =
                        CycleCalculator.isPremenstrualDay(period, dayMillis, periods, pmsLengthDays);

                switch (phase) {
                    case FOLLICULAR:
                        follicularDays.add(calDay);
                        break;
                    case OVULATION:
                        ovulationDays.add(calDay);
                        break;
                    case LUTEAL:
                        lutealDays.add(calDay);
                        if (isPremenstrualDay) {
                            pmsDays.add(calDay);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        int colorMenstruation = ContextCompat.getColor(requireContext(), R.color.phase_menstruation);
        int colorFollicular = ContextCompat.getColor(requireContext(), R.color.phase_follicular);
        int colorOvulation = ContextCompat.getColor(requireContext(), R.color.phase_ovulation);
        int colorLuteal = ContextCompat.getColor(requireContext(), R.color.phase_luteal);

        for (long[] window : windows.confirmed) {
            for (long day = window[0]; day <= window[1]; day += DAY_MILLIS) {
                menstruationConfirmedDays.add(CalendarDay.from(Converters.toLocalDate(day)));
            }
        }

        for (long[] window : windows.anticipated) {
            for (long day = window[0]; day <= window[1]; day += DAY_MILLIS) {
                if (day < todayStart) {
                    continue;
                }
                menstruationAnticipatedDays.add(CalendarDay.from(Converters.toLocalDate(day)));
            }
        }

        calendarView.removeDecorators();
        List<DayViewDecorator> decorators = new ArrayList<>();

        if (showFollicular && !follicularDays.isEmpty()) {
            decorators.add(new PhaseDecorator(follicularDays, colorFollicular));
        }
        if (!ovulationDays.isEmpty()) {
            decorators.add(new PhaseDecorator(ovulationDays, colorOvulation));
        }
        if (showLuteal && !lutealDays.isEmpty()) {
            decorators.add(new PhaseDecorator(lutealDays, colorLuteal));
        }
        if (!menstruationConfirmedDays.isEmpty()) {
            decorators.add(new FilledStrokePhaseDecorator(
                    menstruationConfirmedDays,
                    showFollicular ? colorFollicular : null,
                    colorMenstruation,
                    dpToPx(2),
                    showFollicular ? getDayTextColor(colorFollicular) : null
            ));
        }
        if (!menstruationAnticipatedDays.isEmpty()) {
            decorators.add(new FilledStrokePhaseDecorator(
                    menstruationAnticipatedDays,
                    showFollicular ? colorFollicular : null,
                    colorMenstruation,
                    dpToPx(1),
                    showFollicular ? getDayTextColor(colorFollicular) : null
            ));
        }
        if (showPms && !pmsDays.isEmpty()) {
            if (showLuteal) {
                decorators.add(new FilledStrokePhaseDecorator(
                        pmsDays,
                        colorLuteal,
                        ContextCompat.getColor(requireContext(), R.color.phase_pms),
                        dpToPx(2),
                        getDayTextColor(colorLuteal)
                ));
            } else {
                Drawable pmsHollow = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pms_hollow_day);
                decorators.add(new HollowPhaseDecorator(pmsDays, pmsHollow));
            }
        }

        int innerOutlineColor = ContextCompat.getColor(requireContext(), R.color.flowberry_surface);

        if (currentSelectedDate != null) {
            CalendarDay selectedDay = CalendarDay.from(
                    Converters.toLocalDate(Converters.getStartOfDayMillis(currentSelectedDate))
            );
            int selectedOuterStrokeColor = ColorUtils.setAlphaComponent(
                    ContextCompat.getColor(requireContext(), R.color.flowberry_primary_variant),
                    90
            );
            DayVisualStyle selectedStyle = resolveDayVisualStyle(
                    selectedDay,
                    showFollicular,
                    showLuteal,
                    showPms,
                    menstruationConfirmedDays,
                    menstruationAnticipatedDays,
                    follicularDays,
                    ovulationDays,
                    lutealDays,
                    pmsDays,
                    colorMenstruation,
                    colorFollicular,
                    colorOvulation,
                    colorLuteal,
                    ContextCompat.getColor(requireContext(), R.color.phase_pms)
            );
            Integer selectedInnerStrokeColor = selectedStyle.fillColor != null
                    ? Integer.valueOf(innerOutlineColor)
                    : selectedStyle.strokeColor;
            Integer selectedTextColor = selectedStyle.fillColor != null
                    ? selectedStyle.textColor
                    : Integer.valueOf(ContextCompat.getColor(
                    requireContext(),
                    R.color.flowberry_text_primary
            ));
            decorators.add(new HighlightedDayDecorator(
                    Collections.singleton(selectedDay),
                    null,
                    selectedOuterStrokeColor,
                    dpToPx(0),
                    selectedStyle.fillColor,
                    selectedInnerStrokeColor,
                    dpToPx(selectedStyle.fillColor != null ? 0 : selectedStyle.strokeWidthDp),
                    selectedTextColor,
                    dpToPx(16),
                    dpToPx(2)
            ));
        }

        calendarView.addDecorators(decorators);
    }

    private void updateFertilityHint(List<Period> periods,
                                     long selectedMillis,
                                     boolean inConfirmedMenstruation,
                                     boolean inAnticipatedMenstruation,
                                     boolean isPremenstrualDay) {
        if (textFertilityHint == null) {
            return;
        }

        boolean showFertilityWarnings = prefs.getBoolean("show_fertility_warnings", true);
        boolean showOvulationAsWanted = prefs.getBoolean("show_ovulation_as_wanted", false);

        if (!showFertilityWarnings || periods == null || periods.isEmpty()) {
            textFertilityHint.setVisibility(View.GONE);
            return;
        }

        if (inConfirmedMenstruation || inAnticipatedMenstruation) {
            applyChip(
                    textFertilityHint,
                    getString(R.string.calendar_hint_period_low_fertility),
                    requireContext().getColor(R.color.phase_menstruation),
                    null,
                    0
            );
            textFertilityHint.setVisibility(View.VISIBLE);
            return;
        }

        if (isPremenstrualDay) {
            applyChip(
                    textFertilityHint,
                    getString(R.string.calendar_hint_pms_low_fertility),
                    requireContext().getColor(R.color.phase_pms),
                    null,
                    0
            );
            textFertilityHint.setVisibility(View.VISIBLE);
            return;
        }

        Period lastPeriod = periodDao.getLastPeriodBefore(selectedMillis);
        boolean isFertileWindow = lastPeriod != null
                && CycleCalculator.isFertileWindowDay(lastPeriod, selectedMillis, periods);

        if (!isFertileWindow) {
            textFertilityHint.setVisibility(View.GONE);
            return;
        }

        if (showOvulationAsWanted) {
            applyChip(
                    textFertilityHint,
                    getString(R.string.calendar_hint_fertile_window),
                    requireContext().getColor(R.color.phase_ovulation),
                    null,
                    R.drawable.ic_fertility_pacifier
            );
        } else {
            applyChip(
                    textFertilityHint,
                    getString(R.string.calendar_hint_fertile_protection),
                    requireContext().getColor(R.color.flowberry_primary),
                    null,
                    R.drawable.ic_fertility_protection
            );
        }
        textFertilityHint.setVisibility(View.VISIBLE);
    }

    private void updateQuickPeriodAction(boolean isTodaySelected,
                                         boolean inConfirmedMenstruation,
                                         boolean inAnticipatedMenstruation,
                                         boolean isPremenstrualDay) {
        if (buttonQuickPeriodAction == null) {
            return;
        }

        currentQuickPeriodAction = QUICK_ACTION_NONE;
        buttonQuickPeriodAction.setVisibility(View.GONE);

        if (!isTodaySelected) {
            return;
        }

        long todayStart = Converters.getStartOfDayMillis(new Date());
        Period openPeriod = periodDao.getLastOpenPeriod();
        if (openPeriod != null && openPeriod.startDateMillis <= todayStart) {
            currentQuickPeriodAction = QUICK_ACTION_END_PERIOD;
            buttonQuickPeriodAction.setText(R.string.calendar_quick_end_period);
            buttonQuickPeriodAction.setVisibility(View.VISIBLE);
            return;
        }

        if (!inConfirmedMenstruation && (inAnticipatedMenstruation || isPremenstrualDay)) {
            currentQuickPeriodAction = QUICK_ACTION_START_PERIOD;
            buttonQuickPeriodAction.setText(R.string.calendar_quick_start_period);
            buttonQuickPeriodAction.setVisibility(View.VISIBLE);
        }
    }

    private void handleQuickPeriodAction() {
        if (currentQuickPeriodAction == QUICK_ACTION_NONE) {
            return;
        }

        long todayStart = Converters.getStartOfDayMillis(new Date());

        try {
            if (currentQuickPeriodAction == QUICK_ACTION_START_PERIOD) {
                PeriodRecorder.recordPeriodStart(requireContext(), db, todayStart);
                Toast.makeText(getContext(), R.string.calendar_quick_start_saved, Toast.LENGTH_SHORT).show();
            } else if (currentQuickPeriodAction == QUICK_ACTION_END_PERIOD) {
                PeriodRecorder.recordPeriodEnd(requireContext(), db, todayStart);
                Toast.makeText(getContext(), R.string.calendar_quick_end_saved, Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalStateException e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        currentSelectedDate = new Date(todayStart);
        updateCalendarDecorators(getView());
        updateSelectedDateUI();
    }

    public Date getCurrentSelectedDate() {
        return currentSelectedDate;
    }

    private void updateSelectedDateUI() {
        if (currentSelectedDate == null) {
            return;
        }

        long selectedMillis = currentSelectedDate.getTime();
        long selectedDayStart = Converters.getStartOfDayMillis(currentSelectedDate);
        long todayStart = Converters.getStartOfDayMillis(new Date());
        initialPeriodPromptOpen = false;
        boolean isTodaySelected = selectedDayStart == todayStart;
        if (isTodaySelected) {
            textSelectedDate.setText(R.string.calendar_today);
        } else {
            textSelectedDate.setText(buildSelectedDateText(new Date(selectedMillis)));
        }

        List<Period> periods = periodDao.getAll();
        boolean hasCompleteCycle = periods != null && periods.size() >= 2;
        if (textPredictionWarning != null) {
            textPredictionWarning.setVisibility(hasCompleteCycle ? View.GONE : View.VISIBLE);
        }
        if (periods == null || periods.isEmpty()) {
            applyChip(
                    textPhase,
                    getString(R.string.calendar_unknown),
                    requireContext().getColor(R.color.flowberry_chip_neutral),
                    null,
                    0
            );
            if (textFertilityHint != null) {
                textFertilityHint.setVisibility(View.GONE);
            }
            if (buttonQuickPeriodAction != null) {
                buttonQuickPeriodAction.setVisibility(View.GONE);
            }
            long startOfDay = Converters.getStartOfDayMillis(currentSelectedDate);
            long endOfDay = Converters.getEndOfDayMillis(currentSelectedDate);
            symptomAdapter.setSymptoms(symptomDao.getByDay(startOfDay, endOfDay));
            return;
        }

        List<Symptom> realSymptoms = symptomDao.getAllReal();
        int pmsLengthDays = CycleCalculator.estimatePremenstrualLengthDays(periods, realSymptoms);
        MenstruationWindows windows = computeMenstruationWindows(periods);
        boolean inConfirmedMenstruation = isInWindows(selectedDayStart, windows.confirmed);
        boolean inAnticipatedMenstruation = !inConfirmedMenstruation
                && isInWindows(selectedDayStart, windows.anticipated)
                && selectedDayStart >= todayStart;

        String label = getString(R.string.calendar_unknown);
        int color = requireContext().getColor(R.color.flowberry_chip_neutral);
        Integer phaseOutlineColor = null;
        CyclePhase phase;
        boolean isPremenstrualDay = false;

        if (inConfirmedMenstruation) {
            phase = CyclePhase.FOLLICULAR;
            label = getString(R.string.calendar_phase_period);
            color = requireContext().getColor(R.color.phase_menstruation);
        } else if (inAnticipatedMenstruation) {
            phase = CyclePhase.FOLLICULAR;
            label = getString(R.string.calendar_phase_estimated_period);
            color = requireContext().getColor(R.color.phase_menstruation);
        } else {
            Period lastPeriod = periodDao.getLastPeriodBefore(selectedMillis);
            phase = CycleCalculator.getPhaseForDate(lastPeriod, selectedMillis, periods);
            isPremenstrualDay = lastPeriod != null
                    && phase == CyclePhase.LUTEAL
                    && CycleCalculator.isPremenstrualDay(lastPeriod, selectedMillis, periods, pmsLengthDays);

            switch (phase) {
                case FOLLICULAR:
                    label = getString(R.string.calendar_phase_follicular);
                    color = requireContext().getColor(R.color.phase_follicular);
                    break;
                case OVULATION:
                    label = getString(R.string.calendar_phase_ovulation);
                    color = requireContext().getColor(R.color.phase_ovulation);
                    break;
                case LUTEAL:
                    label = getString(
                            isPremenstrualDay
                                    ? R.string.calendar_phase_luteal_pms
                                    : R.string.calendar_phase_luteal
                    );
                    color = requireContext().getColor(R.color.phase_luteal);
                    if (isPremenstrualDay) {
                        phaseOutlineColor = requireContext().getColor(R.color.phase_pms);
                    }
                    break;
                default:
                    break;
            }
        }

        applyChip(textPhase, label, color, phaseOutlineColor, 0);
        updateFertilityHint(
                periods,
                selectedMillis,
                inConfirmedMenstruation,
                inAnticipatedMenstruation,
                isPremenstrualDay
        );
        updateQuickPeriodAction(
                isTodaySelected,
                inConfirmedMenstruation,
                inAnticipatedMenstruation,
                isPremenstrualDay
        );

        long startOfDay = Converters.getStartOfDayMillis(currentSelectedDate);
        long endOfDay = Converters.getEndOfDayMillis(currentSelectedDate);
        List<Symptom> symptoms = symptomDao.getByDay(startOfDay, endOfDay);

        if (selectedDayStart >= todayStart) {
            List<Period> allPeriods = periodDao.getAll();
            if (allPeriods != null && !allPeriods.isEmpty()) {
                String phaseName = phase.name();
                Period lastPeriod = periodDao.getLastPeriodBefore(selectedMillis);
                boolean isPremenstrualPredictionDay = lastPeriod != null
                        && phase == CyclePhase.LUTEAL
                        && CycleCalculator.isPremenstrualDay(lastPeriod, selectedMillis, allPeriods, pmsLengthDays);

                SymptomPhasePatternDao patternDao = db.symptomPhasePatternDao();
                List<SymptomPhasePattern> patterns = new ArrayList<>(patternDao.getByPhase(phaseName));
                if (isPremenstrualPredictionDay) {
                    patterns.addAll(patternDao.getByPhase(CyclePhase.PMS.name()));
                }

                int totalCycles = periodDao.getCycleCount();

                for (SymptomPhasePattern pattern : patterns) {
                    if (pattern.cyclesWithSymptom <= 0) {
                        continue;
                    }

                    boolean alreadyPresent = false;
                    for (Symptom real : symptoms) {
                        if (real.type.equals(pattern.symptomType)) {
                            alreadyPresent = true;
                            break;
                        }
                    }
                    if (alreadyPresent) {
                        continue;
                    }

                    int percent = totalCycles > 0
                            ? Math.round(pattern.cyclesWithSymptom * 100f / totalCycles)
                            : 0;

                    Symptom anticipated = new Symptom();
                    anticipated.dateMillis = startOfDay;
                    anticipated.type = pattern.symptomType;
                    anticipated.intensity = pattern.typicalIntensity;
                    anticipated.anticipated = true;
                    anticipated.validated = false;
                    anticipated.notes = getString(
                            R.string.calendar_anticipated_symptom_note,
                            percent
                    );

                    symptoms.add(anticipated);
                }
            }
        }

        symptomAdapter.setSymptoms(symptoms);
    }

    private CharSequence buildSelectedDateText(Date selectedDate) {
        String label = getString(R.string.calendar_selected_date);
        String formattedDate = dateFormat.format(selectedDate);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(label);
        builder.append('\n');
        int dateStart = builder.length();
        builder.append(formattedDate);
        builder.setSpan(
                new StyleSpan(android.graphics.Typeface.BOLD),
                dateStart,
                builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        builder.setSpan(
                new RelativeSizeSpan(1.12f),
                dateStart,
                builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return builder;
    }

    private void applyChip(TextView view,
                           String text,
                           int tintColor,
                           @Nullable Integer strokeColor,
                           int iconRes) {
        view.setText(text);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dpToPx(999));
        background.setColor(tintColor);
        if (strokeColor != null) {
            background.setStroke(dpToPx(2), strokeColor);
        }
        view.setBackground(background);
        int contentColor = ColorUtils.calculateLuminance(tintColor) > 0.5
                ? ContextCompat.getColor(requireContext(), R.color.flowberry_ink)
                : ContextCompat.getColor(requireContext(), R.color.flowberry_cream);
        view.setTextColor(contentColor);

        Drawable icon = null;
        if (iconRes != 0) {
            icon = ContextCompat.getDrawable(requireContext(), iconRes);
            if (icon != null) {
                icon = icon.mutate();
                icon.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN);
            }
        }
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
    }

    private DayVisualStyle resolveDayVisualStyle(CalendarDay day,
                                                 boolean showFollicular,
                                                 boolean showLuteal,
                                                 boolean showPms,
                                                 Set<CalendarDay> menstruationConfirmedDays,
                                                 Set<CalendarDay> menstruationAnticipatedDays,
                                                 Set<CalendarDay> follicularDays,
                                                 Set<CalendarDay> ovulationDays,
                                                 Set<CalendarDay> lutealDays,
                                                 Set<CalendarDay> pmsDays,
                                                 int colorMenstruation,
                                                 int colorFollicular,
                                                 int colorOvulation,
                                                 int colorLuteal,
                                                 int colorPms) {
        if (menstruationConfirmedDays.contains(day)) {
            return new DayVisualStyle(
                    showFollicular ? colorFollicular : null,
                    showFollicular ? getDayTextColor(colorFollicular) : null,
                    colorMenstruation,
                    2
            );
        }
        if (menstruationAnticipatedDays.contains(day)) {
            return new DayVisualStyle(
                    showFollicular ? colorFollicular : null,
                    showFollicular ? getDayTextColor(colorFollicular) : null,
                    colorMenstruation,
                    1
            );
        }
        if (showPms && pmsDays.contains(day)) {
            return new DayVisualStyle(
                    showLuteal ? colorLuteal : null,
                    showLuteal ? getDayTextColor(colorLuteal) : null,
                    colorPms,
                    2
            );
        }
        if (ovulationDays.contains(day)) {
            return new DayVisualStyle(colorOvulation, getDayTextColor(colorOvulation), null, 0);
        }
        if (showLuteal && lutealDays.contains(day)) {
            return new DayVisualStyle(colorLuteal, getDayTextColor(colorLuteal), null, 0);
        }
        if (showFollicular && follicularDays.contains(day)) {
            return new DayVisualStyle(colorFollicular, getDayTextColor(colorFollicular), null, 0);
        }
        return new DayVisualStyle(null, null, null, 0);
    }

    private int getDayTextColor(int backgroundColor) {
        double luminance = androidx.core.graphics.ColorUtils.calculateLuminance(backgroundColor);
        return luminance > 0.5
                ? ContextCompat.getColor(requireContext(), R.color.flowberry_ink)
                : ContextCompat.getColor(requireContext(), R.color.flowberry_cream);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static class MenstruationWindows {
        final List<long[]> confirmed = new ArrayList<>();
        final List<long[]> anticipated = new ArrayList<>();
    }

    private static final class DayVisualStyle {
        @Nullable
        final Integer fillColor;
        @Nullable
        final Integer textColor;
        @Nullable
        final Integer strokeColor;
        final int strokeWidthDp;

        DayVisualStyle(@Nullable Integer fillColor,
                       @Nullable Integer textColor,
                       @Nullable Integer strokeColor,
                       int strokeWidthDp) {
            this.fillColor = fillColor;
            this.textColor = textColor;
            this.strokeColor = strokeColor;
            this.strokeWidthDp = strokeWidthDp;
        }
    }
}
