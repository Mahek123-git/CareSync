package com.caresync.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.caresync.app.R;
import com.caresync.app.models.Patient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class PatientRegistrationActivity extends AppCompatActivity {

    private TextInputEditText etName, etAge, etPhone;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale, rbOther;
    private MaterialButton btnRegister;
    private FirebaseFirestore db;
    private String uid, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_registration);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        etName = findViewById(R.id.etPatientName);
        etAge = findViewById(R.id.etPatientAge);
        etPhone = findViewById(R.id.etPhone);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        rbOther = findViewById(R.id.rbOther);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> validate());
    }

    private void validate() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String ageStr = etAge.getText() != null ? etAge.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) { etName.setError("Required"); return; }
        if (TextUtils.isEmpty(ageStr)) { etAge.setError("Required"); return; }

        String gender = rbFemale.isChecked() ? "Female" : rbOther.isChecked() ? "Other" : "Male";

        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            Patient patient = new Patient(uid, name, Integer.parseInt(ageStr), gender, phone, email);
            patient.setFcmToken(token);
            db.collection("patients").document(uid).set(patient)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, PatientDashboardActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Complete Registration");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}
