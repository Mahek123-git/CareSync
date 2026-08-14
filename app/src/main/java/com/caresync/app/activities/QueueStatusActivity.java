package com.caresync.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.adapters.QueueAdapter;
import com.caresync.app.models.Appointment;
import com.caresync.app.utils.NotificationHelper;
import com.caresync.app.utils.QueueNotificationManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.*;

public class QueueStatusActivity extends AppCompatActivity {

    private TextView tvQueueNumber, tvStatus, tvDoctorInfo, tvHospital, tvNoAppt, tvLiveQueueTitle;
    private MaterialButton btnCancel, btnUpdate;
    private RecyclerView rvLiveQueue;
    private QueueAdapter adapter;
    private List<Appointment> liveQueueList = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid, today, currentApptId, currentDoctorId;
    private int myQueueNum = -1;
    private ListenerRegistration apptListener, queueListener;
    
    // Flags to prevent repeat notifications in the same session
    private boolean turnNotified = false;
    private boolean readyNotified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_status);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        ImageView ivBack = findViewById(R.id.ivBack);
        tvQueueNumber = findViewById(R.id.tvQueueNumber);
        tvStatus = findViewById(R.id.tvStatus);
        tvDoctorInfo = findViewById(R.id.tvDoctorInfo);
        tvHospital = findViewById(R.id.tvHospital);
        tvNoAppt = findViewById(R.id.tvNoAppointment);
        tvLiveQueueTitle = findViewById(R.id.tvLiveQueueTitle);
        btnCancel = findViewById(R.id.btnCancelAppointment);
        btnUpdate = findViewById(R.id.btnUpdateAppointment);
        rvLiveQueue = findViewById(R.id.rvLiveQueue);

        adapter = new QueueAdapter(liveQueueList, null);
        adapter.setUserRole("patient");
        rvLiveQueue.setLayoutManager(new LinearLayoutManager(this));
        rvLiveQueue.setAdapter(adapter);

        ivBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> cancelAppointment());
        btnUpdate.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookAppointmentActivity.class);
            startActivity(intent);
        });

        listenToMyAppt();
    }

    private void listenToMyAppt() {
        apptListener = db.collection("appointments")
                .whereEqualTo("patientId", uid)
                .whereEqualTo("date", today)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    boolean found = false;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String status = doc.getString("status");
                        if (!"cancelled".equals(status)) {
                            found = true;
                            currentApptId = doc.getId();
                            currentDoctorId = doc.getString("doctorId");
                            Long q = doc.getLong("queueNumber");
                            myQueueNum = q != null ? q.intValue() : -1;
                            
                            tvQueueNumber.setText(myQueueNum != -1 ? String.valueOf(myQueueNum) : "—");
                            tvStatus.setText(capitalize(status != null ? status : "waiting"));
                            tvDoctorInfo.setText("Doctor: " + doc.getString("doctorName"));
                            tvHospital.setText("Hospital: " + doc.getString("hospitalName"));
                            
                            tvNoAppt.setVisibility(View.GONE);
                            
                            boolean canEdit = "waiting".equals(status);
                            btnCancel.setVisibility(canEdit ? View.VISIBLE : View.GONE);
                            btnUpdate.setVisibility(canEdit ? View.VISIBLE : View.GONE);
                            
                            tvLiveQueueTitle.setVisibility(View.VISIBLE);
                            
                            // Check for Notification Triggers from Firestore
                            checkForNotifications(doc);
                            
                            listenToLiveQueue();
                            break;
                        }
                    }
                    if (!found) resetUI();
                });
    }

    private void checkForNotifications(DocumentSnapshot doc) {
        Boolean notifiedTurn = doc.getBoolean("notifiedTurn");
        Boolean notifiedReady = doc.getBoolean("notifiedReady");
        String status = doc.getString("status");

        // "Your Turn Now" logic
        if ((Boolean.TRUE.equals(notifiedTurn) || "in progress".equals(status)) && !turnNotified) {
            NotificationHelper.showNotification(this, "Your Turn Now", 
                    "Please proceed to the doctor's cabin immediately.", QueueStatusActivity.class);
            turnNotified = true;
        } 
        // "Stay Ready" logic
        else if (Boolean.TRUE.equals(notifiedReady) && !readyNotified) {
            NotificationHelper.showNotification(this, "Stay Ready", 
                    "You are next in line. Please wait near the cabin.", QueueStatusActivity.class);
            readyNotified = true;
        }
    }

    private void listenToLiveQueue() {
        if (currentDoctorId == null) return;
        if (queueListener != null) queueListener.remove();

        queueListener = db.collection("appointments")
                .whereEqualTo("doctorId", currentDoctorId)
                .whereEqualTo("date", today)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    liveQueueList.clear();
                    
                    for (QueryDocumentSnapshot doc : snap) {
                        Appointment a = doc.toObject(Appointment.class);
                        a.setId(doc.getId());
                        if (!"cancelled".equals(a.getStatus())) {
                            liveQueueList.add(a);
                        }
                    }

                    Collections.sort(liveQueueList, (o1, o2) -> {
                        if (o1.isEmergency() != o2.isEmergency()) return o1.isEmergency() ? -1 : 1;
                        return Integer.compare(o1.getQueueNumber(), o2.getQueueNumber());
                    });
                    
                    adapter.notifyDataSetChanged();
                });
    }

    private void resetUI() {
        tvQueueNumber.setText("—");
        tvStatus.setText("No Appointment");
        tvNoAppt.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.GONE);
        btnUpdate.setVisibility(View.GONE);
        tvLiveQueueTitle.setVisibility(View.GONE);
        liveQueueList.clear();
        adapter.notifyDataSetChanged();
        currentApptId = null;
        currentDoctorId = null;
        myQueueNum = -1;
        turnNotified = false;
        readyNotified = false;
    }

    private void cancelAppointment() {
        if (currentApptId == null) return;

        db.collection("appointments").document(currentApptId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            Appointment appt = doc.toObject(Appointment.class);
            if (appt == null) return;
            appt.setId(doc.getId());
            int cancelledQueueNum = appt.getQueueNumber();

            db.collection("appointments")
                    .whereEqualTo("doctorId", appt.getDoctorId())
                    .whereEqualTo("date", appt.getDate())
                    .get()
                    .addOnSuccessListener(snap -> {
                        WriteBatch batch = db.batch();
                        // 1. Mark the current appointment as cancelled
                        batch.update(db.collection("appointments").document(appt.getId()), "status", "cancelled");

                        // 2. Decrement all subsequent waiting/present patients
                        for (QueryDocumentSnapshot d : snap) {
                            Appointment a = d.toObject(Appointment.class);
                            if (!d.getId().equals(appt.getId()) &&
                                    !"completed".equals(a.getStatus()) &&
                                    !"cancelled".equals(a.getStatus()) &&
                                    a.getQueueNumber() > cancelledQueueNum) {

                                batch.update(d.getReference(), "queueNumber", a.getQueueNumber() - 1);
                            }
                        }

                        batch.commit().addOnSuccessListener(v -> {
                            Toast.makeText(this, "Appointment cancelled and queue updated", Toast.LENGTH_SHORT).show();
                            // 3. Trigger notifications for patients who moved up
                            QueueNotificationManager.handleQueueShift(appt.getHospitalName(), appt.getDoctorId(), appt.getDate(), true, cancelledQueueNum);
                        });
                    });
        });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (apptListener != null) apptListener.remove();
        if (queueListener != null) queueListener.remove();
    }
}
