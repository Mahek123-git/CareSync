package com.caresync.app.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.caresync.app.R;
import com.caresync.app.services.QueueWatcherService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PatientDashboardActivity extends AppCompatActivity {

    private TextView tvPatientName, tvQueueNumber;
    private FirebaseFirestore db;
    private String uid, today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        tvPatientName = findViewById(R.id.tvPatientName);
        tvQueueNumber = findViewById(R.id.tvQueueNumber);
        ImageView ivLogout = findViewById(R.id.ivLogout);

        CardView cardBook = findViewById(R.id.cardBookAppointment);
        CardView cardQueue = findViewById(R.id.cardQueueStatus);
        CardView cardRx = findViewById(R.id.cardPrescription);
        CardView cardReminder = findViewById(R.id.cardReminders);

        cardBook.setOnClickListener(v -> startActivity(new Intent(this, BookAppointmentActivity.class)));
        cardQueue.setOnClickListener(v -> startActivity(new Intent(this, QueueStatusActivity.class)));
        cardRx.setOnClickListener(v -> startActivity(new Intent(this, PatientPrescriptionActivity.class)));
        cardReminder.setOnClickListener(v -> startActivity(new Intent(this, MedicineReminderActivity.class)));
        ivLogout.setOnClickListener(v -> {
            stopService(new Intent(this, QueueWatcherService.class));
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });

        loadPatientInfo();
        loadQueueStatus();
        startQueueWatcher();
    }

    private void startQueueWatcher() {
        Intent serviceIntent = new Intent(this, QueueWatcherService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void loadPatientInfo() {
        db.collection("patients").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) tvPatientName.setText(doc.getString("name"));
                });
    }

    private void loadQueueStatus() {
        db.collection("appointments")
                .whereEqualTo("patientId", uid)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snap -> {
                    boolean hasAppt = false;
                    for (QueryDocumentSnapshot doc : snap) {
                        Long q = doc.getLong("queueNumber");
                        String status = doc.getString("status");
                        if (!"cancelled".equals(status) && !"completed".equals(status) && q != null) {
                            tvQueueNumber.setText("Queue #" + q);
                            hasAppt = true;
                            break;
                        }
                    }
                    if (!hasAppt) tvQueueNumber.setText("No active booking");
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadQueueStatus();
    }
}
