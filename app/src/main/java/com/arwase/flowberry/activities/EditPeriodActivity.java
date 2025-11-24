package com.arwase.flowberry.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigationevent.NavigationEventInfo;

import com.arwase.flowberry.R;
import com.arwase.flowberry.database.AppDatabase;
import com.arwase.flowberry.database.PeriodDao;
import com.arwase.flowberry.models.Period;
import com.google.android.material.appbar.MaterialToolbar;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.threeten.bp.LocalDate;

import java.util.Calendar;

public class EditPeriodActivity extends AppCompatActivity {

    private PeriodDao periodDao;
    private AppDatabase db;
    private RadioGroup radioGroupType;
    private RadioButton radioStart, radioEnd;
    private MaterialCalendarView inlineCalendar;
    private boolean isInitialMode;
    private long editPeriodId = -1;
    private Period periodToEdit;
    private long selectedDateMillis = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isInitialMode = getIntent().getBooleanExtra("mode_initial", false);
        editPeriodId = getIntent().getLongExtra("period_id", -1);

        setContentView(R.layout.activity_edit_period);
        db = AppDatabase.getInstance(this);
        periodDao = db.periodDao();

        radioGroupType = findViewById(R.id.radioGroupType);
        radioStart = findViewById(R.id.radioStart);
        radioEnd = findViewById(R.id.radioEnd);
        inlineCalendar = findViewById(R.id.inlineCalendar);
        Button buttonSave = findViewById(R.id.buttonSavePeriod);
        Button buttonCancel = findViewById(R.id.buttonCancelPeriod);
        MaterialToolbar topBar = findViewById(R.id.topAppBar);
        TextView titleQuestion = findViewById(R.id.textQuestion); // on va ajouter cet id
        TextView textExplanation = findViewById(R.id.textExplanation); // on va ajouter cet id

        // Pré-remplir avec la date du jour
        LocalDate today = LocalDate.now();
        inlineCalendar.setSelectedDate(CalendarDay.from(today));
        selectedDateMillis = toMillis(today);
        inlineCalendar.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) {
                selectedDateMillis = toMillis(LocalDate.of(date.getYear(), date.getMonth(), date.getDay()));
            }
        });


        if (isInitialMode) {
            // Mode première ouverture : on ne demande QUE le début des dernières règles
            topBar.setTitle("Première connexion");
            titleQuestion.setText("Date de début de tes dernières règles");
            textExplanation.setVisibility(View.VISIBLE);
            radioStart.setChecked(true);
            radioGroupType.setVisibility(View.GONE); // on masque le choix Début/Fin
            //topBar.setNavigationIcon(NavigationEventInfo.None);
            buttonCancel.setText("Plus tard");
        }

        // Mode édition : on charge le cycle
        if (editPeriodId != -1) {
            periodToEdit = periodDao.getById(editPeriodId);
            if (periodToEdit != null) {
                titleQuestion.setText("Modifier ce cycle");

                // par défaut, on sélectionne "Début des règles"
                radioStart.setChecked(true);
                setupEditMode();
            }
        }

        buttonSave.setOnClickListener(v -> savePeriod());
        buttonCancel.setOnClickListener(v -> finish());
        topBar.setNavigationOnClickListener(v -> {
            // On annule et on revient simplement à MainActivity
            finish();
        });

    }
    private void setupEditMode() {
        if (periodToEdit == null) return;

        radioGroupType.setVisibility(View.VISIBLE);

        // Radio par défaut = début
        radioStart.setChecked(true);
        selectedDateMillis = periodToEdit.startDateMillis;
        inlineCalendar.setSelectedDate(toCalendarDay(selectedDateMillis));

        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == radioStart.getId()) {
                selectedDateMillis = periodToEdit.startDateMillis;
                inlineCalendar.setSelectedDate(toCalendarDay(selectedDateMillis));
            } else if (checkedId == radioEnd.getId()) {
                long endMillis = periodToEdit.endDateMillis != null ?
                        periodToEdit.endDateMillis :
                        periodToEdit.startDateMillis;

                selectedDateMillis = endMillis;
                inlineCalendar.setSelectedDate(toCalendarDay(selectedDateMillis));
            }
        });
    }

    /**
     * Sauvegarde ou édition
     */
    private void savePeriod() {

        // MODE INITIAL
        if (isInitialMode) {
            Period p = new Period();
            p.startDateMillis = selectedDateMillis;
            p.endDateMillis = null;
            periodDao.insert(p);
            finish();
            return;
        }

        // MODE ÉDITION
        if (editPeriodId != -1 && periodToEdit != null) {
            if (radioStart.isChecked()) {
                if (periodToEdit.endDateMillis != null &&
                        selectedDateMillis > periodToEdit.endDateMillis) {
                    Toast.makeText(this, "Le début ne peut pas être après la fin", Toast.LENGTH_SHORT).show();
                    return;
                }
                periodToEdit.startDateMillis = selectedDateMillis;
            } else {
                if (selectedDateMillis < periodToEdit.startDateMillis) {
                    Toast.makeText(this, "La fin ne peut pas être avant le début", Toast.LENGTH_SHORT).show();
                    return;
                }
                periodToEdit.endDateMillis = selectedDateMillis;
            }

            periodDao.update(periodToEdit);
            finish();
            return;
        }

        // MODE NORMAL (déclaration début / fin)
        if (radioStart.isChecked()) {
            Period p = new Period();
            p.startDateMillis = selectedDateMillis;
            p.endDateMillis = null;
            periodDao.insert(p);
        } else {
            Period lastOpen = periodDao.getLastOpenPeriod();
            if (lastOpen == null) {
                Toast.makeText(this, "Aucune période en cours trouvée", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedDateMillis < lastOpen.startDateMillis) {
                Toast.makeText(this, "La fin ne peut pas être avant le début", Toast.LENGTH_SHORT).show();
                return;
            }
            lastOpen.endDateMillis = selectedDateMillis;
            periodDao.update(lastOpen);
        }

        finish();
    }

    /** Utils */
    private long toMillis(LocalDate date) {
        Calendar c = Calendar.getInstance();
        c.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth(), 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private CalendarDay toCalendarDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return CalendarDay.from(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

}
