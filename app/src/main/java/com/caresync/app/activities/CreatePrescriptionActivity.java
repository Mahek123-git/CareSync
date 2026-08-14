package com.caresync.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.caresync.app.R;
import com.caresync.app.models.Medicine;
import com.caresync.app.models.Prescription;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.*;

public class CreatePrescriptionActivity extends AppCompatActivity {

    private AutoCompleteTextView actvPatient;
    private TextInputEditText etDiagnosis, etNotes;
    private LinearLayout layoutMedicines;
    private MaterialButton btnAddMed, btnSave;
    private FirebaseFirestore db;
    private String uid, doctorName;
    private final List<View> medicineViews = new ArrayList<>();
    private final Map<String, String> patientMap = new HashMap<>(); // Name -> ID
    private final List<String> patientNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_prescription);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ImageView ivBack = findViewById(R.id.ivBack);
        actvPatient = findViewById(R.id.actvPatient);
        etDiagnosis = findViewById(R.id.etDiagnosis);
        etNotes = findViewById(R.id.etNotes);
        layoutMedicines = findViewById(R.id.layoutMedicines);
        btnAddMed = findViewById(R.id.btnAddMedicine);
        btnSave = findViewById(R.id.btnSavePrescription);

        ivBack.setOnClickListener(v -> finish());
        btnAddMed.setOnClickListener(v -> addMedicineRow());
        btnSave.setOnClickListener(v -> savePrescription());

        addMedicineRow(); // Add first row
        loadDoctorName();
        loadPatients();
    }

    private void loadDoctorName() {
        db.collection("doctors").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) doctorName = doc.getString("name");
                });
    }

    private void loadPatients() {
        db.collection("patients").get().addOnSuccessListener(snap -> {
            patientMap.clear();
            patientNames.clear();
            for (QueryDocumentSnapshot doc : snap) {
                String name = doc.getString("name");
                if (name != null) {
                    patientMap.put(name, doc.getId());
                    patientNames.add(name);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, patientNames);
            actvPatient.setAdapter(adapter);
        });
    }

    private void addMedicineRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.item_medicine_input, layoutMedicines, false);
        
        CheckBox cbMorning = row.findViewById(R.id.cbMed1Morning);
        CheckBox cbAfternoon = row.findViewById(R.id.cbMed1Afternoon);
        CheckBox cbNight = row.findViewById(R.id.cbMed1Night);
        
        RadioGroup rgMorning = row.findViewById(R.id.rgMorning);
        RadioGroup rgAfternoon = row.findViewById(R.id.rgAfternoon);
        RadioGroup rgNight = row.findViewById(R.id.rgNight);

        cbMorning.setOnCheckedChangeListener((b, isChecked) -> rgMorning.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        cbAfternoon.setOnCheckedChangeListener((b, isChecked) -> rgAfternoon.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        cbNight.setOnCheckedChangeListener((b, isChecked) -> rgNight.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        layoutMedicines.addView(row);
        medicineViews.add(row);
    }

    private void savePrescription() {
        String patName = actvPatient.getText().toString().trim();
        String diag = etDiagnosis.getText() != null ? etDiagnosis.getText().toString().trim() : "";
        String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        if (TextUtils.isEmpty(patName)) { actvPatient.setError("Required"); return; }
        if (TextUtils.isEmpty(diag)) { etDiagnosis.setError("Required"); return; }

        String patientId = patientMap.get(patName);
        if (patientId == null) {
            Toast.makeText(this, "Select a valid patient from the list", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Medicine> medicines = new ArrayList<>();
        for (View v : medicineViews) {
            TextInputEditText etMedName = v.findViewById(R.id.etMed1Name);
            CheckBox cbMorning = v.findViewById(R.id.cbMed1Morning);
            CheckBox cbAfternoon = v.findViewById(R.id.cbMed1Afternoon);
            CheckBox cbNight = v.findViewById(R.id.cbMed1Night);
            
            if (etMedName == null) continue;
            String medName = etMedName.getText() != null ? etMedName.getText().toString().trim() : "";
            
            if (!TextUtils.isEmpty(medName)) {
                Medicine m = new Medicine(medName, cbMorning.isChecked(), cbAfternoon.isChecked(), cbNight.isChecked());
                
                if (cbMorning.isChecked()) {
                    RadioGroup rg = v.findViewById(R.id.rgMorning);
                    int checkedId = rg.getCheckedRadioButtonId();
                    if (checkedId != -1) {
                        m.setMorningTiming(((RadioButton) v.findViewById(checkedId)).getText().toString() + " Meal");
                    }
                }
                if (cbAfternoon.isChecked()) {
                    RadioGroup rg = v.findViewById(R.id.rgAfternoon);
                    int checkedId = rg.getCheckedRadioButtonId();
                    if (checkedId != -1) {
                        m.setAfternoonTiming(((RadioButton) v.findViewById(checkedId)).getText().toString() + " Meal");
                    }
                }
                if (cbNight.isChecked()) {
                    RadioGroup rg = v.findViewById(R.id.rgNight);
                    int checkedId = rg.getCheckedRadioButtonId();
                    if (checkedId != -1) {
                        m.setNightTiming(((RadioButton) v.findViewById(checkedId)).getText().toString() + " Meal");
                    }
                }
                
                medicines.add(m);
            }
        }

        if (medicines.isEmpty()) {
            Toast.makeText(this, "Add at least one medicine", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        String rxId = db.collection("prescriptions").document().getId();
        Prescription rx = new Prescription(rxId, uid,
                doctorName != null ? "Dr. " + doctorName : "Doctor",
                patientId, patName, diag, medicines, notes, date);

        db.collection("prescriptions").document(rxId).set(rx)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Prescription saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
