package com.arwase.flowberryapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.activities.EditPeriodActivity;
import com.arwase.flowberryapp.adapters.PeriodAdapter;
import com.arwase.flowberryapp.database.AppDatabase;
import com.arwase.flowberryapp.database.PeriodDao;
import com.arwase.flowberryapp.logic.PeriodCycleUpdater;
import com.arwase.flowberryapp.logic.SymptomPatternTrainer;
import com.arwase.flowberryapp.models.CycleStats;
import com.arwase.flowberryapp.models.Period;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private PeriodDao periodDao;

    private TextView textCycleAvg;
    private TextView textPeriodAvg;
    private TextView textNextPeriod;
    private RecyclerView recyclerPeriods;
    private PeriodAdapter periodAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textCycleAvg = view.findViewById(R.id.textCycleAvg);
        textPeriodAvg = view.findViewById(R.id.textPeriodAvg);
        textNextPeriod = view.findViewById(R.id.textNextPeriod);
        recyclerPeriods = view.findViewById(R.id.recyclerPeriods);

        recyclerPeriods.setHasFixedSize(true);
        recyclerPeriods.setLayoutManager(new LinearLayoutManager(requireContext()));

        periodAdapter = new PeriodAdapter(new PeriodAdapter.OnPeriodActionListener() {
            @Override
            public void onEdit(Period period) {
                openEditPeriod(period.id);
            }

            @Override
            public void onDelete(Period period) {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                periodDao.delete(period);
                PeriodCycleUpdater.recomputeAllCycleLengths(db);
                SymptomPatternTrainer.rebuildAllPatterns(db);
                refreshData();
            }
        });
        recyclerPeriods.setAdapter(periodAdapter);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        periodDao = db.periodDao();
    }

    private void openEditPeriod(long periodId) {
        Intent intent = new Intent(requireContext(), EditPeriodActivity.class);
        intent.putExtra("period_id", periodId);
        startActivity(intent);
    }

    private void refreshData() {
        List<Period> periods = periodDao.getAll();
        if (periods == null || periods.isEmpty()) {
            textCycleAvg.setText(getString(
                    R.string.stats_cycle_average_fallback,
                    getString(R.string.stats_cycle_average_default, 28)
            ));
            textPeriodAvg.setText(getString(
                    R.string.stats_period_average_fallback,
                    getString(R.string.stats_period_average_default, 5)
            ));
            textNextPeriod.setText(R.string.stats_next_period_empty);
            periodAdapter.setPeriods(periods);
            return;
        }

        double avgCycle = CycleStats.getAverageCycleLength(periods);
        double avgPeriod = CycleStats.getAveragePeriodLength(periods);
        long nextPeriodMillis = CycleStats.predictNextPeriodStart(periods);

        final int defaultCycleLen = 28;
        final int defaultPeriodLen = 5;

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (avgCycle <= 0) {
            textCycleAvg.setText(getString(
                    R.string.stats_cycle_average_fallback,
                    getString(R.string.stats_cycle_average_default, defaultCycleLen)
            ));
        } else {
            textCycleAvg.setText(getString(R.string.stats_cycle_average_value, avgCycle));
        }

        if (avgPeriod <= 0) {
            textPeriodAvg.setText(getString(
                    R.string.stats_period_average_fallback,
                    getString(R.string.stats_period_average_default, defaultPeriodLen)
            ));
        } else {
            textPeriodAvg.setText(getString(R.string.stats_period_average_value, avgPeriod));
        }

        if (nextPeriodMillis <= 0) {
            long lastStart = periods.get(0).startDateMillis;
            for (Period p : periods) {
                if (p.startDateMillis > lastStart) {
                    lastStart = p.startDateMillis;
                }
            }
            nextPeriodMillis = lastStart + defaultCycleLen * CycleStats.DAY_MILLIS;
        }

        if (nextPeriodMillis > 0) {
            textNextPeriod.setText(getString(
                    R.string.stats_next_period_value,
                    df.format(new Date(nextPeriodMillis))
            ));
        } else {
            textNextPeriod.setText(R.string.stats_next_period_empty);
        }

        periodAdapter.setPeriods(periods);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }
}
