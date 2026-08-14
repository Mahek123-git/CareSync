package com.caresync.app.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.caresync.app.R;
import com.caresync.app.services.AdminWatcherService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvAdminName, tvHospitalName;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tvAdminName = findViewById(R.id.tvAdminName);
        tvHospitalName = findViewById(R.id.tvHospitalName);
        ImageView ivLogout = findViewById(R.id.ivLogout);
        CardView cardQueue = findViewById(R.id.cardManageQueue);
        CardView cardDoctors = findViewById(R.id.cardDoctors);
        CardView cardPatients = findViewById(R.id.cardPatients);

        cardQueue.setOnClickListener(v -> startActivity(new Intent(this, ManageQueueActivity.class)));
        cardDoctors.setOnClickListener(v -> startActivity(new Intent(this, AdminDoctorsActivity.class)));
        cardPatients.setOnClickListener(v -> startActivity(new Intent(this, AdminPatientsActivity.class)));
        ivLogout.setOnClickListener(v -> {
            stopService(new Intent(this, AdminWatcherService.class));
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });

        loadAdminInfo();
        startAdminWatcher();
    }

    private void startAdminWatcher() {
        Intent serviceIntent = new Intent(this, AdminWatcherService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void loadAdminInfo() {
        db.collection("admins").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvAdminName.setText(doc.getString("name"));
                        tvHospitalName.setText(doc.getString("hospitalName"));
                    }
                });
    }
}
