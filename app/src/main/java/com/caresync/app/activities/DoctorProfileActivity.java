package com.caresync.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.caresync.app.R;
import com.caresync.app.models.Doctor;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DoctorProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileSpec, tvInitial, tvAge, tvQual, tvHospital, tvSlots;
    private MaterialButton btnEmergency;
    private FirebaseFirestore db;
    private String uid;
    private Doctor currentDoctor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ImageView ivBack = findViewById(R.id.ivBack);
        ImageView ivEdit = findViewById(R.id.ivEdit);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileSpec = findViewById(R.id.tvProfileSpec);
        tvInitial = findViewById(R.id.tvInitial);
        tvAge = findViewById(R.id.tvAge);
        tvQual = findViewById(R.id.tvQualification);
        tvHospital = findViewById(R.id.tvHospital);
        tvSlots = findViewById(R.id.tvSlots);
        btnEmergency = findViewById(R.id.btnDeclareEmergency);

        ivBack.setOnClickListener(v -> finish());
        ivEdit.setOnClickListener(v -> showEditSlotsDialog());
        btnEmergency.setOnClickListener(v -> handleEmergencyDeclaration());
        
        loadProfile();
    }

    private void loadProfile() {
        db.collection("doctors").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentDoctor = doc.toObject(Doctor.class);
                        if (currentDoctor != null) {
                            String name = currentDoctor.getName();
                            tvProfileName.setText("Dr. " + name);
                            tvProfileSpec.setText(currentDoctor.getSpecialization());
                            tvInitial.setText(name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "D");
                            tvAge.setText(currentDoctor.getAge() + " years");
                            tvQual.setText(currentDoctor.getQualification());
                            tvHospital.setText(currentDoctor.getHospitalName());
                            
                            displaySlots(currentDoctor.getTimeSlots());
                        }
                    }
                });
    }

    private void handleEmergencyDeclaration() {
        if (currentDoctor == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Declare Emergency")
                .setMessage("This will notify all today's patients and the hospital admin that you have an emergency. Continue?")
                .setPositiveButton("Declare", (dialog, which) -> sendEmergencyNotifications())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendEmergencyNotifications() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // We update all today's appointments for this doctor to trigger the QueueWatcherService on patients' and admin's devices
        db.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this, "No appointments today to notify.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snap) {
                        String status = doc.getString("status");
                        if ("waiting".equals(status) || "present".equals(status) || "in progress".equals(status)) {
                            batch.update(doc.getReference(), "doctorEmergency", true);
                        }
                    }
                    
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Emergency declared. Notifications sent to patients and admin.", Toast.LENGTH_LONG).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to send notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
        
        // Also update the doctor document to reflect emergency status globally if needed
        db.collection("doctors").document(uid).update("isEmergency", true);
    }

    private void displaySlots(List<String> slots) {
        if (slots == null || slots.isEmpty()) {
            tvSlots.setText("No slots defined. Tap edit to add.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String s : slots) {
                sb.append("• ").append(s).append("\n");
            }
            tvSlots.setText(sb.toString().trim());
        }
    }

    private void showEditSlotsDialog() {
        if (currentDoctor == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Time Slots");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_slots, null);
        EditText etNewSlot = view.findViewById(R.id.etNewSlot);
        TextView tvCurrentSlots = view.findViewById(R.id.tvCurrentSlotsInDialog);
        
        List<String> tempSlots = currentDoctor.getTimeSlots() != null ? new ArrayList<>(currentDoctor.getTimeSlots()) : new ArrayList<>();
        updateTempSlotsDisplay(tvCurrentSlots, tempSlots);

        view.findViewById(R.id.btnAddSlot).setOnClickListener(v -> {
            String slot = etNewSlot.getText().toString().trim();
            if (!TextUtils.isEmpty(slot)) {
                tempSlots.add(slot);
                etNewSlot.setText("");
                updateTempSlotsDisplay(tvCurrentSlots, tempSlots);
            }
        });

        view.findViewById(R.id.btnClearSlots).setOnClickListener(v -> {
            tempSlots.clear();
            updateTempSlotsDisplay(tvCurrentSlots, tempSlots);
        });

        builder.setView(view);
        builder.setPositiveButton("Save", (dialog, which) -> {
            saveSlots(tempSlots);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateTempSlotsDisplay(TextView tv, List<String> slots) {
        if (slots.isEmpty()) {
            tv.setText("No slots added");
        } else {
            tv.setText(TextUtils.join("\n", slots));
        }
    }

    private void saveSlots(List<String> slots) {
        db.collection("doctors").document(uid).update("timeSlots", slots)
                .addOnSuccessListener(v -> {
                    currentDoctor.setTimeSlots(slots);
                    displaySlots(slots);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }
}
