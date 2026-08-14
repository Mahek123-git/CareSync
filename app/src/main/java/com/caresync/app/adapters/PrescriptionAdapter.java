package com.caresync.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.models.Medicine;
import com.caresync.app.models.Prescription;
import java.util.List;

public class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.VH> {

    private final List<Prescription> list;

    public PrescriptionAdapter(List<Prescription> list) { this.list = list; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Prescription rx = list.get(pos);
        h.tvDoctor.setText(rx.getDoctorName() != null ? rx.getDoctorName() : "Doctor");
        h.tvDate.setText(rx.getDate() != null ? rx.getDate() : "");
        h.tvDiagnosis.setText("Diagnosis: " + (rx.getDiagnosis() != null ? rx.getDiagnosis() : "—"));

        if (rx.getMedicines() != null && !rx.getMedicines().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Medicine m : rx.getMedicines()) {
                sb.append("• ").append(m.getName())
                  .append(" (").append(m.getScheduleText()).append(")\n");
            }
            h.tvMedicines.setText(sb.toString().trim());
        } else {
            h.tvMedicines.setText("—");
        }

        if (rx.getNotes() != null && !rx.getNotes().isEmpty()) {
            h.tvNotes.setText("Note: " + rx.getNotes());
            h.tvNotes.setVisibility(View.VISIBLE);
        } else {
            h.tvNotes.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDoctor, tvDate, tvDiagnosis, tvMedicines, tvNotes;
        VH(View v) {
            super(v);
            tvDoctor = v.findViewById(R.id.tvDoctorName);
            tvDate = v.findViewById(R.id.tvDate);
            tvDiagnosis = v.findViewById(R.id.tvDiagnosis);
            tvMedicines = v.findViewById(R.id.tvMedicines);
            tvNotes = v.findViewById(R.id.tvNotes);
        }
    }
}
