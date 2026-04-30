package com.arwase.flowberryapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.arwase.flowberryapp.fragments.OptionsFragment;

public final class LanguageManager {

    public static final String KEY_APP_LANGUAGE = "pref_app_language";
    public static final String LANGUAGE_SYSTEM = "system";
    public static final String LANGUAGE_FRENCH = "fr";
    public static final String LANGUAGE_ENGLISH = "en";

    private LanguageManager() {
    }

    public static void applySavedLanguage(Context context) {
        AppCompatDelegate.setApplicationLocales(toLocales(context, getSavedLanguage(context)));
    }

    public static void setLanguage(Context context, String language) {
        AppCompatDelegate.setApplicationLocales(toLocales(context, language));
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                OptionsFragment.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        return prefs.getString(KEY_APP_LANGUAGE, LANGUAGE_SYSTEM);
    }

    public static String getCurrentLanguageValue() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return LANGUAGE_SYSTEM;
        }

        String tags = locales.toLanguageTags();
        if (tags == null || tags.isEmpty()) {
            return LANGUAGE_SYSTEM;
        }

        String primaryLanguage = tags.split(",")[0];
        if (primaryLanguage.startsWith(LANGUAGE_FRENCH)) {
            return LANGUAGE_FRENCH;
        }
        if (primaryLanguage.startsWith(LANGUAGE_ENGLISH)) {
            return LANGUAGE_ENGLISH;
        }
        return LANGUAGE_SYSTEM;
    }

    private static LocaleListCompat toLocales(Context context, String language) {
        if (LANGUAGE_FRENCH.equals(language) || LANGUAGE_ENGLISH.equals(language)) {
            return LocaleListCompat.forLanguageTags(language);
        }
        String systemLanguage = LocaleListCompat.getAdjustedDefault().toLanguageTags();
        if (systemLanguage != null && systemLanguage.startsWith(LANGUAGE_FRENCH)) {
            return LocaleListCompat.forLanguageTags(LANGUAGE_FRENCH);
        }
        return LocaleListCompat.forLanguageTags(LANGUAGE_ENGLISH);
    }
}
