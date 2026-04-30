package com.arwase.flowberryapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class ThemeManager {


    public static final String KEY_THEME_MODE = "pref_theme_mode";
    public static final int THEME_FOLLOW_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static void applyTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // 🔁 MIGRATION : si une ancienne valeur int existe, on la convertit en String
        Object raw = prefs.getAll().get(KEY_THEME_MODE);
        if (raw instanceof Integer) {
            prefs.edit()
                    .putString(KEY_THEME_MODE, String.valueOf(raw))
                    .apply();
        }

        int mode = getThemeMode(context);
        applyNightMode(mode);
    }

    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // ✅ on stocke toujours une STRING, comme ListPreference
        prefs.edit()
                .putString(KEY_THEME_MODE, String.valueOf(mode))
                .apply();

        applyNightMode(mode);
    }

    public static int getThemeMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // ✅ on lit une STRING, jamais un int
        String value = prefs.getString(KEY_THEME_MODE, String.valueOf(THEME_FOLLOW_SYSTEM));

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return THEME_FOLLOW_SYSTEM;
        }
    }

    private static void applyNightMode(int mode) {
        switch (mode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_FOLLOW_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
