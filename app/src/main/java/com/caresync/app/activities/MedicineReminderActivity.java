package com.caresync.app.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.caresync.app.R;
import com.caresync.app.adapters.ReminderAdapter;
import com.caresync.app.models.Medicine;
import com.caresync.app.models.Prescription;
import com.caresync.app.utils.MedicineAlarmReceiver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.*;

public class MedicineReminderActivity extends AppCompatActivity implements ReminderAdapter.OnReminderActionListener {

    private RecyclerView rvReminders;
    private ReminderAdapter adapter;
    private List<Medicine> medicines = new ArrayList<>();
    private Map<Medicine, String> medicinePrescriptionMap = new HashMap<>(); // Medicine -> PrescriptionDocId
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_reminder);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ImageView ivBack = findViewById(R.id.ivBack);
        rvReminders = findViewById(R.id.rvReminders);

        ivBack.setOnClickListener(v -> finish());

        adapter = new ReminderAdapter(medicines, this);
        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        rvReminders.setAdapter(adapter);

        checkExactAlarmPermission();
        loadMedicines();
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private void loadMedicines() {
        db.collection("prescriptions")
                .whereEqualTo("patientId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    medicines.clear();
                    medicinePrescriptionMap.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Prescription rx = doc.toObject(Prescription.class);
                        if (rx.getMedicines() != null) {
                            for (Medicine m : rx.getMedicines()) {
                                medicines.add(m);
                                medicinePrescriptionMap.put(m, doc.getId());
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onSetTime(Medicine medicine, String slot) {
        new TimePickerDialog(this, (view, hour, minute) -> {
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            switch (slot) {
                case "morning": medicine.setMorningTime(timeStr); break;
                case "afternoon": medicine.setAfternoonTime(timeStr); break;
                case "night": medicine.setNightTime(timeStr); break;
            }
            
            // Save time to Firestore
            String rxId = medicinePrescriptionMap.get(medicine);
            if (rxId != null) {
                db.collection("prescriptions").document(rxId).get().addOnSuccessListener(doc -> {
                    Prescription rx = doc.toObject(Prescription.class);
                    if (rx != null && rx.getMedicines() != null) {
                        for (Medicine m : rx.getMedicines()) {
                            if (m.getName().equals(medicine.getName())) {
                                m.setMorningTime(medicine.getMorningTime());
                                m.setAfternoonTime(medicine.getAfternoonTime());
                                m.setNightTime(medicine.getNightTime());
                            }
                        }
                        db.collection("prescriptions").document(rxId).set(rx);
                    }
                });
            }

            adapter.notifyDataSetChanged();
            scheduleAlarm(medicine.getName(), slot, hour, minute);
            Toast.makeText(this, "Reminder set for " + timeStr, Toast.LENGTH_SHORT).show();
        }, 8, 0, true).show();
    }

    @Override
    public void onDelete(Medicine medicine) {
        String rxId = medicinePrescriptionMap.get(medicine);
        if (rxId != null) {
            db.collection("prescriptions").document(rxId).get().addOnSuccessListener(doc -> {
                Prescription rx = doc.toObject(Prescription.class);
                if (rx != null && rx.getMedicines() != null) {
                    List<Medicine> updatedList = new ArrayList<>();
                    for (Medicine m : rx.getMedicines()) {
                        if (!m.getName().equals(medicine.getName())) {
                            updatedList.add(m);
                        }
                    }
                    
                    // Cancel all alarms for this medicine
                    cancelAlarms(medicine.getName());
                    
                    if (updatedList.isEmpty()) {
                        // If no medicines left, we could delete prescription, but better to just clear list
                        rx.setMedicines(updatedList);
                        db.collection("prescriptions").document(rxId).set(rx).addOnSuccessListener(v -> loadMedicines());
                    } else {
                        rx.setMedicines(updatedList);
                        db.collection("prescriptions").document(rxId).set(rx).addOnSuccessListener(v -> loadMedicines());
                    }
                    Toast.makeText(this, "Medicine reminder removed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void cancelAlarms(String medName) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        String[] slots = {"morning", "afternoon", "night"};
        for (String slot : slots) {
            Intent intent = new Intent(this, MedicineAlarmReceiver.class);
            int reqCode = (medName + slot).hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(this, reqCode, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null && alarmManager != null) {
                alarmManager.cancel(pi);
            }
        }
    }

    private void scheduleAlarm(String medName, String slot, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MedicineAlarmReceiver.class);
        intent.putExtra("medName", medName);
        intent.putExtra("slot", slot);

        int reqCode = (medName + slot).hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(this, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        }
    }
}
