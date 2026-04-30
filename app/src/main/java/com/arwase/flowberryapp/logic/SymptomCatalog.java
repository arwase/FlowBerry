package com.arwase.flowberryapp.logic;

import android.content.Context;

import androidx.annotation.Nullable;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.database.SymptomTypeDao;
import com.arwase.flowberryapp.models.SymptomType;

public final class SymptomCatalog {

    private SymptomCatalog() {
    }

    public static void ensureDefaults(Context context, SymptomTypeDao symptomTypeDao) {
        String[] names = context.getResources().getStringArray(R.array.default_symptom_names);
        String[] categories = context.getResources().getStringArray(R.array.default_symptom_categories);
        int count = Math.min(names.length, categories.length);

        for (int i = 0; i < count; i++) {
            Entry entry = new Entry(names[i], categories[i]);
            SymptomType existing = symptomTypeDao.getByName(entry.name);
            if (existing != null) {
                if (existing.archived && !existing.userCreated) {
                    existing.archived = false;
                    symptomTypeDao.update(existing);
                }
                continue;
            }

            SymptomType symptomType = new SymptomType();
            symptomType.name = entry.name;
            symptomType.category = entry.category;
            symptomType.userCreated = false;
            symptomType.archived = false;
            symptomTypeDao.insert(symptomType);
        }
    }

    public static SymptomType getOrCreate(SymptomTypeDao symptomTypeDao,
                                          String rawName,
                                          @Nullable String category,
                                          boolean userCreated) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            return null;
        }

        SymptomType existing = symptomTypeDao.getByName(name);
        if (existing != null) {
            if (existing.archived) {
                existing.archived = false;
                symptomTypeDao.update(existing);
            }
            return existing;
        }

        SymptomType symptomType = new SymptomType();
        symptomType.name = name;
        symptomType.category = category;
        symptomType.userCreated = userCreated;
        symptomType.archived = false;
        long newId = symptomTypeDao.insert(symptomType);
        symptomType.id = newId;
        return symptomType;
    }

    private static final class Entry {
        final String name;
        final String category;

        Entry(String name, String category) {
            this.name = name;
            this.category = category;
        }
    }
}
