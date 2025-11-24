package com.arwase.flowberry.models;

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
    public int intensity;    // 1 à 5 par exemple
    public String notes;     // texte libre
}