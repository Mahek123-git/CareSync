package com.caresync.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.models.Appointment;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.VH> {

    public interface QueueActionListener {
        void onUpdateStatus(Appointment appt, String status);
        void onMoveUp(Appointment appt);
        default void onViewPrescription(Appointment appt) {}
    }

    private final List<Appointment> list;
    private final QueueActionListener listener;
    private String userRole = "patient"; // "admin", "doctor", or "patient"

    public QueueAdapter(List<Appointment> list, QueueActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void setUserRole(String role) {
        this.userRole = role;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Appointment a = list.get(pos);
        h.tvQueue.setText(a.isEmergency() ? "EMG" : String.valueOf(a.getQueueNumber()));
        h.tvName.setText(a.getPatientName());
        h.tvDoctor.setText("Dr. " + a.getDoctorName());
        h.tvTime.setText(a.getTime());
        
        if (h.tvIllness != null) {
            h.tvIllness.setText("Illness: " + (a.getIllness() != null ? a.getIllness() : "Not specified"));
            h.tvIllness.setVisibility(View.VISIBLE);
        }

        if (a.isEmergency()) {
            h.layoutQueueBadge.setBackgroundResource(R.drawable.circle_red);
        } else {
            h.layoutQueueBadge.setBackgroundResource(R.drawable.circle_blue);
        }

        String status = a.getStatus() != null ? a.getStatus() : "waiting";
        h.tvStatus.setText(capitalize(status));

        int color;
        switch (status) {
            case "present": color = Color.parseColor("#4CAF50"); break;
            case "absent":  color = Color.parseColor("#F44336"); break;
            case "completed": color = Color.parseColor("#2196F3"); break;
            case "in progress": color = Color.parseColor("#9C27B0"); break;
            default:        color = Color.parseColor("#FF9800"); break;
        }
        h.tvStatus.setBackgroundColor(color);

        // Reset visibility
        h.btnAction.setVisibility(View.GONE);

        if ("admin".equals(userRole)) {
            h.tvStatus.setOnClickListener(v -> {
                // Admin can manage: Present, Absent, Waiting, In Progress. REMOVED Completed.
                String[] options = {"Waiting", "Present", "Absent", "In Progress"};
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Update Status")
                        .setItems(options, (dialog, which) -> {
                            listener.onUpdateStatus(a, options[which].toLowerCase());
                        })
                        .show();
            });
            h.ivMoveUp.setVisibility(View.VISIBLE);
            h.ivMoveUp.setOnClickListener(v -> listener.onMoveUp(a));
        } else if ("doctor".equals(userRole)) {
            h.ivMoveUp.setVisibility(View.GONE);
            if (!"completed".equals(status)) {
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setText("Complete Consultation");
                h.btnAction.setOnClickListener(v -> {
                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Complete Consultation")
                            .setMessage("Mark this appointment as completed? Ensure you have saved the prescription.")
                            .setPositiveButton("Yes", (dialog, which) -> listener.onUpdateStatus(a, "completed"))
                            .setNegativeButton("No", null)
                            .show();
                });
            }
            h.tvStatus.setOnClickListener(null);
        } else {
            // Patient Role
            h.ivMoveUp.setVisibility(View.GONE);
            h.tvStatus.setOnClickListener(null);
            if ("completed".equals(status)) {
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setText("View Prescription");
                h.btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onViewPrescription(a);
                });
            }
        }
    }

    @Override public int getItemCount() { return list.size(); }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvQueue, tvName, tvDoctor, tvTime, tvStatus, tvIllness;
        ImageView ivMoveUp;
        View layoutQueueBadge;
        MaterialButton btnAction;
        VH(View v) {
            super(v);
            tvQueue = v.findViewById(R.id.tvQueueNum);
            tvName = v.findViewById(R.id.tvPatientName);
            tvDoctor = v.findViewById(R.id.tvDoctor);
            tvTime = v.findViewById(R.id.tvTime);
            tvStatus = v.findViewById(R.id.tvStatus);
            ivMoveUp = v.findViewById(R.id.ivMoveUp);
            layoutQueueBadge = v.findViewById(R.id.layoutQueueBadge);
            tvIllness = v.findViewById(R.id.tvIllness);
            // We need to add btnAction to item_queue.xml
            btnAction = v.findViewById(R.id.btnAction);
        }
    }
}
