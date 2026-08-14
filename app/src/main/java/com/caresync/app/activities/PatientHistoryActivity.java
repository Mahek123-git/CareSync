package com.caresync.app.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.adapters.PrescriptionAdapter;
import com.caresync.app.models.Prescription;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PatientHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private TextView tvEmpty;
    private PrescriptionAdapter adapter;
    private final List<Prescription> historyList = new ArrayList<>();
    private FirebaseFirestore db;
    private String patientId, patientName, currentDoctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_prescription);

        db = FirebaseFirestore.getInstance();
        currentDoctorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        patientId = getIntent().getStringExtra("patientId");
        patientName = getIntent().getStringExtra("patientName");

        ImageView ivBack = findViewById(R.id.ivBack);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvHistory = findViewById(R.id.rvPrescriptions);

        // Fix: getParent() returns ViewParent, so we cast it to ViewGroup to access child views
        if (ivBack.getParent() instanceof ViewGroup) {
            ViewGroup header = (ViewGroup) ivBack.getParent();
            for (int i = 0; i < header.getChildCount(); i++) {
                View child = header.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setText(patientName + "'s History");
                }
            }
        }

        ivBack.setOnClickListener(v -> finish());

        adapter = new PrescriptionAdapter(historyList);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        loadPatientHistory();
    }

    private void loadPatientHistory() {
        if (patientId == null || currentDoctorId == null) return;

        db.collection("prescriptions")
                .whereEqualTo("patientId", patientId)
                .whereEqualTo("doctorId", currentDoctorId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    historyList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Prescription rx = doc.toObject(Prescription.class);
                        historyList.add(rx);
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    // Fallback if index is not yet created
                    db.collection("prescriptions")
                            .whereEqualTo("patientId", patientId)
                            .whereEqualTo("doctorId", currentDoctorId)
                            .get()
                            .addOnSuccessListener(snap -> {
                                historyList.clear();
                                for (QueryDocumentSnapshot doc : snap) {
                                    historyList.add(doc.toObject(Prescription.class));
                                }
                                // Sort manually
                                historyList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                                adapter.notifyDataSetChanged();
                                tvEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                            });
                });
    }
}
