package com.arwase.flowberryapp.activities;

import android.Manifest;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.fragments.CalendarFragment;
import com.arwase.flowberryapp.fragments.InfoFragment;
import com.arwase.flowberryapp.fragments.OptionsFragment;
import com.arwase.flowberryapp.fragments.StatsFragment;
import com.arwase.flowberryapp.notifications.ImportantNotificationScheduler;
import com.arwase.flowberryapp.utils.DataMaintenance;
import com.arwase.flowberryapp.utils.LanguageManager;
import com.arwase.flowberryapp.utils.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jakewharton.threetenabp.AndroidThreeTen;

import java.util.Date;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LanguageManager.applySavedLanguage(this);
        ThemeManager.applyTheme(this);
        // 🔴 Important : on dit à Android de garder le contenu SOUS la barre système
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        AndroidThreeTen.init(getApplication());
        DataMaintenance.runPendingMigrations(this);
        ImportantNotificationScheduler.syncScheduling(this);
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(
                getResources().getColor(R.color.flowberry_status_bar)
        );


        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton btnAddSymptom = findViewById(R.id.fabAddSymptom);
        FloatingActionButton btnAddPeriod = findViewById(R.id.fabAddPeriod);
        btnAddPeriod.setRippleColor(getResources().getColor(R.color.flowberry_divider));
        // Onglet par défaut : Calendrier
        if (savedInstanceState == null) {
            showFragment(new CalendarFragment());
            bottomNav.setSelectedItemId(R.id.nav_calendar);
        }
        updateFabVisibility(
                getSupportFragmentManager().findFragmentById(R.id.fragmentContainer)
        );

        btnAddSymptom.setOnClickListener(v -> {
            Date baseDate = new Date(); // par défaut : aujourd’hui

            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (f instanceof CalendarFragment) {
                Date selected = ((CalendarFragment) f).getCurrentSelectedDate();
                if (selected != null) {
                    baseDate = selected;
                }
            }

            Intent intent = new Intent(this, EditSymptomActivity.class);
            intent.putExtra("date", baseDate.getTime());

            startActivity(intent);
        });

        btnAddPeriod.setOnClickListener(v ->{
            Date baseDate = new Date(); // par défaut : aujourd’hui

            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (f instanceof CalendarFragment) {
                Date selected = ((CalendarFragment) f).getCurrentSelectedDate();
                if (selected != null) {
                    baseDate = selected;
                }
            }

            Intent intent = new Intent(this, EditPeriodActivity.class);
            intent.putExtra("date", baseDate.getTime());
            startActivity(intent);
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_calendar) {
                Fragment fragment = new CalendarFragment();
                showFragment(fragment);
                updateFabVisibility(fragment);
                return true;
            } else if (id == R.id.nav_info) {
                Fragment fragment = new InfoFragment();
                showFragment(fragment);
                updateFabVisibility(fragment);
                return true;
            } else if (id == R.id.nav_stats) {
                Fragment fragment = new StatsFragment();
                showFragment(fragment);
                updateFabVisibility(fragment);
                return true;
            } else if (id == R.id.nav_settings) {
                Fragment fragment = new OptionsFragment();
                showFragment(fragment);
                updateFabVisibility(fragment);
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ImportantNotificationScheduler.syncScheduling(this);
        requestNotificationPermissionIfNeeded();
        updateFabVisibility(getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
        // Quand on revient d'EditPeriodActivity, on peut recharger les couleurs, etc.
        //updateCalendarDecorators();
        //updateSelectedDateUI();
    }

    private void updateFabVisibility(Fragment fragment) {
        int visibility = fragment instanceof CalendarFragment ? android.view.View.VISIBLE : android.view.View.GONE;
        findViewById(R.id.fabContainer).setVisibility(visibility);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(
                ImportantNotificationScheduler.PREFS_NAME,
                Context.MODE_PRIVATE
        );
        boolean enabled = prefs.getBoolean(ImportantNotificationScheduler.KEY_IMPORTANT_NOTIFICATIONS, false);
        if (!enabled) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
    }

}
