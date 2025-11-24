package com.arwase.flowberry.activities;

import static com.arwase.flowberry.utils.Converters.getStartOfDayMillis;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.room.Room;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.arwase.flowberry.R;
import com.arwase.flowberry.database.AppDatabase;
import com.arwase.flowberry.database.SymptomDao;
import com.arwase.flowberry.models.Symptom;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditSymptomActivity extends AppCompatActivity {

    private TextView textDate;
    private EditText editType;
    private EditText editIntensity;
    private EditText editNotes;
    private Button buttonSave;

    private AppDatabase db;
    private SymptomDao symptomDao;

    private Date selectedDate;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔴 Important : on dit à Android de garder le contenu SOUS la barre système
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        setContentView(R.layout.activity_edit_symptom);

        textDate = findViewById(R.id.textEditSymptomDate);
        editType = findViewById(R.id.editSymptomType);
        editIntensity = findViewById(R.id.editSymptomIntensity);
        editNotes = findViewById(R.id.editSymptomNotes);
        buttonSave = findViewById(R.id.buttonSaveSymptom);
        MaterialToolbar topBar = findViewById(R.id.topAppBar);

        topBar.setNavigationOnClickListener(v -> {
            // On annule et on revient simplement à MainActivity
            finish();
        });
        Button buttonCancel = findViewById(R.id.buttonCancelSymptom);

        buttonCancel.setOnClickListener(v -> {
            // On ferme simplement l'écran sans rien sauvegarder
            finish();
        });

        db = AppDatabase.getInstance(this);
        symptomDao = db.symptomDao();

        long dateMillis = getIntent().getLongExtra("date", System.currentTimeMillis());
        selectedDate = new Date(dateMillis);
        textDate.setText("Date : " + dateFormat.format(selectedDate));

        buttonSave.setOnClickListener(v -> saveSymptom());
    }

    private void saveSymptom() {
        String type = editType.getText().toString().trim();
        String intensityStr = editIntensity.getText().toString().trim();
        String notes = editNotes.getText().toString().trim();

        if (type.isEmpty() || intensityStr.isEmpty()) {
            Toast.makeText(this, "Type et intensité sont obligatoires", Toast.LENGTH_SHORT).show();
            return;
        }

        int intensity;
        try {
            intensity = Integer.parseInt(intensityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Intensité invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        if (intensity < 1 || intensity > 5) {
            Toast.makeText(this, "Intensité doit être entre 1 et 5", Toast.LENGTH_SHORT).show();
            return;
        }

        Symptom s = new Symptom();
        s.dateMillis = getStartOfDayMillis(selectedDate); // même idée que plus haut
        s.type = type;
        s.intensity = intensity;
        s.notes = notes;

        symptomDao.insert(s);

        Toast.makeText(this, "Symptôme enregistré", Toast.LENGTH_SHORT).show();
        finish();
    }
}