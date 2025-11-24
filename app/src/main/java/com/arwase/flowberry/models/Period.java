package com.arwase.flowberry.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "periods")
public class Period {

    @PrimaryKey(autoGenerate = true)
    public long id;

    // Début des règles (timestamp en millis)
    public long startDateMillis;

    // Fin des règles (peut être null → utiliser Long)
    public Long endDateMillis;

    // Longueur de cycle estimée (en jours, ex : 28)
    public int cycleLengthDays;

    // Durée des règles estimée (en jours, ex : 5)
    public int periodLengthDays;
}
