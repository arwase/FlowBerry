package com.arwase.flowberryapp.activities;

import static com.arwase.flowberryapp.utils.Converters.getStartOfDayMillis;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.database.SymptomDao;
import com.arwase.flowberryapp.database.SymptomTypeDao;
import com.arwase.flowberryapp.logic.SymptomCatalog;
import com.arwase.flowberryapp.logic.SymptomPatternTrainer;
import com.arwase.flowberryapp.models.Symptom;
import com.arwase.flowberryapp.models.SymptomType;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EditSymptomActivity extends AppCompatActivity {
    private long symptomId = -1;
    private boolean isEditMode = false;
    private AutoCompleteTextView autoCompleteSymptom;
    private Slider sliderIntensity;
    private TextInputLayout textInputLayoutDate;
    private TextView editTextDate;
    private TextView textViewIntensityValue;
    private EditText editNotes;
    private Button buttonSave;
    private AppDatabase db;
    private SymptomDao symptomDao;
    private LocalDate selectedDate;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private SymptomTypeDao symptomTypeDao;
    private List<SymptomType> symptomTypes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        db = AppDatabase.getInstance(this);
        symptomDao = db.symptomDao();
        symptomTypeDao = db.symptomTypeDao();

        setContentView(R.layout.activity_edit_symptom);
        editTextDate = findViewById(R.id.editTextDate);
        textInputLayoutDate = findViewById(R.id.textInputLayoutDate);
        textViewIntensityValue = findViewById(R.id.textViewIntensityValue);
        autoCompleteSymptom = findViewById(R.id.autoCompleteSymptom);
        sliderIntensity = findViewById(R.id.editSymptomIntensity);
        editNotes = findViewById(R.id.editSymptomNotes);
        buttonSave = findViewById(R.id.buttonSaveSymptom);
        MaterialToolbar topBar = findViewById(R.id.topAppBar);
        Button buttonAddSymptomType = findViewById(R.id.buttonAddSymptomType);
        Button buttonCancel = findViewById(R.id.buttonCancelSymptom);

        setupSymptomDropdown();
        setupDateField();
        setupIntensitySlider();

        long dateExtra = getIntent().getLongExtra("date", -1L);
        symptomId = getIntent().getLongExtra("symptom_id", -1L);

        if (symptomId != -1L) {
            if (topBar != null) {
                topBar.setTitle(R.string.edit_symptom_edit_title);
            }
            isEditMode = true;
            Symptom existing = symptomDao.getById(symptomId);
            if (existing != null) {
                LocalDate localDate = Instant
                        .ofEpochMilli(existing.dateMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                selectedDate = localDate;

                editTextDate.setText(dateFormat.format(new Date(existing.dateMillis)));
                autoCompleteSymptom.setText(existing.type, false);
                sliderIntensity.setValue(existing.intensity);
                updateIntensityLabel(existing.intensity);
                editNotes.setText(existing.notes != null ? existing.notes : "");
            }
        } else {
            LocalDate baseLocalDate;
            if (dateExtra > 0) {
                baseLocalDate = Instant.ofEpochMilli(dateExtra).atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                baseLocalDate = LocalDate.now();
            }

            selectedDate = baseLocalDate;
            long millis = baseLocalDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            editTextDate.setText(dateFormat.format(new Date(millis)));

            if (topBar != null) {
                topBar.setTitle(R.string.edit_symptom_add_title);
            }
        }

        buttonAddSymptomType.setOnClickListener(v -> showCreateSymptomTypeDialog());

        if (topBar != null) {
            topBar.setNavigationOnClickListener(v -> finish());
        }
        buttonCancel.setOnClickListener(v -> finish());
        buttonSave.setOnClickListener(v -> saveSymptom());
    }

    private void setupSymptomDropdown() {
        symptomTypes = symptomTypeDao.getAllActive();
        List<String> names = new ArrayList<>();
        for (SymptomType t : symptomTypes) {
            names.add(t.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                names
        );
        autoCompleteSymptom.setAdapter(adapter);
    }

    private void setupIntensitySlider() {
        sliderIntensity.setValue(3f);
        updateIntensityLabel(3);

        sliderIntensity.addOnChangeListener((slider, value, fromUser) -> {
            int intValue = Math.round(value);
            slider.setValue(intValue);
            updateIntensityLabel(intValue);
        });
    }

    private void showCreateSymptomTypeDialog() {
        final EditText editText = new EditText(this);
        editText.setHint(R.string.edit_symptom_new_dialog_hint);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_symptom_new_dialog_title)
                .setView(editText)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String name = editText.getText().toString().trim();
                    if (name.isEmpty()) {
                        return;
                    }

                    SymptomType existing = symptomTypeDao.getByName(name);
                    if (existing != null) {
                        autoCompleteSymptom.setText(existing.name, false);
                        return;
                    }

                    SymptomCatalog.getOrCreate(symptomTypeDao, name, null, true);
                    setupSymptomDropdown();
                    autoCompleteSymptom.setText(name, false);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateIntensityLabel(int value) {
        textViewIntensityValue.setText(getString(R.string.edit_symptom_intensity_value, value));
    }

    private void setupDateField() {
        long dateMillis = getIntent().getLongExtra("date", System.currentTimeMillis());
        Instant instant = Instant.ofEpochMilli(dateMillis);
        selectedDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

        updateDateText();

        View.OnClickListener dateClickListener = v -> showDatePicker();
        editTextDate.setOnClickListener(dateClickListener);
        textInputLayoutDate.setEndIconOnClickListener(dateClickListener);
    }

    private void updateDateText() {
        if (selectedDate == null) {
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        editTextDate.setText(selectedDate.format(formatter));
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText(R.string.edit_symptom_choose_date)
                        .setSelection(selectedDate
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli())
                        .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Instant instant = Instant.ofEpochMilli(selection);
            selectedDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            updateDateText();
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER_SYMPTOM");
    }

    private void saveSymptom() {
        String symptomType = autoCompleteSymptom.getText().toString().trim();
        int intensity = Math.round(sliderIntensity.getValue());
        String notes = editNotes.getText().toString().trim();

        if (symptomType.isEmpty() || intensity == 0) {
            Toast.makeText(this, R.string.edit_symptom_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (intensity < 1 || intensity > 5) {
            Toast.makeText(this, R.string.edit_symptom_invalid_intensity, Toast.LENGTH_SHORT).show();
            return;
        }

        LocalDate localDate = selectedDate;
        Instant instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        long dayStart = getStartOfDayMillis(new Date(instant.toEpochMilli()));

        Symptom previousSymptom = null;
        String previousType = null;
        long previousDateMillis = -1L;

        Symptom symptom;
        if (isEditMode) {
            symptom = symptomDao.getById(symptomId);
            if (symptom == null) {
                symptom = new Symptom();
                symptom.id = symptomId;
            } else {
                previousSymptom = copySymptom(symptom);
                previousType = previousSymptom.type;
                previousDateMillis = previousSymptom.dateMillis;
            }
        } else {
            symptom = new Symptom();
        }

        symptom.dateMillis = dayStart;
        symptom.type = symptomType;
        SymptomType resolvedType = SymptomCatalog.getOrCreate(symptomTypeDao, symptomType, null, true);
        symptom.typeId = resolvedType != null ? resolvedType.id : null;
        symptom.intensity = intensity;
        symptom.notes = notes;
        symptom.anticipated = false;
        symptom.validated = true;

        if (isEditMode) {
            symptomDao.update(symptom);
            if (previousSymptom != null
                    && (!Objects.equals(previousType, symptom.type)
                    || previousDateMillis != symptom.dateMillis)) {
                SymptomPatternTrainer.removeRealSymptom(db, previousSymptom);
                SymptomPatternTrainer.recordRealSymptom(db, symptom);
            }
        } else {
            symptomDao.insert(symptom);
            SymptomPatternTrainer.recordRealSymptom(db, symptom);
        }

        Toast.makeText(
                this,
                isEditMode ? R.string.edit_symptom_updated : R.string.edit_symptom_saved,
                Toast.LENGTH_SHORT
        ).show();
        finish();
    }

    private Symptom copySymptom(Symptom source) {
        Symptom copy = new Symptom();
        copy.id = source.id;
        copy.dateMillis = source.dateMillis;
        copy.type = source.type;
        copy.typeId = source.typeId;
        copy.intensity = source.intensity;
        copy.notes = source.notes;
        copy.anticipated = source.anticipated;
        copy.validated = source.validated;
        return copy;
    }
}
