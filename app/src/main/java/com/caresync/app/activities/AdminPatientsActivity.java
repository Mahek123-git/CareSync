package com.caresync.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.adapters.PersonAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.*;

public class AdminPatientsActivity extends AppCompatActivity {

    private RecyclerView rvList;
    private TextView tvEmpty, tvTitle;
    private PersonAdapter adapter;
    private List<String[]> personList = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_list);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ImageView ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvList = findViewById(R.id.rvList);
        findViewById(R.id.etSearch).setVisibility(View.GONE);

        tvTitle.setText("Patients");
        ivBack.setOnClickListener(v -> finish());

        adapter = new PersonAdapter(personList);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.setAdapter(adapter);

        loadHospitalThenPatients();
    }

    private void loadHospitalThenPatients() {
        db.collection("admins").document(uid).get()
                .addOnSuccessListener(adDoc -> {
                    String hospital = adDoc.getString("hospitalName");
                    if (hospital == null) return;
                    db.collection("appointments")
                            .whereEqualTo("hospitalName", hospital)
                            .get()
                            .addOnSuccessListener(snap -> {
                                personList.clear();
                                Set<String> seen = new HashSet<>();
                                for (QueryDocumentSnapshot doc : snap) {
                                    String pid = doc.getString("patientId");
                                    String pname = doc.getString("patientName");
                                    String date = doc.getString("date");
                                    if (pid != null && !seen.contains(pid)) {
                                        seen.add(pid);
                                        personList.add(new String[]{
                                                pname != null ? pname : "Unknown",
                                                "Last visit: " + (date != null ? date : "—")
                                        });
                                    }
                                }
                                adapter.notifyDataSetChanged();
                                tvEmpty.setVisibility(personList.isEmpty() ? View.VISIBLE : View.GONE);
                            });
                });
    }
}
