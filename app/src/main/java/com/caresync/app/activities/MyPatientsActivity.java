package com.caresync.app.activities;

import android.content.Intent;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyPatientsActivity extends AppCompatActivity {

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

        tvTitle.setText("My Patients");
        ivBack.setOnClickListener(v -> finish());

        findViewById(R.id.etSearch).setVisibility(View.GONE);

        adapter = new PersonAdapter(personList);
        adapter.setOnPersonClickListener(person -> {
            String patientId = person[2];
            String patientName = person[0];
            Intent intent = new Intent(MyPatientsActivity.this, PatientHistoryActivity.class);
            intent.putExtra("patientId", patientId);
            intent.putExtra("patientName", patientName);
            startActivity(intent);
        });
        
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.setAdapter(adapter);

        loadPatients();
    }

    private void loadPatients() {
        db.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    personList.clear();
                    Set<String> seen = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        String pid = doc.getString("patientId");
                        String pname = doc.getString("patientName");
                        if (pid != null && !seen.contains(pid)) {
                            seen.add(pid);
                            personList.add(new String[]{
                                pname != null ? pname : "Unknown", 
                                "Previous patient",
                                pid
                            });
                        }
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(personList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }
}
