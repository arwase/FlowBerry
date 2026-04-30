package com.arwase.flowberryapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.models.CycleStats;
import com.arwase.flowberryapp.models.Period;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PeriodAdapter extends RecyclerView.Adapter<PeriodAdapter.PeriodViewHolder> {

    public interface OnPeriodActionListener {
        void onEdit(Period period);
        void onDelete(Period period);
    }

    private final List<Period> periods = new ArrayList<>();
    private final DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final OnPeriodActionListener listener;

    public PeriodAdapter(OnPeriodActionListener listener) {
        this.listener = listener;
    }

    public void setPeriods(List<Period> list) {
        periods.clear();
        if (list != null) periods.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PeriodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_period, parent, false);
        return new PeriodViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PeriodViewHolder holder, int position) {
        Period period = periods.get(position);
        Context context = holder.itemView.getContext();

        boolean isCurrentCycle = period.endDateMillis == null;

        String title;
        if (isCurrentCycle && position == 0) {
            title = context.getString(R.string.period_item_current_cycle);
        } else {
            int lengthDays = CycleStats.getCycleLengthForPosition(periods, position);
            if (lengthDays > 0) {
                title = context.getString(R.string.period_item_cycle_length, lengthDays);
            } else {
                title = context.getString(R.string.period_item_placeholder_title);
            }
        }

        Date startDate = new Date(period.startDateMillis);
        String startStr = df.format(startDate);

        String subtitle;
        if (period.endDateMillis != null) {
            Date endDate = new Date(period.endDateMillis);
            String endStr = df.format(endDate);
            subtitle = context.getString(R.string.period_item_dates_range, startStr, endStr);
        } else {
            subtitle = context.getString(R.string.period_item_dates_current, startStr);
        }

        holder.textTitle.setText(title);
        holder.textDates.setText(subtitle);

        holder.buttonEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(period);
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(period);
        });
    }

    @Override
    public int getItemCount() {
        return periods.size();
    }

    static class PeriodViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textDates;
        Button buttonEdit;
        Button buttonDelete;

        PeriodViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textPeriodTitle);
            textDates = itemView.findViewById(R.id.textPeriodDates);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
