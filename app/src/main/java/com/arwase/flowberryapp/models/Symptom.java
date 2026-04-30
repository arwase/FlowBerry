package com.arwase.flowberryapp.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "symptoms")
public class Symptom {

    @PrimaryKey(autoGenerate = true)
    public long id;

    // Date du symptôme (timestamp millis, généralement à minuit)
    public long dateMillis;

    public String type;      // ex: "douleurs", "migraines", "fatigue"

    // Référence vers SymptomType
    public Long typeId;      // Long (nullable au début le temps de migrer)

    public int intensity;    // 1 à 5 par exemple
    public String notes;     // texte libre
    public boolean anticipated;   // symptôme prévu à l’avance
    public boolean validated;     // déjà validé ou pas
}
