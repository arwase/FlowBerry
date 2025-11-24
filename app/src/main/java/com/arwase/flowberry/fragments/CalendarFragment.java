package com.arwase.flowberry.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arwase.flowberry.R;
import com.arwase.flowberry.activities.EditPeriodActivity;
import com.arwase.flowberry.activities.EditSymptomActivity;
import com.arwase.flowberry.activities.MainActivity;
import com.arwase.flowberry.adapters.SymptomAdapter;
import com.arwase.flowberry.database.AppDatabase;
import com.arwase.flowberry.database.PeriodDao;
import com.arwase.flowberry.database.SymptomDao;
import com.arwase.flowberry.decorators.PhaseDecorator;
import com.arwase.flowberry.logic.CycleCalculator;
import com.arwase.flowberry.models.CyclePhase;
import com.arwase.flowberry.models.Period;
import com.arwase.flowberry.models.Symptom;
import com.arwase.flowberry.utils.Converters;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import org.threeten.bp.LocalDate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarFragment extends Fragment {

    public CalendarFragment() { }
    private MaterialCalendarView calendarView;
    private TextView textSelectedDate;
    private TextView textPhase;
    private RecyclerView recyclerSymptoms;
    private FloatingActionButton btnAddSymptom;
    private FloatingActionButton btnAddPeriod;

    private AppDatabase db;
    private PeriodDao periodDao;
    private SymptomDao symptomDao;

    private SymptomAdapter symptomAdapter;

    private Date currentSelectedDate;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onResume(){
        super.onResume();

        updateCalendarDecorators();
        updateSelectedDateUI();
    }



    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ici tu recoller tout ce que tu avais dans MainActivity :
        // findViewById sur calendarView, textSelectedDate, textPhase, recycler, boutons…

        // 🔹 Initialisation Room avec le contexte du fragment
        db = AppDatabase.getInstance(requireContext());
        periodDao = db.periodDao();
        symptomDao = db.symptomDao();

        // 🔹 Récupération des vues (attention : view.findViewById, pas getActivity().findViewById)
        calendarView = view.findViewById(R.id.calendarView);
        textSelectedDate = view.findViewById(R.id.textSelectedDate);
        textPhase = view.findViewById(R.id.textPhase);
        recyclerSymptoms = view.findViewById(R.id.recyclerSymptoms);
        btnAddSymptom = view.findViewById(R.id.fabAddSymptom);
        btnAddPeriod = view.findViewById(R.id.fabAddPeriod);

        // Date par défaut : aujourd'hui
        Calendar cal = Calendar.getInstance();
        currentSelectedDate = cal.getTime();

        recyclerSymptoms.setLayoutManager(new LinearLayoutManager(requireContext()));
        symptomAdapter = new SymptomAdapter(null);
        recyclerSymptoms.setAdapter(symptomAdapter);

        // Conversion en LocalDate pour le CalendarView
        LocalDate localDate = LocalDate.of(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
        );
        calendarView.setSelectedDate(CalendarDay.from(localDate));
        updateSelectedDateUI();

        btnAddSymptom.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditSymptomActivity.class);
            intent.putExtra("date", currentSelectedDate.getTime());
            startActivity(intent);
        });

        btnAddPeriod.setOnClickListener(v ->{
            Intent intent = new Intent(requireContext(), EditPeriodActivity.class);
            intent.putExtra("date", currentSelectedDate.getTime());
            startActivity(intent);
        });

        checkInitialPeriod();


        //ensureDemoPeriodExists();
        updateCalendarDecorators();

        // Quand l'utilisateur sélectionne un jour dans le calendrier
        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(MaterialCalendarView widget, CalendarDay date, boolean selected) {
                Calendar c = Calendar.getInstance();
                c.set(date.getYear(), date.getMonth() - 1, date.getDay()); // attention : mois 0-based en Date
                currentSelectedDate = c.getTime();
                updateSelectedDateUI();
            }
        });
    }

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private void checkInitialPeriod() {
        Period last = periodDao.getLastPeriod();
        if (last == null) {
            // Aucune période connue → on demande la date de début des dernières règles
            Intent intent = new Intent(requireContext(), EditPeriodActivity.class);
            intent.putExtra("mode_initial", true);
            startActivity(intent);
        }
    }

    private void ensureDemoPeriodExists() {
        List<Period> all = periodDao.getAll();
        if (all != null && !all.isEmpty()) {
            // Il y a déjà des périodes en base, on ne touche à rien
            return;
        }

        // Sinon on crée une période de test :
        // début des règles = il y a 3 jours
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        long todayStart = c.getTimeInMillis();
        long startDateMillis = todayStart - 3L * 24L * 60L * 60L * 1000L; // J-3

        Period p = new Period();
        p.startDateMillis = startDateMillis;
        p.endDateMillis = null;        // on peut laisser null
        p.cycleLengthDays = 28;        // cycle “classique”
        p.periodLengthDays = 5;        // règles sur 5 jours

        periodDao.insert(p);
    }

    private void updateCalendarDecorators() {
        // Récupérer toutes les périodes connues
        List<Period> periods = periodDao.getAll();
        if (periods == null || periods.isEmpty()) {
            // rien à colorer pour l’instant
            calendarView.removeDecorators();
            return;
        }

        Set<CalendarDay> menstruationDays = new HashSet<>();
        Set<CalendarDay> follicularDays = new HashSet<>();
        Set<CalendarDay> ovulationDays = new HashSet<>();
        Set<CalendarDay> lutealDays = new HashSet<>();
        Set<CalendarDay> pmsDays = new HashSet<>();

        for (Period p : periods) {
            long startMillis = p.startDateMillis;
            if (startMillis == 0) continue;

            int cycleLength = p.cycleLengthDays > 0 ? p.cycleLengthDays : 28;

            // LocalDate du début de cette période
            LocalDate startLocal = Converters.toLocalDate(startMillis);

            for (int i = 0; i < cycleLength; i++) {
                long dayMillis = startMillis + i * DAY_MILLIS;
                LocalDate date = startLocal.plusDays(i);
                CalendarDay calDay = CalendarDay.from(date);

                CyclePhase phase = CycleCalculator.getPhaseForDate(p, dayMillis);

                switch (phase) {
                    case MENSTRUATION:
                        menstruationDays.add(calDay);
                        break;
                    case FOLLICULAR:
                        follicularDays.add(calDay);
                        break;
                    case OVULATION:
                        ovulationDays.add(calDay);
                        break;
                    case LUTEAL:
                        lutealDays.add(calDay);
                        break;
                    case PMS:
                        pmsDays.add(calDay);
                        break;
                    default:
                        // UNKNOWN : on ne colorie pas
                        break;
                }
            }
        }

        int colorMenstruation = ContextCompat.getColor(requireContext(), R.color.phase_menstruation);
        int colorFollicular = ContextCompat.getColor(requireContext(), R.color.phase_follicular);
        int colorOvulation = ContextCompat.getColor(requireContext(), R.color.phase_ovulation);
        int colorLuteal = ContextCompat.getColor(requireContext(), R.color.phase_luteal);
        int colorPms = ContextCompat.getColor(requireContext(), R.color.phase_pms);

        // On enlève les anciennes décorations et on remet les nouvelles
        calendarView.removeDecorators();

        List<PhaseDecorator> decorators = new ArrayList<>();
        if (!menstruationDays.isEmpty()) {
            decorators.add(new PhaseDecorator(menstruationDays, colorMenstruation));
        }
        if (!follicularDays.isEmpty()) {
            decorators.add(new PhaseDecorator(follicularDays, colorFollicular));
        }
        if (!ovulationDays.isEmpty()) {
            decorators.add(new PhaseDecorator(ovulationDays, colorOvulation));
        }
        if (!lutealDays.isEmpty()) {
            decorators.add(new PhaseDecorator(lutealDays, colorLuteal));
        }
        if (!pmsDays.isEmpty()) {
            decorators.add(new PhaseDecorator(pmsDays, colorPms));
        }

        calendarView.addDecorators(decorators);
    }


    private void updateSelectedDateUI() {
        if (currentSelectedDate == null) return;

        textSelectedDate.setText("Date sélectionnée : " + dateFormat.format(currentSelectedDate));

        // 1) Récupérer la dernière période avant cette date
        long timeMillis = currentSelectedDate.getTime();
        Period lastPeriod = periodDao.getLastPeriodBefore(timeMillis);

        CyclePhase phase = CycleCalculator.getPhaseForDate(lastPeriod, timeMillis);
        textPhase.setText("Phase : " + phaseToLabel(phase));

        // 2) Charger les symptômes du jour
        long startOfDay = Converters.getStartOfDayMillis(currentSelectedDate);
        long endOfDay = Converters.getEndOfDayMillis(currentSelectedDate);
        List<Symptom> symptoms = symptomDao.getByDay(startOfDay, endOfDay);

        symptomAdapter.setSymptoms(symptoms);
    }

    private String phaseToLabel(CyclePhase phase) {
        switch (phase) {
            case MENSTRUATION:
                return "Règles";
            case FOLLICULAR:
                return "Phase folliculaire";
            case OVULATION:
                return "Ovulation (estimée)";
            case LUTEAL:
                return "Phase lutéale";
            case PMS:
                return "Syndrome prémenstruel (estimé)";
            default:
                return "Inconnue";
        }
    }
}
