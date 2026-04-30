package com.arwase.flowberryapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.database.PeriodDao;
import com.arwase.flowberryapp.logic.PeriodCycleUpdater;
import com.arwase.flowberryapp.logic.PeriodRecorder;
import com.arwase.flowberryapp.models.Period;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Calendar;

public class EditPeriodActivity extends AppCompatActivity {

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private PeriodDao periodDao;
    private AppDatabase db;
    private boolean isInitialMode;
    private long editPeriodId = -1;
    private Period periodToEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isInitialMode = getIntent().getBooleanExtra("mode_initial", false);
        editPeriodId = getIntent().getLongExtra("period_id", -1);

        setContentView(R.layout.activity_edit_period);
        db = AppDatabase.getInstance(this);
        periodDao = db.periodDao();

        RadioGroup radioGroupType = findViewById(R.id.radioGroupType);
        RadioButton radioStart = findViewById(R.id.radioStart);
        RadioButton radioEnd = findViewById(R.id.radioEnd);
        DatePicker datePickerStart = findViewById(R.id.datePickerPeriod);
        DatePicker datePickerEnd = findViewById(R.id.datePickerPeriodEnd);
        CheckBox checkPeriodFinished = findViewById(R.id.checkPeriodFinished);
        Button buttonSave = findViewById(R.id.buttonSavePeriod);
        Button buttonCancel = findViewById(R.id.buttonCancelPeriod);
        MaterialToolbar topBar = findViewById(R.id.topAppBar);
        TextView titleQuestion = findViewById(R.id.textQuestion);
        TextView textExplanation = findViewById(R.id.textExplanation);
        TextView textStartDateLabel = findViewById(R.id.textStartDateLabel);
        TextView textEndDateLabel = findViewById(R.id.textEndDateLabel);

        Calendar today = Calendar.getInstance();
        datePickerStart.updateDate(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );
        datePickerEnd.updateDate(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );

        if (isInitialMode) {
            topBar.setTitle(R.string.edit_period_initial_title);
            titleQuestion.setText(R.string.edit_period_initial_question);
            textExplanation.setVisibility(View.VISIBLE);
            radioStart.setChecked(true);
            radioGroupType.setVisibility(View.GONE);
            textStartDateLabel.setText(R.string.edit_period_start);
            checkPeriodFinished.setVisibility(View.VISIBLE);
            buttonCancel.setText(R.string.later);
        }

