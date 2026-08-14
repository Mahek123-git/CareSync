package com.caresync.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.adapters.PersonAdapter;
import com.caresync.app.models.Admin;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminRegistrationActivity extends AppCompatActivity {

    private TextInputEditText etName, etHospital, etHospitalId, etContact;
    private MaterialButton btnRegister;
    private RecyclerView rvDoctors;
    private TextView tvDoctorsTitle;
    private PersonAdapter doctorAdapter;
    private List<String[]> doctorList = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_registration);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        etName = findViewById(R.id.etAdminName);
        etHospital = findViewById(R.id.etHospitalName);
        etHospitalId = findViewById(R.id.etHospitalId);
        etContact = findViewById(R.id.etContact);
        btnRegister = findViewById(R.id.btnRegister);
        rvDoctors = findViewById(R.id.rvDoctorsPreview);
        tvDoctorsTitle = findViewById(R.id.tvDoctorsTitle);

        doctorAdapter = new PersonAdapter(doctorList);
        rvDoctors.setLayoutManager(new LinearLayoutManager(this));
        rvDoctors.setAdapter(doctorAdapter);

        etHospital.addTextChangedListener(new TextWatcher() {
            @Override 
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override 
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                fetchDoctorsForHospital(s.toString().trim());
            }
        });

        btnRegister.setOnClickListener(v -> validate());
    }

    private void fetchDoctorsForHospital(String hospitalName) {
        if (hospitalName.length() < 3) {
            doctorList.clear();
            doctorAdapter.notifyDataSetChanged();
            tvDoctorsTitle.setVisibility(View.GONE);
            rvDoctors.setVisibility(View.GONE);
            return;
        }

        db.collection("doctors")
                .whereEqualTo("hospitalName", hospitalName)
                .get()
                .addOnSuccessListener(snap -> {
                    doctorList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        String name = doc.getString("name");
                        String spec = doc.getString("specialization");
                        doctorList.add(new String[]{
                                "Dr. " + (name != null ? name : "Unknown"),
                                spec != null ? spec : "General"
                        });
                    }
                    doctorAdapter.notifyDataSetChanged();
                    if (!doctorList.isEmpty()) {
                        tvDoctorsTitle.setVisibility(View.VISIBLE);
                        rvDoctors.setVisibility(View.VISIBLE);
                    } else {
                        tvDoctorsTitle.setVisibility(View.GONE);
                        rvDoctors.setVisibility(View.GONE);
                    }
                });
    }

    private void validate() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String hospital = etHospital.getText() != null ? etHospital.getText().toString().trim() : "";
        String hid = etHospitalId.getText() != null ? etHospitalId.getText().toString().trim() : "";
        String contact = etContact.getText() != null ? etContact.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) { etName.setError("Required"); return; }
        if (TextUtils.isEmpty(hospital)) { etHospital.setError("Required"); return; }
        if (TextUtils.isEmpty(hid)) { etHospitalId.setError("Required"); return; }

        btnRegister.setEnabled(false);
        btnRegister.setText("Registering...");

        Admin admin = new Admin(uid, name, hospital, hid, contact, email);
        db.collection("admins").document(uid).set(admin)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Admin registered!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Complete Registration");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
