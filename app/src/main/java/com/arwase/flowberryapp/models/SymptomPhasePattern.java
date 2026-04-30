package com.arwase.flowberryapp.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// com.arwase.models.flowberryapp.SymptomPhasePattern.java
@Entity(tableName = "symptom_phase_patterns",
        indices = {@Index(value = {"symptomType", "phase"}, unique = true)})
public class SymptomPhasePattern {

    @PrimaryKey(autoGenerate = true)
    public long id;

    // ex : "Maux de ventre", "Migraine"
    @NonNull
    public String symptomType;

    // ex : "MENSTRUATION", "PMS", "OVULATION"…
    @NonNull
    public String phase;

    // Optionnel : intensité moyenne ou typique
    public int typicalIntensity;
    // nombre de cycles où ce symptôme a été observé (au moins une fois)
    public int cyclesWithSymptom;

}
