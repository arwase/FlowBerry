package com.arwase.flowberryapp.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "symptom_types")
public class SymptomType {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;          // "Migraine", "Fatigue", "Nausées"...

    @Nullable
    public String category;      // "Douleur", "Humeur", "Digestion"... (optionnel)

    public boolean userCreated;  // true si ajouté par l'utilisatrice

    public boolean archived;     // pour masquer un symptôme qu'elle ne veut plus voir

}
