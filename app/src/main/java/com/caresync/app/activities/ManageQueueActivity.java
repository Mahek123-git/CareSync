package com.caresync.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.adapters.QueueAdapter;
import com.caresync.app.models.Appointment;
import com.caresync.app.utils.QueueNotificationManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.*;

public class ManageQueueActivity extends AppCompatActivity implements QueueAdapter.QueueActionListener {

    private RecyclerView rvQueue;
    private TextView tvEmpty, tvHospital;
    private QueueAdapter adapter;
    private List<Appointment> queueList = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid, today, hospitalName;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_queue);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        ImageView ivBack = findViewById(R.id.ivBack);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvHospital = findViewById(R.id.tvHospitalName);
        rvQueue = findViewById(R.id.rvQueue);
        MaterialButton btnClearAll = findViewById(R.id.btnClearAll);

        ivBack.setOnClickListener(v -> finish());
        btnClearAll.setOnClickListener(v -> showClearDialog());

        adapter = new QueueAdapter(queueList, this);
        adapter.setUserRole("admin");
        rvQueue.setLayoutManager(new LinearLayoutManager(this));
        rvQueue.setAdapter(adapter);

        loadAdminHospital();
    }

    private void showClearDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("This will delete ALL appointments for today. They will no longer be visible to Patients or Doctors. Continue?")
                .setPositiveButton("Clear All", (dialog, which) -> clearAllAppointments())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllAppointments() {
        db.collection("appointments")
                .whereEqualTo("hospitalName", hospitalName)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snap) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnSuccessListener(v -> 
                        Toast.makeText(this, "All today's records cleared", Toast.LENGTH_SHORT).show()
                    );
                });
    }

    private void loadAdminHospital() {
        db.collection("admins").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        hospitalName = doc.getString("hospitalName");
                        tvHospital.setText(hospitalName);
                        listenQueue();
                    }
                });
    }

    private void listenQueue() {
        listener = db.collection("appointments")
                .whereEqualTo("hospitalName", hospitalName)
                .whereEqualTo("date", today)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    queueList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Appointment a = doc.toObject(Appointment.class);
                        a.setId(doc.getId());
                        // Hide cancelled and completed appointments from Manage Queue
                        if (!"cancelled".equals(a.getStatus()) && !"completed".equals(a.getStatus())) {
                            queueList.add(a);
                        }
                    }
                    Collections.sort(queueList, (o1, o2) -> {
                        if (o1.isEmergency() != o2.isEmergency()) {
                            return o1.isEmergency() ? -1 : 1;
                        }
                        return Integer.compare(o1.getQueueNumber(), o2.getQueueNumber());
                    });
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(queueList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onUpdateStatus(Appointment appt, String status) {
        if ("absent".equals(status)) {
            triggerQueueShift(appt);
        } else {
            db.collection("appointments").document(appt.getId())
                .update("status", status)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                    appt.setStatus(status);
                    QueueNotificationManager.handleStatusUpdate(appt);
                });
        }
    }

    private void triggerQueueShift(Appointment absentAppt) {
        db.collection("appointments")
                .whereEqualTo("hospitalName", absentAppt.getHospitalName())
                .whereEqualTo("doctorId", absentAppt.getDoctorId())
                .whereEqualTo("date", absentAppt.getDate())
                .get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = db.batch();
                    // 1. Mark the current patient as absent
                    batch.update(db.collection("appointments").document(absentAppt.getId()), "status", "absent");

                    // 2. Decrement all subsequent patients
                    for (QueryDocumentSnapshot doc : snap) {
                        Appointment a = doc.toObject(Appointment.class);
                        if (!doc.getId().equals(absentAppt.getId()) && 
                            !"completed".equals(a.getStatus()) && 
                            !"cancelled".equals(a.getStatus()) &&
                            a.getQueueNumber() > absentAppt.getQueueNumber()) {
                            
                            batch.update(doc.getReference(), "queueNumber", a.getQueueNumber() - 1);
                        }
                    }

                    batch.commit().addOnSuccessListener(v -> {
                        Toast.makeText(this, "Patient marked absent. Queue updated.", Toast.LENGTH_SHORT).show();
                        // 3. Trigger notifications for the new #1 and #2 in the shifted queue
                        QueueNotificationManager.handleQueueShift(absentAppt.getHospitalName(), absentAppt.getDoctorId(), absentAppt.getDate());
                    });
                });
    }

    @Override
    public void onMoveUp(Appointment appt) {
        int idx = queueList.indexOf(appt);
        if (idx <= 0) return;

        Appointment prev = queueList.get(idx - 1);
        int prevQ = prev.getQueueNumber();
        int currQ = appt.getQueueNumber();

        WriteBatch batch = db.batch();
        batch.update(db.collection("appointments").document(appt.getId()), "queueNumber", prevQ);
        batch.update(db.collection("appointments").document(prev.getId()), "queueNumber", currQ);
        
        batch.commit().addOnSuccessListener(v -> {
            appt.setQueueNumber(prevQ);
            prev.setQueueNumber(currQ);
            QueueNotificationManager.handleQueueMove(appt, prev);
        });
    }
}
