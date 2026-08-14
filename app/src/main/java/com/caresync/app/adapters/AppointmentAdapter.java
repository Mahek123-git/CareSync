package com.caresync.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.models.Appointment;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.VH> {

    private final List<Appointment> list;
    private final boolean isAdminMode;

    public AppointmentAdapter(List<Appointment> list, boolean isAdminMode) {
        this.list = list;
        this.isAdminMode = isAdminMode;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Appointment a = list.get(pos);
        h.tvQueue.setText(String.valueOf(a.getQueueNumber()));
        h.tvName.setText(a.getPatientName());
        h.tvTime.setText(a.getTime());
        String status = a.getStatus() != null ? a.getStatus() : "waiting";
        h.tvStatus.setText(capitalize(status));
        int color;
        switch (status) {
            case "present": color = Color.parseColor("#4CAF50"); break;
            case "absent":  color = Color.parseColor("#F44336"); break;
            case "completed": color = Color.parseColor("#2196F3"); break;
            default: color = Color.parseColor("#FF9800"); break;
        }
        h.tvStatus.setBackgroundColor(color);
    }

    @Override public int getItemCount() { return list.size(); }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvQueue, tvName, tvTime, tvStatus;
        VH(View v) {
            super(v);
            tvQueue = v.findViewById(R.id.tvQueueNumber);
            tvName = v.findViewById(R.id.tvPatientName);
            tvTime = v.findViewById(R.id.tvAppointmentTime);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }
}
