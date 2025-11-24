package com.arwase.flowberry.fragments;
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

import com.arwase.flowberry.R;
import com.arwase.flowberry.activities.EditPeriodActivity;
import com.arwase.flowberry.adapters.PeriodAdapter;
import com.arwase.flowberry.database.AppDatabase;
import com.arwase.flowberry.database.PeriodDao;
import com.arwase.flowberry.models.CycleStats;
import com.arwase.flowberry.models.Period;

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
                periodDao.delete(period);
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

        double avgCycle = CycleStats.getAverageCycleLength(periods);
        double avgPeriod = CycleStats.getAveragePeriodLength(periods);
        long nextPeriodMillis = CycleStats.predictNextPeriodStart(periods);

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (avgCycle <= 0) {
            textCycleAvg.setText("Durée moyenne du cycle : pas assez de données");
        } else {
            textCycleAvg.setText("Durée moyenne du cycle : " +
                    String.format(Locale.getDefault(), "%.1f jours", avgCycle));
        }

        if (avgPeriod <= 0) {
            textPeriodAvg.setText("Durée moyenne des règles : pas assez de données");
        } else {
            textPeriodAvg.setText("Durée moyenne des règles : " +
                    String.format(Locale.getDefault(), "%.1f jours", avgPeriod));
        }

        if (nextPeriodMillis > 0) {
            textNextPeriod.setText("Prochain début estimé : " + df.format(new Date(nextPeriodMillis)));
        } else {
            textNextPeriod.setText("Prochain début estimé : -");
        }

        periodAdapter.setPeriods(periods);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }
}
