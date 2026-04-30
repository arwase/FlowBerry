package com.arwase.flowberryapp.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.activities.PrivacyPolicyActivity;
import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.logic.CalendarBackupManager;
import com.arwase.flowberryapp.logic.SymptomPatternTrainer;
import com.arwase.flowberryapp.notifications.ImportantNotificationScheduler;
import com.arwase.flowberryapp.utils.LanguageManager;
import com.arwase.flowberryapp.utils.ThemeManager;

import java.io.IOException;

public class OptionsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREFS_NAME = "flowberry_prefs";

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted && isAdded()) {
                    Toast.makeText(
                            requireContext(),
                            R.string.settings_notifications_permission_toast,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

    private final ActivityResultLauncher<String[]> importBackupLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null || !isAdded()) {
                    return;
                }

                try {
                    CalendarBackupManager.ImportResult result =
                            CalendarBackupManager.importFromUri(requireContext(), uri);
                    Toast.makeText(requireContext(), R.string.backup_import_success, Toast.LENGTH_SHORT).show();
                    Toast.makeText(
                            requireContext(),
                            getString(
                                    R.string.backup_import_success_details,
                                    result.periodsCount,
                                    result.symptomsCount,
                                    result.symptomTypesCount
                            ),
                            Toast.LENGTH_LONG
                    ).show();
                } catch (IllegalArgumentException e) {
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Toast.makeText(requireContext(), R.string.backup_import_error_generic, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(PREFS_NAME);
        getPreferenceManager().setSharedPreferencesMode(android.content.Context.MODE_PRIVATE);
        setPreferencesFromResource(R.xml.fragment_options, rootKey);

        ListPreference themePref = findPreference("pref_theme_mode");
        if (themePref != null) {
            themePref.setOnPreferenceChangeListener(this);
            int currentMode = ThemeManager.getThemeMode(requireContext());
            themePref.setValue(String.valueOf(currentMode));
        }

        ListPreference languagePref = findPreference(LanguageManager.KEY_APP_LANGUAGE);
        if (languagePref != null) {
            languagePref.setOnPreferenceChangeListener(this);
            languagePref.setValue(LanguageManager.getSavedLanguage(requireContext()));
        }

        Preference rebuildPatternsPref = findPreference("pref_rebuild_patterns");
        if (rebuildPatternsPref != null) {
            rebuildPatternsPref.setOnPreferenceClickListener(preference -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                SymptomPatternTrainer.rebuildAllPatterns(db);
                Toast.makeText(requireContext(), R.string.settings_rebuild_predictions_done, Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        Preference exportBackupPref = findPreference("pref_export_calendar_backup");
        if (exportBackupPref != null) {
            exportBackupPref.setOnPreferenceClickListener(preference -> {
                try {
                    startActivity(CalendarBackupManager.createShareIntent(requireContext()));
                    Toast.makeText(requireContext(), R.string.backup_export_success, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(requireContext(), R.string.backup_export_error, Toast.LENGTH_LONG).show();
                }
                return true;
            });
        }

        Preference importBackupPref = findPreference("pref_import_calendar_backup");
        if (importBackupPref != null) {
            importBackupPref.setOnPreferenceClickListener(preference -> {
                importBackupLauncher.launch(new String[]{"application/json", "text/plain", "*/*"});
                return true;
            });
        }

        Preference privacyPolicyPref = findPreference("pref_privacy_policy");
        if (privacyPolicyPref != null) {
            privacyPolicyPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireContext(), PrivacyPolicyActivity.class));
                return true;
            });
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ("pref_theme_mode".equals(preference.getKey())) {
            int mode = Integer.parseInt((String) newValue);
            ThemeManager.setThemeMode(requireContext(), mode);
            requireActivity().recreate();
            return true;
        }
        if (LanguageManager.KEY_APP_LANGUAGE.equals(preference.getKey())) {
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            if (prefs != null) {
                prefs.edit()
                        .putString(LanguageManager.KEY_APP_LANGUAGE, (String) newValue)
                        .apply();
            }
            if (preference instanceof ListPreference) {
                ((ListPreference) preference).setValue((String) newValue);
            }
            LanguageManager.setLanguage(requireContext(), (String) newValue);
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
        applyBottomPadding();
    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!ImportantNotificationScheduler.KEY_IMPORTANT_NOTIFICATIONS.equals(key) || !isAdded()) {
            return;
        }

        boolean enabled = sharedPreferences.getBoolean(key, false);
        ImportantNotificationScheduler.syncScheduling(requireContext(), enabled);

        if (enabled) {
            requestNotificationPermissionIfNeeded();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void applyBottomPadding() {
        RecyclerView listView = getListView();
        if (listView == null) {
            return;
        }

        int bottomPadding = Math.round(120 * getResources().getDisplayMetrics().density);
        listView.setClipToPadding(false);
        listView.setPadding(
                listView.getPaddingLeft(),
                listView.getPaddingTop(),
                listView.getPaddingRight(),
                bottomPadding
        );
    }
}
