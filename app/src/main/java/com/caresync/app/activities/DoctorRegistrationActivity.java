package com.caresync.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.caresync.app.R;
import com.caresync.app.models.Doctor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DoctorRegistrationActivity extends AppCompatActivity {

    private static final String TAG = "DoctorRegistration";
    private TextInputEditText etName, etAge, etQualification, etSpecialization, etHospital, etLicense;
    private MaterialButton btnRegister;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_registration);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etName = findViewById(R.id.etDoctorName);
        etAge = findViewById(R.id.etDoctorAge);
        etQualification = findViewById(R.id.etQualification);
        etSpecialization = findViewById(R.id.etSpecialization);
        etHospital = findViewById(R.id.etHospitalName);
        etLicense = findViewById(R.id.etLicenseNumber);
        btnRegister = findViewById(R.id.btnRegister);

        // Hide the upload layout since we are skipping storage
        View uploadLayout = findViewById(R.id.layoutUpload);
        if (uploadLayout != null) {
            uploadLayout.setVisibility(View.GONE);
        }
        
        TextView tvUploadTitle = findViewById(R.id.tvUploadTitle);
        if (tvUploadTitle != null) {
            tvUploadTitle.setVisibility(View.GONE);
        }

        btnRegister.setOnClickListener(v -> validateAndRegister());
    }

    private void validateAndRegister() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String ageStr = etAge.getText() != null ? etAge.getText().toString().trim() : "";
        String qual = etQualification.getText() != null ? etQualification.getText().toString().trim() : "";
        String spec = etSpecialization.getText() != null ? etSpecialization.getText().toString().trim() : "";
        String hospital = etHospital.getText() != null ? etHospital.getText().toString().trim() : "";
        String license = etLicense.getText() != null ? etLicense.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) { etName.setError("Required"); return; }
        if (TextUtils.isEmpty(ageStr)) { etAge.setError("Required"); return; }
        if (TextUtils.isEmpty(qual)) { etQualification.setError("Required"); return; }
        if (TextUtils.isEmpty(spec)) { etSpecialization.setError("Required"); return; }
        if (TextUtils.isEmpty(hospital)) { etHospital.setError("Required"); return; }
        if (TextUtils.isEmpty(license)) { etLicense.setError("Required"); return; }

        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");
        
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            etAge.setError("Invalid age");
            btnRegister.setEnabled(true);
            btnRegister.setText("Complete Registration");
            return;
        }

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Save directly to Firestore without uploading any files
        saveDoctor(name, age, qual, spec, hospital, license, "", email);
    }

    private void saveDoctor(String name, int age, String qual, String spec,
                            String hospital, String license, String licenseUrl, String email) {
        Doctor doctor = new Doctor(uid, name, age, qual, spec, hospital, license, licenseUrl, email);
        db.collection("doctors").document(uid).set(doctor)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, DoctorDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore Error: " + e.getMessage());
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Complete Registration");
                    Toast.makeText(this, "Permission Denied: Update your Firestore Rules in Firebase Console.", Toast.LENGTH_LONG).show();
                });
    }
}
