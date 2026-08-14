package com.caresync.app.activities;

import android.os.Bundle;
import android.view.View;
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

public class PatientPrescriptionActivity extends AppCompatActivity {

    private RecyclerView rvPrescriptions;
    private TextView tvEmpty;
    private PrescriptionAdapter adapter;
    private final List<Prescription> rxList = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_prescription);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ImageView ivBack = findViewById(R.id.ivBack);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvPrescriptions = findViewById(R.id.rvPrescriptions);

        ivBack.setOnClickListener(v -> finish());

        adapter = new PrescriptionAdapter(rxList);
        rvPrescriptions.setLayoutManager(new LinearLayoutManager(this));
        rvPrescriptions.setAdapter(adapter);

        loadPrescriptions();
    }

    private void loadPrescriptions() {
        // Query prescriptions where patientId matches current user's UID
        db.collection("prescriptions")
                .whereEqualTo("patientId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    rxList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Prescription rx = doc.toObject(Prescription.class);
                        rxList.add(rx);
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(rxList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    // If timestamp index is missing, fallback to name search
                    loadPrescriptionsFallback();
                });
    }

    private void loadPrescriptionsFallback() {
        db.collection("patients").document(uid).get().addOnSuccessListener(doc -> {
            String name = doc.getString("name");
            if (name != null) {
                db.collection("prescriptions")
                        .whereEqualTo("patientName", name)
                        .get()
                        .addOnSuccessListener(snap -> {
                            rxList.clear();
                            for (QueryDocumentSnapshot d : snap) {
                                rxList.add(d.toObject(Prescription.class));
                            }
                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(rxList.isEmpty() ? View.VISIBLE : View.GONE);
                        });
            }
        });
    }
}
