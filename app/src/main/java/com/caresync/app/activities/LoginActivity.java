package com.caresync.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.caresync.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        // Note: ProgressBar should be in the layout, if not, findViewById will return null
        // Looking at activity_login.xml, it's missing. I will keep it for logic but it may cause null if not in XML.
        progressBar = findViewById(R.id.progressBar); 
        TextView tvSignUp = findViewById(R.id.tvSignUp);

        btnLogin.setOnClickListener(v -> loginUser());
        tvSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) { etEmail.setError("Email is required"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password is required"); return; }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateFcmTokenAndRedirect();
                    } else {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateFcmTokenAndRedirect() {
        String uid = mAuth.getCurrentUser().getUid();
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            // Check patient collection
            db.collection("patients").document(uid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    db.collection("patients").document(uid).update("fcmToken", token);
                    startActivity(new Intent(this, PatientDashboardActivity.class));
                    finish();
                } else {
                    // Check doctor collection
                    db.collection("doctors").document(uid).get().addOnCompleteListener(taskDoc -> {
                        if (taskDoc.isSuccessful() && taskDoc.getResult().exists()) {
                            db.collection("doctors").document(uid).update("fcmToken", token);
                            startActivity(new Intent(this, DoctorDashboardActivity.class));
                            finish();
                        } else {
                            // Check admin collection
                            db.collection("admins").document(uid).get().addOnCompleteListener(taskAdm -> {
                                if (taskAdm.isSuccessful() && taskAdm.getResult().exists()) {
                                    db.collection("admins").document(uid).update("fcmToken", token);
                                    startActivity(new Intent(this, AdminDashboardActivity.class));
                                    finish();
                                } else {
                                    startActivity(new Intent(this, ModuleSelectActivity.class));
                                    finish();
                                }
                            });
                        }
                    });
                }
            });
        });
    }
}
