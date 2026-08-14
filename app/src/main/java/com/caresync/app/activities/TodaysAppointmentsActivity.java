package com.caresync.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.adapters.QueueAdapter;
import com.caresync.app.models.Appointment;
import com.caresync.app.utils.QueueNotificationManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.*;

public class TodaysAppointmentsActivity extends AppCompatActivity implements QueueAdapter.QueueActionListener {

    private RecyclerView rvAppointments;
    private TextView tvEmpty, tvDate;
    private QueueAdapter adapter;
    private List<Appointment> appointmentList = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid, today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todays_appointments);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        ImageView ivBack = findViewById(R.id.ivBack);
        tvDate = findViewById(R.id.tvDate);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvAppointments = findViewById(R.id.rvAppointments);

        tvDate.setText(new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(new Date()));
        ivBack.setOnClickListener(v -> finish());

        adapter = new QueueAdapter(appointmentList, this);
        adapter.setUserRole("doctor");
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvAppointments.setAdapter(adapter);

        loadAppointments();
    }

    private void loadAppointments() {
        db.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .whereEqualTo("date", today)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    appointmentList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Appointment a = doc.toObject(Appointment.class);
                        a.setId(doc.getId());
                        if (!"cancelled".equals(a.getStatus()) && !"completed".equals(a.getStatus())) {
                            appointmentList.add(a);
                        }
                    }
                    Collections.sort(appointmentList, (o1, o2) -> {
                        if (o1.isEmergency() != o2.isEmergency()) return o1.isEmergency() ? -1 : 1;
                        return Integer.compare(o1.getQueueNumber(), o2.getQueueNumber());
                    });
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(appointmentList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onUpdateStatus(Appointment appt, String status) {
        if ("completed".equals(status)) {
            db.collection("prescriptions")
                    .whereEqualTo("doctorId", uid)
                    .whereEqualTo("patientId", appt.getPatientId())
                    .whereEqualTo("date", new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()))
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) {
                            Toast.makeText(this, "Please save a prescription first!", Toast.LENGTH_LONG).show();
                        } else {
                            updateStatusAndShiftQueue(appt);
                        }
                    });
        } else {
            db.collection("appointments").document(appt.getId())
                    .update("status", status)
                    .addOnSuccessListener(v -> {
                        appt.setStatus(status);
                        QueueNotificationManager.handleStatusUpdate(appt);
                    });
        }
    }

    private void updateStatusAndShiftQueue(Appointment completedAppt) {
        WriteBatch batch = db.batch();
        batch.update(db.collection("appointments").document(completedAppt.getId()), "status", "completed");
        
        db.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        Appointment a = doc.toObject(Appointment.class);
                        if (!doc.getId().equals(completedAppt.getId()) && 
                            !"completed".equals(a.getStatus()) && 
                            !"cancelled".equals(a.getStatus()) &&
                            a.getQueueNumber() > completedAppt.getQueueNumber()) {
                            
                            batch.update(doc.getReference(), "queueNumber", a.getQueueNumber() - 1);
                        }
                    }
                    
                    batch.commit().addOnSuccessListener(v -> {
                        Toast.makeText(this, "Consultation Completed. Queue updated.", Toast.LENGTH_SHORT).show();
                        // Trigger notifications for new #1 and #2 in the shifted queue
                        QueueNotificationManager.handleQueueShift(completedAppt.getHospitalName(), uid, today);
                    });
                });
    }

    @Override public void onMoveUp(Appointment appt) {}
}
