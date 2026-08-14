package com.caresync.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.caresync.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DoctorDashboardActivity extends AppCompatActivity {

    private TextView tvDoctorName, tvSpecialization, tvAppointmentCount, tvPatientCount;
    private FirebaseFirestore db;
    private String uid;
    private String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        tvDoctorName = findViewById(R.id.tvDoctorName);
        tvSpecialization = findViewById(R.id.tvSpecialization);
        tvAppointmentCount = findViewById(R.id.tvAppointmentCount);
        tvPatientCount = findViewById(R.id.tvPatientCount);

        CardView cardToday = findViewById(R.id.cardTodaysAppointments);
        CardView cardPatients = findViewById(R.id.cardMyPatients);
        CardView cardPrescription = findViewById(R.id.cardPrescription);
        CardView cardProfile = findViewById(R.id.cardProfile);
        ImageView ivLogout = findViewById(R.id.ivLogout);

        cardToday.setOnClickListener(v -> startActivity(new Intent(this, TodaysAppointmentsActivity.class)));
        cardPatients.setOnClickListener(v -> startActivity(new Intent(this, MyPatientsActivity.class)));
        cardPrescription.setOnClickListener(v -> startActivity(new Intent(this, CreatePrescriptionActivity.class)));
        cardProfile.setOnClickListener(v -> startActivity(new Intent(this, DoctorProfileActivity.class)));
        ivLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });

        loadDoctorInfo();
        loadCounts();
    }

    private void loadDoctorInfo() {
        db.collection("doctors").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvDoctorName.setText("Dr. " + doc.getString("name"));
                        tvSpecialization.setText(doc.getString("specialization"));
                    }
                });
    }

    private void loadCounts() {
        // Today's appointments
        db.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snap -> tvAppointmentCount.setText(snap.size() + " scheduled"));

        // All patients
        db.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .get()
                .addOnSuccessListener(snap -> tvPatientCount.setText(snap.size() + " total"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCounts();
    }
}