        if (editPeriodId != -1) {
            periodToEdit = periodDao.getById(editPeriodId);
            if (periodToEdit != null) {
                titleQuestion.setText(R.string.edit_period_edit_title);
                radioStart.setChecked(true);

                Calendar startCalendar = Calendar.getInstance();
                startCalendar.setTimeInMillis(periodToEdit.startDateMillis);
                datePickerStart.updateDate(
                        startCalendar.get(Calendar.YEAR),
                        startCalendar.get(Calendar.MONTH),
                        startCalendar.get(Calendar.DAY_OF_MONTH)
                );

                radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == radioStart.getId()) {
                        Calendar start = Calendar.getInstance();
                        start.setTimeInMillis(periodToEdit.startDateMillis);
                        datePickerStart.updateDate(
                                start.get(Calendar.YEAR),
                                start.get(Calendar.MONTH),
                                start.get(Calendar.DAY_OF_MONTH)
                        );
                    } else if (checkedId == radioEnd.getId()) {
                        long endMillis = periodToEdit.endDateMillis != null
                                ? periodToEdit.endDateMillis
                                : periodToEdit.startDateMillis;
                        Calendar end = Calendar.getInstance();
                        end.setTimeInMillis(endMillis);
                        datePickerStart.updateDate(
                                end.get(Calendar.YEAR),
                                end.get(Calendar.MONTH),
                                end.get(Calendar.DAY_OF_MONTH)
                        );
                    }
                });
            }
        }

        datePickerStart.setMaxDate(today.getTimeInMillis());
        datePickerEnd.setMaxDate(today.getTimeInMillis());

        if (isInitialMode) {
            checkPeriodFinished.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateInitialEndDateVisibility(
                        datePickerStart,
                        datePickerEnd,
                        checkPeriodFinished,
                        textEndDateLabel
                );
            });

            datePickerStart.init(
                    datePickerStart.getYear(),
                    datePickerStart.getMonth(),
                    datePickerStart.getDayOfMonth(),
                    (view, year, monthOfYear, dayOfMonth) -> updateInitialEndDateVisibility(
                            datePickerStart,
                            datePickerEnd,
                            checkPeriodFinished,
                            textEndDateLabel
                    )
            );

            updateInitialEndDateVisibility(
                    datePickerStart,
                    datePickerEnd,
                    checkPeriodFinished,
                    textEndDateLabel
            );
        }

        buttonSave.setOnClickListener(v -> {
            long startDateMillis = getDatePickerMillis(datePickerStart);

            if (isInitialMode) {
                boolean ongoing = checkPeriodFinished.isChecked();
                boolean hasEnded = !ongoing;

                if (hasEnded) {
                    long endDateMillis = getDatePickerMillis(datePickerEnd);
                    if (endDateMillis < startDateMillis) {
                        Toast.makeText(this, R.string.edit_period_error_end_before_start, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Period period = new Period();
                    period.startDateMillis = startDateMillis;
                    period.endDateMillis = endDateMillis;
                    period.cycleLengthDays = 0;
                    period.periodLengthDays = (int) ((endDateMillis - startDateMillis) / DAY_MILLIS) + 1;
                    periodDao.insert(period);
                    PeriodCycleUpdater.recomputeAllCycleLengths(db);
                } else {
                    try {
                        PeriodRecorder.recordPeriodStart(this, db, startDateMillis);
                    } catch (IllegalStateException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                finish();
                return;
            }

            if (editPeriodId != -1 && periodToEdit != null) {
                int checkedId = radioGroupType.getCheckedRadioButtonId();

                if (checkedId == radioStart.getId()) {
                    if (periodToEdit.endDateMillis != null && startDateMillis > periodToEdit.endDateMillis) {
                        Toast.makeText(this, R.string.edit_period_error_start_after_end, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    periodToEdit.startDateMillis = startDateMillis;
                    if (periodToEdit.endDateMillis != null) {
                        int periodLen = (int) ((periodToEdit.endDateMillis - periodToEdit.startDateMillis) / DAY_MILLIS) + 1;
                        periodToEdit.periodLengthDays = Math.max(1, periodLen);
                    }

                    periodDao.update(periodToEdit);

                    Period previous = periodDao.getLastPeriodBefore(periodToEdit.startDateMillis - 1);
                    if (previous != null) {
                        long diff = periodToEdit.startDateMillis - previous.startDateMillis;
                        int cycleLen = (int) Math.round(diff / (double) DAY_MILLIS);
                        if (cycleLen >= 15 && cycleLen <= 60) {
                            previous.cycleLengthDays = cycleLen;
                            periodDao.update(previous);
                        }
                    }
                    PeriodCycleUpdater.recomputeAllCycleLengths(db);
                } else if (checkedId == radioEnd.getId()) {
                    if (startDateMillis < periodToEdit.startDateMillis) {
                        Toast.makeText(this, R.string.edit_period_error_end_before_start, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    periodToEdit.endDateMillis = startDateMillis;
                    int periodLen = (int) ((periodToEdit.endDateMillis - periodToEdit.startDateMillis) / DAY_MILLIS) + 1;
                    periodToEdit.periodLengthDays = Math.max(1, periodLen);

                    periodDao.update(periodToEdit);
                    PeriodCycleUpdater.recomputeAllCycleLengths(db);
                }

                finish();
                return;
            }

            int checkedId = radioGroupType.getCheckedRadioButtonId();
            try {
                if (checkedId == radioStart.getId()) {
                    PeriodRecorder.recordPeriodStart(this, db, startDateMillis);
                } else if (checkedId == radioEnd.getId()) {
                    PeriodRecorder.recordPeriodEnd(this, db, startDateMillis);
                }
            } catch (IllegalStateException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            finish();
        });

        buttonCancel.setOnClickListener(v -> finish());
        topBar.setNavigationOnClickListener(v -> finish());
    }

    private long getDatePickerMillis(DatePicker datePicker) {
        Calendar selected = Calendar.getInstance();
        selected.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), 0, 0, 0);
        selected.set(Calendar.MILLISECOND, 0);
        return selected.getTimeInMillis();
    }

    private void updateInitialEndDateVisibility(DatePicker datePickerStart,
                                                DatePicker datePickerEnd,
                                                CheckBox checkPeriodFinished,
                                                TextView textEndDateLabel) {
        long startDateMillis = getDatePickerMillis(datePickerStart);
        boolean shouldDefaultToOngoing = shouldDefaultToOngoing(startDateMillis);

        if (!checkPeriodFinished.isPressed()) {
            checkPeriodFinished.setChecked(shouldDefaultToOngoing);
        }

        boolean showEndDate = !checkPeriodFinished.isChecked();
        textEndDateLabel.setVisibility(showEndDate ? View.VISIBLE : View.GONE);
        datePickerEnd.setVisibility(showEndDate ? View.VISIBLE : View.GONE);

        if (showEndDate) {
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.setTimeInMillis(startDateMillis);
            datePickerEnd.setMinDate(startDateMillis);
            if (getDatePickerMillis(datePickerEnd) < startDateMillis) {
                datePickerEnd.updateDate(
                        startCalendar.get(Calendar.YEAR),
                        startCalendar.get(Calendar.MONTH),
                        startCalendar.get(Calendar.DAY_OF_MONTH)
                );
            }
        }
    }

    private boolean shouldDefaultToOngoing(long startDateMillis) {
        long todayStart = getStartOfTodayMillis();
        long diffDays = (todayStart - startDateMillis) / DAY_MILLIS;
        return diffDays >= 0 && diffDays <= 5;
    }

    private long getStartOfTodayMillis() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today.getTimeInMillis();
    }
}
