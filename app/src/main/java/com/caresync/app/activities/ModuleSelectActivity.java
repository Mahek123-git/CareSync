package com.caresync.app.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.caresync.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ModuleSelectActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_select);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        CardView cardDoctor = findViewById(R.id.cardDoctor);
        CardView cardPatient = findViewById(R.id.cardPatient);
        CardView cardAdmin = findViewById(R.id.cardAdmin);

        cardDoctor.setOnClickListener(v -> checkAndRoute("doctors",
                DoctorRegistrationActivity.class, DoctorDashboardActivity.class));

        cardPatient.setOnClickListener(v -> checkAndRoute("patients",
                PatientRegistrationActivity.class, PatientDashboardActivity.class));

        cardAdmin.setOnClickListener(v -> checkAndRoute("admins",
                AdminRegistrationActivity.class, AdminDashboardActivity.class));
    }

    private void checkAndRoute(String collection, Class<?> regClass, Class<?> dashClass) {
        db.collection(collection).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        startActivity(new Intent(this, dashClass));
                    } else {
                        startActivity(new Intent(this, regClass));
                    }
                })
                .addOnFailureListener(e -> startActivity(new Intent(this, regClass)));
    }
}
