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
import com.caresync.app.models.Doctor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AdminDoctorsActivity extends AppCompatActivity {

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

        tvTitle.setText("Doctors");
        ivBack.setOnClickListener(v -> finish());

        adapter = new PersonAdapter(personList);
        rvList.setLayoutManager(new LinearLayoutManager(this));
        rvList.setAdapter(adapter);

        loadHospitalThenDoctors();
    }

    private void loadHospitalThenDoctors() {
        db.collection("admins").document(uid).get()
                .addOnSuccessListener(adDoc -> {
                    String hospital = adDoc.getString("hospitalName");
                    if (hospital == null) return;
                    db.collection("doctors")
                            .whereEqualTo("hospitalName", hospital)
                            .get()
                            .addOnSuccessListener(snap -> {
                                personList.clear();
                                for (QueryDocumentSnapshot doc : snap) {
                                    Doctor d = doc.toObject(Doctor.class);
                                    String name = "Dr. " + (d.getName() != null ? d.getName() : "Unknown");
                                    String spec = d.getSpecialization() != null ? d.getSpecialization() : "General";
                                    
                                    StringBuilder subtitle = new StringBuilder(spec);
                                    if (d.getTimeSlots() != null && !d.getTimeSlots().isEmpty()) {
                                        subtitle.append("\nAvailable: ");
                                        for (int i = 0; i < d.getTimeSlots().size(); i++) {
                                            subtitle.append(d.getTimeSlots().get(i));
                                            if (i < d.getTimeSlots().size() - 1) subtitle.append(", ");
                                        }
                                    } else {
                                        subtitle.append("\nNo slots added");
                                    }

                                    personList.add(new String[]{
                                            name,
                                            subtitle.toString(),
                                            doc.getId()
                                    });
                                }
                                adapter.notifyDataSetChanged();
                                tvEmpty.setVisibility(personList.isEmpty() ? View.VISIBLE : View.GONE);
                            });
                });
    }
}
