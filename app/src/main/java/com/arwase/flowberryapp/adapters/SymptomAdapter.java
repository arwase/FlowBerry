package com.arwase.flowberryapp.adapters;

import static com.arwase.flowberryapp.utils.Converters.getStartOfDayMillis;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arwase.flowberryapp.R;
import com.arwase.flowberryapp.models.Symptom;
import com.google.android.material.button.MaterialButton;

import java.util.Date;
import java.util.List;

public class SymptomAdapter extends RecyclerView.Adapter<SymptomAdapter.SymptomViewHolder> {
    public interface SymptomListener {
        void onEdit(Symptom symptom);
        void onDelete(Symptom symptom);
        void onValidate(Symptom symptom);
    }

    private List<Symptom> symptoms;
    private final SymptomListener listener;

    public SymptomAdapter(List<Symptom> symptoms, SymptomListener listener) {
        this.symptoms = symptoms;
        this.listener = listener;
    }

    public void setSymptoms(List<Symptom> newSymptoms) {
        this.symptoms = newSymptoms;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SymptomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_symptom, parent, false);
        return new SymptomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SymptomViewHolder holder, int position) {
        Symptom symptom = symptoms.get(position);
        holder.bind(symptom, listener);
    }

    @Override
    public int getItemCount() {
        return symptoms.size();
    }

    static class SymptomViewHolder extends RecyclerView.ViewHolder {

        TextView textSymptomName;
        TextView textSymptomDetails;
        TextView textSymptomIntensity;
        MaterialButton buttonEdit;
        MaterialButton buttonDelete;
        MaterialButton buttonValidate;
        ImageView imageAnticipated;

        SymptomViewHolder(@NonNull View itemView) {
            super(itemView);
            textSymptomName = itemView.findViewById(R.id.textSymptomName);
            textSymptomDetails = itemView.findViewById(R.id.textSymptomDetails);
            textSymptomIntensity = itemView.findViewById(R.id.textSymptomIntensity);
            imageAnticipated = itemView.findViewById(R.id.imageAnticipated);
            buttonEdit = itemView.findViewById(R.id.buttonEditSymptom);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteSymptom);
            buttonValidate = itemView.findViewById(R.id.buttonValidateSymptom);
        }

        void bind(Symptom symptom, SymptomListener listener) {
            textSymptomName.setText(symptom.type);
            textSymptomIntensity.setText(itemView.getContext().getString(R.string.symptom_item_intensity, symptom.intensity));
            textSymptomDetails.setText(symptom.notes);

            long dayStart = symptom.dateMillis;
            long todayStart = getStartOfDayMillis(new Date());
            boolean isToday = dayStart == todayStart;

            if (symptom.anticipated) {
                imageAnticipated.setVisibility(View.VISIBLE);
                buttonEdit.setVisibility(View.GONE);
                buttonDelete.setVisibility(View.GONE);
                buttonValidate.setVisibility(View.GONE);

                if (!symptom.validated && isToday) {
                    imageAnticipated.setVisibility(View.VISIBLE);
                    buttonValidate.setVisibility(View.VISIBLE);
                    buttonValidate.setOnClickListener(v -> listener.onValidate(symptom));
                }
            } else {
                buttonValidate.setVisibility(View.GONE);
                imageAnticipated.setVisibility(View.GONE);
                buttonEdit.setVisibility(View.VISIBLE);
                buttonDelete.setVisibility(View.VISIBLE);
                buttonEdit.setOnClickListener(v -> listener.onEdit(symptom));
                buttonDelete.setOnClickListener(v -> listener.onDelete(symptom));
            }
        }
    }
}
