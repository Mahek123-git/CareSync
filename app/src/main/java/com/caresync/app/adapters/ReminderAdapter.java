package com.caresync.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.models.Medicine;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.VH> {

    public interface OnReminderActionListener {
        void onSetTime(Medicine medicine, String slot);
        void onDelete(Medicine medicine);
    }

    private final List<Medicine> list;
    private final OnReminderActionListener listener;

    public ReminderAdapter(List<Medicine> list, OnReminderActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Medicine m = list.get(pos);
        h.tvName.setText(m.getName());
        h.tvSchedule.setText(m.getScheduleText());

        // Morning
        if (m.isMorning()) {
            h.layoutMorning.setVisibility(View.VISIBLE);
            h.tvMorningTime.setText(m.getMorningTime() != null ? m.getMorningTime() : "Not set");
            h.btnMorning.setOnClickListener(v -> listener.onSetTime(m, "morning"));
        } else {
            h.layoutMorning.setVisibility(View.GONE);
        }

        // Afternoon
        if (m.isAfternoon()) {
            h.layoutAfternoon.setVisibility(View.VISIBLE);
            h.tvAfternoonTime.setText(m.getAfternoonTime() != null ? m.getAfternoonTime() : "Not set");
            h.btnAfternoon.setOnClickListener(v -> listener.onSetTime(m, "afternoon"));
        } else {
            h.layoutAfternoon.setVisibility(View.GONE);
        }

        // Night
        if (m.isNight()) {
            h.layoutNight.setVisibility(View.VISIBLE);
            h.tvNightTime.setText(m.getNightTime() != null ? m.getNightTime() : "Not set");
            h.btnNight.setOnClickListener(v -> listener.onSetTime(m, "night"));
        } else {
            h.layoutNight.setVisibility(View.GONE);
        }

        h.ivDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Reminder")
                    .setMessage("Are you sure you want to remove this medicine from your active reminders?")
                    .setPositiveButton("Delete", (dialog, which) -> listener.onDelete(m))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSchedule, tvMorningTime, tvAfternoonTime, tvNightTime;
        LinearLayout layoutMorning, layoutAfternoon, layoutNight;
        MaterialButton btnMorning, btnAfternoon, btnNight;
        ImageView ivDelete;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvMedicineName);
            tvSchedule = v.findViewById(R.id.tvSchedule);
            layoutMorning = v.findViewById(R.id.layoutMorning);
            layoutAfternoon = v.findViewById(R.id.layoutAfternoon);
            layoutNight = v.findViewById(R.id.layoutNight);
            tvMorningTime = v.findViewById(R.id.tvMorningTime);
            tvAfternoonTime = v.findViewById(R.id.tvAfternoonTime);
            tvNightTime = v.findViewById(R.id.tvNightTime);
            btnMorning = v.findViewById(R.id.btnSetMorning);
            btnAfternoon = v.findViewById(R.id.btnSetAfternoon);
            btnNight = v.findViewById(R.id.btnSetNight);
            ivDelete = v.findViewById(R.id.ivDelete);
        }
    }
}
