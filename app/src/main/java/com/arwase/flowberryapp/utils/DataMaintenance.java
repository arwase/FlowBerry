package com.arwase.flowberryapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.logic.SymptomCatalog;
import com.arwase.flowberryapp.logic.SymptomPatternTrainer;

public final class DataMaintenance {

    private static final String PREFS_NAME = "flowberry_maintenance";
    private static final String KEY_DATA_VERSION = "data_version";
    private static final int CURRENT_DATA_VERSION = 4;

    private DataMaintenance() {
    }

    public static void runPendingMigrations(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int appliedVersion = prefs.getInt(KEY_DATA_VERSION, 0);

        if (appliedVersion < CURRENT_DATA_VERSION) {
            AppDatabase db = AppDatabase.getInstance(context);
            SymptomCatalog.ensureDefaults(context, db.symptomTypeDao());
            SymptomPatternTrainer.rebuildAllPatterns(db);
            prefs.edit().putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).apply();
        }
    }
}
