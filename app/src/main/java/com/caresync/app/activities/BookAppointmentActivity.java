package com.caresync.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.caresync.app.R;
import com.caresync.app.models.Appointment;
import com.caresync.app.models.Doctor;
import com.caresync.app.utils.QueueNotificationManager;
import com.caresync.app.utils.ReminderHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.*;

public class BookAppointmentActivity extends AppCompatActivity {

    private AutoCompleteTextView autoHospital;
    private Spinner spinnerDoctor;
    private TextInputEditText etDate, etTime, etIllness;
    private TextView tvAvailability;
    private CheckBox cbEmergency;
    private MaterialButton btnBook;
    private FirebaseFirestore db;
    private String uid, patientName;
    private final List<String> hospitalList = new ArrayList<>();
    private final List<Doctor> doctorList = new ArrayList<>();
    private ArrayAdapter<String> hospitalAdapter, doctorAdapter;
    private final List<String> doctorNames = new ArrayList<>();
    private Calendar selectedCal = Calendar.getInstance();

    private boolean isUpdateMode = false;
    private Appointment existingAppt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ImageView ivBack = findViewById(R.id.ivBack);
        autoHospital = findViewById(R.id.autoCompleteHospital);
        spinnerDoctor = findViewById(R.id.spinnerDoctor);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etIllness = findViewById(R.id.etIllness);
        tvAvailability = findViewById(R.id.tvAvailability);
        cbEmergency = findViewById(R.id.cbEmergency);
        btnBook = findViewById(R.id.btnBookAppointment);

        hospitalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, hospitalList);
        autoHospital.setAdapter(hospitalAdapter);

        doctorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, doctorNames);
        doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctor.setAdapter(doctorAdapter);

        ivBack.setOnClickListener(v -> finish());
        etDate.setOnClickListener(v -> pickDate());
        etTime.setOnClickListener(v -> pickTime());
        btnBook.setOnClickListener(v -> bookOrUpdateAppointment());

        autoHospital.setOnItemClickListener((parent, view, position, id) -> filterDoctors());
        
        autoHospital.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterDoctors(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerDoctor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAvailabilityDisplay();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadPatientName();
        loadHospitalsAndDoctors();
        checkForExistingAppointment();
    }

    private void checkForExistingAppointment() {
        String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("appointments")
                .whereEqualTo("patientId", uid)
                .whereEqualTo("date", todayStr)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        String status = doc.getString("status");
                        if ("waiting".equals(status)) {
                            existingAppt = doc.toObject(Appointment.class);
                            existingAppt.setId(doc.getId());
                            isUpdateMode = true;
                            btnBook.setText("Update Appointment");
                            preFillFields();
                            break;
                        }
                    }
                });
    }

    private void preFillFields() {
        if (existingAppt == null) return;
        etDate.setText(existingAppt.getDate());
        etTime.setText(existingAppt.getTime());
        etIllness.setText(existingAppt.getIllness());
        cbEmergency.setChecked(existingAppt.isEmergency());
        autoHospital.setText(existingAppt.getHospitalName(), false);
    }

    private void loadPatientName() {
        db.collection("patients").document(uid).get()
                .addOnSuccessListener(doc -> { if (doc.exists()) patientName = doc.getString("name"); });
    }

    private void loadHospitalsAndDoctors() {
        db.collection("doctors").get().addOnSuccessListener(snap -> {
            Set<String> hospitals = new LinkedHashSet<>();
            doctorList.clear();
            for (QueryDocumentSnapshot doc : snap) {
                Doctor d = doc.toObject(Doctor.class);
                d.setUid(doc.getId());
                if (d.getHospitalName() != null) {
                    hospitals.add(d.getHospitalName());
                    doctorList.add(d);
                }
            }
            hospitalList.clear();
            hospitalList.addAll(hospitals);
            hospitalAdapter.notifyDataSetChanged();
            
            if (isUpdateMode && existingAppt != null) {
                autoHospital.setText(existingAppt.getHospitalName(), false);
            }
            
            filterDoctors();
        });
    }

    private void filterDoctors() {
        String selectedHospital = autoHospital.getText().toString().trim();
        if (selectedHospital.isEmpty()) {
            doctorNames.clear();
            doctorAdapter.notifyDataSetChanged();
            tvAvailability.setVisibility(View.GONE);
            return;
        }

        doctorNames.clear();
        for (Doctor d : doctorList) {
            if (selectedHospital.equalsIgnoreCase(d.getHospitalName())) {
                doctorNames.add("Dr. " + d.getName() + " (" + (d.getSpecialization() != null ? d.getSpecialization() : "Gen") + ")");
            }
        }
        doctorAdapter.notifyDataSetChanged();
        
        if (isUpdateMode && existingAppt != null && selectedHospital.equalsIgnoreCase(existingAppt.getHospitalName())) {
            String target = "Dr. " + existingAppt.getDoctorName();
            for (int i = 0; i < doctorNames.size(); i++) {
                if (doctorNames.get(i).startsWith(target)) {
                    spinnerDoctor.setSelection(i);
                    break;
                }
            }
        }
        
        updateAvailabilityDisplay();
    }

    private void updateAvailabilityDisplay() {
        String hospital = autoHospital.getText().toString().trim();
        int docPos = spinnerDoctor.getSelectedItemPosition();
        
        if (hospital.isEmpty() || docPos < 0 || doctorNames.isEmpty()) {
            tvAvailability.setVisibility(View.GONE);
            return;
        }

        Doctor selectedDoctor = null;
        int cnt = 0;
        for (Doctor d : doctorList) {
            if (hospital.equalsIgnoreCase(d.getHospitalName())) {
                if (cnt == docPos) { selectedDoctor = d; break; }
                cnt++;
            }
        }

        if (selectedDoctor != null && selectedDoctor.getTimeSlots() != null && !selectedDoctor.getTimeSlots().isEmpty()) {
            StringBuilder sb = new StringBuilder("Available Slots:\n");
            for (String slot : selectedDoctor.getTimeSlots()) {
                sb.append("• ").append(slot).append("\n");
            }
            tvAvailability.setText(sb.toString().trim());
            tvAvailability.setVisibility(View.VISIBLE);
        } else {
            tvAvailability.setText("Availability not specified");
            tvAvailability.setVisibility(View.VISIBLE);
        }
    }

    private void pickDate() {
        new DatePickerDialog(this, (v, y, m, d) -> {
            selectedCal.set(y, m, d);
            etDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d));
        }, selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH),
                selectedCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime() {
        new TimePickerDialog(this, (v, h, m) ->
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                selectedCal.get(Calendar.HOUR_OF_DAY), selectedCal.get(Calendar.MINUTE), true).show();
    }

    private void bookOrUpdateAppointment() {
        String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String time = etTime.getText() != null ? etTime.getText().toString().trim() : "";
        String illness = etIllness.getText() != null ? etIllness.getText().toString().trim() : "";
        String hospital = autoHospital.getText().toString().trim();
        boolean isEmergency = cbEmergency.isChecked();

        if (date.isEmpty()) { Toast.makeText(this, "Select date", Toast.LENGTH_SHORT).show(); return; }
        if (time.isEmpty()) { Toast.makeText(this, "Select time", Toast.LENGTH_SHORT).show(); return; }
        if (illness.isEmpty()) { etIllness.setError("Required"); return; }
        if (hospital.isEmpty()) { autoHospital.setError("Required"); return; }
        
        int docPos = spinnerDoctor.getSelectedItemPosition();
        if (doctorNames.isEmpty() || docPos < 0) { Toast.makeText(this, "Select a valid doctor", Toast.LENGTH_SHORT).show(); return; }

        Doctor selectedDoctor = null;
        int cnt = 0;
        for (Doctor d : doctorList) {
            if (hospital.equalsIgnoreCase(d.getHospitalName())) {
                if (cnt == docPos) { selectedDoctor = d; break; }
                cnt++;
            }
        }

        if (selectedDoctor == null) { Toast.makeText(this, "Doctor not found", Toast.LENGTH_SHORT).show(); return; }

        final String doctorId = selectedDoctor.getUid();
        final String doctorName = selectedDoctor.getName();
        final String specialization = selectedDoctor.getSpecialization();
        
        btnBook.setEnabled(false);
        btnBook.setText(isUpdateMode ? "Updating..." : "Booking...");

        if (isUpdateMode && existingAppt != null) {
            handleUpdate(date, time, illness, isEmergency, doctorId, doctorName, specialization, hospital);
        } else {
            handleNewBooking(date, time, illness, isEmergency, doctorId, doctorName, specialization, hospital);
        }
    }

    private void handleUpdate(String date, String time, String illness, boolean isEmergency,
                              String doctorId, String doctorName, String specialization, String hospital) {
        
        final String oldDocId = existingAppt.getDoctorId();
        final String oldDate = existingAppt.getDate();
        final String oldHospital = existingAppt.getHospitalName();
        final int oldQueueNum = existingAppt.getQueueNumber();
        final boolean sameQueue = doctorId.equals(oldDocId) && date.equals(oldDate);

        if (sameQueue) {
            db.collection("appointments")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("date", date)
                    .get()
                    .addOnSuccessListener(snap -> {
                        WriteBatch batch = db.batch();
                        List<Appointment> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snap) {
                            Appointment a = doc.toObject(Appointment.class);
                            a.setId(doc.getId());
                            if (!"cancelled".equals(a.getStatus()) && !"completed".equals(a.getStatus())) {
                                if (doc.getId().equals(existingAppt.getId())) a.setEmergency(isEmergency);
                                list.add(a);
                            }
                        }
                        
                        list.sort((o1, o2) -> {
                            if (o1.isEmergency() != o2.isEmergency()) return o1.isEmergency() ? -1 : 1;
                            return Integer.compare(o1.getQueueNumber(), o2.getQueueNumber());
                        });

                        int calculatedNum = 1;
                        for (int i = 0; i < list.size(); i++) {
                            int qNum = i + 1;
                            Appointment a = list.get(i);
                            if (a.getId().equals(existingAppt.getId())) {
                                calculatedNum = qNum;
                            } else if (a.getQueueNumber() != qNum) {
                                batch.update(db.collection("appointments").document(a.getId()), "queueNumber", qNum);
                            }
                        }

                        final int finalNum = calculatedNum;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("time", time);
                        updates.put("illness", illness);
                        updates.put("emergency", isEmergency);
                        updates.put("queueNumber", finalNum);
                        batch.update(db.collection("appointments").document(existingAppt.getId()), updates);

                        batch.commit().addOnSuccessListener(v -> {
                            Toast.makeText(BookAppointmentActivity.this, "Appointment updated! Serial #" + finalNum, Toast.LENGTH_LONG).show();
                            finish();
                        });
                    });
        } else {
            db.collection("appointments")
                    .whereEqualTo("doctorId", oldDocId)
                    .whereEqualTo("date", oldDate)
                    .get()
                    .addOnSuccessListener(snapOld -> {
                        WriteBatch batch = db.batch();
                        
                        List<Appointment> oldList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapOld) {
                            if (doc.getId().equals(existingAppt.getId())) continue;
                            Appointment a = doc.toObject(Appointment.class);
                            a.setId(doc.getId());
                            if (!"cancelled".equals(a.getStatus()) && !"completed".equals(a.getStatus())) {
                                oldList.add(a);
                            }
                        }
                        oldList.sort((o1, o2) -> {
                            if (o1.isEmergency() != o2.isEmergency()) return o1.isEmergency() ? -1 : 1;
                            return Integer.compare(o1.getQueueNumber(), o2.getQueueNumber());
                        });
                        for (int i = 0; i < oldList.size(); i++) {
                            batch.update(db.collection("appointments").document(oldList.get(i).getId()), "queueNumber", i + 1);
                        }

                        db.collection("appointments")
                                .whereEqualTo("doctorId", doctorId)
                                .whereEqualTo("date", date)
                                .get()
                                .addOnSuccessListener(snapNew -> {
                                    List<Appointment> newList = new ArrayList<>();
                                    for (QueryDocumentSnapshot doc : snapNew) {
                                        Appointment a = doc.toObject(Appointment.class);
                                        a.setId(doc.getId());
                                        if (!"cancelled".equals(a.getStatus()) && !"completed".equals(a.getStatus())) {
                                            newList.add(a);
                                        }
                                    }
                                    
                                    Appointment placeholder = new Appointment();
                                    placeholder.setId(existingAppt.getId());
                                    placeholder.setEmergency(isEmergency);
                                    placeholder.setTimestamp(System.currentTimeMillis());
                                    placeholder.setQueueNumber(9999);
                                    
                                    newList.add(placeholder);
                                    newList.sort((o1, o2) -> {
                                        if (o1.isEmergency() != o2.isEmergency()) return o1.isEmergency() ? -1 : 1;
                                        int qRes = Integer.compare(o1.getQueueNumber(), o2.getQueueNumber());
                                        if (qRes != 0) return qRes;
                                        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
                                    });

                                    int calculatedNewNum = 1;
                                    for (int i = 0; i < newList.size(); i++) {
                                        int qNum = i + 1;
                                        Appointment a = newList.get(i);
                                        if (a.getId().equals(existingAppt.getId())) {
                                            calculatedNewNum = qNum;
                                        } else if (a.getQueueNumber() != qNum) {
                                            batch.update(db.collection("appointments").document(a.getId()), "queueNumber", qNum);
                                        }
                                    }

                                    final int finalNewNum = calculatedNewNum;

                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("date", date);
                                    updates.put("time", time);
                                    updates.put("illness", illness);
                                    updates.put("emergency", isEmergency);
                                    updates.put("doctorId", doctorId);
                                    updates.put("doctorName", doctorName);
                                    updates.put("specialization", specialization);
                                    updates.put("hospitalName", hospital);
                                    updates.put("queueNumber", finalNewNum);
                                    updates.put("timestamp", placeholder.getTimestamp());
                                    updates.put("status", "waiting");
                                    
                                    batch.update(db.collection("appointments").document(existingAppt.getId()), updates);
                                    
                                    batch.commit().addOnSuccessListener(v -> {
                                        Toast.makeText(BookAppointmentActivity.this, "Appointment moved! Serial #" + finalNewNum, Toast.LENGTH_LONG).show();
                                        QueueNotificationManager.handleQueueShift(oldHospital, oldDocId, oldDate, true, oldQueueNum);
                                        QueueNotificationManager.handleQueueShift(hospital, doctorId, date);
                                        finish();
                                    });
                                });
                    });
        }
    }

    private void handleNewBooking(String date, String time, String illness, boolean isEmergency,
                                 String doctorId, String doctorName, String specialization, String hospital) {
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = db.batch();
                    List<Appointment> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Appointment a = doc.toObject(Appointment.class);
                        a.setId(doc.getId());
                        if (!"cancelled".equals(a.getStatus()) && !"completed".equals(a.getStatus())) {
                            list.add(a);
                        }
                    }
                    
                    String apptId = db.collection("appointments").document().getId();
                    Appointment newAppt = new Appointment(apptId, uid, patientName != null ? patientName : "Patient",
                            doctorId, doctorName, hospital, date, time, 9999, isEmergency);
                    newAppt.setSpecialization(specialization);
                    newAppt.setIllness(illness);
                    
                    list.add(newAppt);
                    list.sort((o1, o2) -> {
                        if (o1.isEmergency() != o2.isEmergency()) return o1.isEmergency() ? -1 : 1;
                        int qRes = Integer.compare(o1.getQueueNumber(), o2.getQueueNumber());
                        if (qRes != 0) return qRes;
                        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
                    });
                    
                    int calculatedAssignedNum = 1;
                    for (int i = 0; i < list.size(); i++) {
                        int qNum = i + 1;
                        Appointment a = list.get(i);
                        if (a.getId().equals(apptId)) {
                            calculatedAssignedNum = qNum;
                        } else if (a.getQueueNumber() != qNum) {
                            batch.update(db.collection("appointments").document(a.getId()), "queueNumber", qNum);
                        }
                    }
                    
                    final int assignedNum = calculatedAssignedNum;
                    
                    newAppt.setQueueNumber(assignedNum);
                    batch.set(db.collection("appointments").document(apptId), newAppt);
                    batch.commit().addOnSuccessListener(v -> {
                        Toast.makeText(BookAppointmentActivity.this, "Appointment booked! Serial #" + assignedNum, Toast.LENGTH_LONG).show();
                        ReminderHelper.scheduleReminders(BookAppointmentActivity.this, newAppt);
                        finish();
                    });
                });
    }
}
