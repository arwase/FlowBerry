package com.arwase.flowberry.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import org.threeten.bp.LocalDate;
import androidx.core.view.WindowCompat;

import com.arwase.flowberry.R;
import com.arwase.flowberry.decorators.PhaseDecorator;
import com.arwase.flowberry.fragments.CalendarFragment;
import com.arwase.flowberry.fragments.InfoFragment;
import com.arwase.flowberry.fragments.OptionsFragment;
import com.arwase.flowberry.fragments.StatsFragment;
import com.arwase.flowberry.utils.Converters;
import com.arwase.flowberry.adapters.SymptomAdapter;
import com.arwase.flowberry.database.AppDatabase;
import com.arwase.flowberry.database.PeriodDao;
import com.arwase.flowberry.database.SymptomDao;
import com.arwase.flowberry.logic.CycleCalculator;
import com.arwase.flowberry.models.CyclePhase;
import com.arwase.flowberry.models.Period;
import com.arwase.flowberry.models.Symptom;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔴 Important : on dit à Android de garder le contenu SOUS la barre système
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        AndroidThreeTen.init(getApplication());
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(
                getResources().getColor(R.color.purple_500)
        );


        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Onglet par défaut : Calendrier
        if (savedInstanceState == null) {
            showFragment(new CalendarFragment());
            bottomNav.setSelectedItemId(R.id.nav_calendar);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_calendar) {
                showFragment(new CalendarFragment());
                return true;
            } else if (id == R.id.nav_info) {
                showFragment(new InfoFragment());
                return true;
            } else if (id == R.id.nav_stats) {
                showFragment(new StatsFragment());
                return true;
            } else if (id == R.id.nav_settings) {
                showFragment(new OptionsFragment());
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
        // Quand on revient d'EditPeriodActivity, on peut recharger les couleurs, etc.
        //updateCalendarDecorators();
        //updateSelectedDateUI();
    }

}