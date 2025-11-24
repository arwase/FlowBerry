package com.arwase.flowberry.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arwase.flowberry.R;
import com.arwase.flowberry.models.Period;

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
        Period p = periods.get(position);

        String title = "Cycle " + (position + 1);
        holder.textTitle.setText(title);

        String start = df.format(new Date(p.startDateMillis));
        String end;
        if (p.endDateMillis != null) {
            end = df.format(new Date(p.endDateMillis));
        } else {
            end = "en cours";
        }
        holder.textDates.setText("Du " + start + " au " + end);

        holder.buttonEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(p);
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(p);
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
