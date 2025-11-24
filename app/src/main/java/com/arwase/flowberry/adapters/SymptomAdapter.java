package com.arwase.flowberry.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arwase.flowberry.R;
import com.arwase.flowberry.models.Symptom;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SymptomAdapter extends RecyclerView.Adapter<SymptomAdapter.SymptomViewHolder> {

    private List<Symptom> symptoms;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public SymptomAdapter(List<Symptom> symptoms) {
        this.symptoms = symptoms;
    }

    public void setSymptoms(List<Symptom> newSymptoms) {
        this.symptoms = newSymptoms;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SymptomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_symptom, parent, false);
        return new SymptomViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SymptomViewHolder holder, int position) {
        Symptom s = symptoms.get(position);
        holder.textType.setText(s.type + " (" + s.intensity + "/5)");
        holder.textNotes.setText(s.notes != null ? s.notes : "");
    }

    @Override
    public int getItemCount() {
        return symptoms != null ? symptoms.size() : 0;
    }

    static class SymptomViewHolder extends RecyclerView.ViewHolder {

        TextView textType;
        TextView textNotes;

        SymptomViewHolder(@NonNull View itemView) {
            super(itemView);
            textType = itemView.findViewById(R.id.textSymptomType);
            textNotes = itemView.findViewById(R.id.textSymptomNotes);
        }
    }
}