package com.arwase.flowberry.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.arwase.flowberry.models.Period;
import com.arwase.flowberry.models.Symptom;
import com.arwase.flowberry.utils.Converters;

@Database(
        entities = {Period.class, Symptom.class},
        version = 1,
        exportSchema = false
)

public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public abstract PeriodDao periodDao();
    public abstract SymptomDao symptomDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "flowberry-db"
                            )
                            // pour éviter les erreurs de migration en dev
                            .fallbackToDestructiveMigration()
                            // pour l’instant on autorise les requêtes sur le thread principal
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}