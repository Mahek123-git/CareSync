package com.caresync.app.models;

import java.util.List;

public class Prescription {
    private String id;
    private String doctorId;
    private String doctorName;
    private String patientId;
    private String patientName;
    private String diagnosis;
    private List<Medicine> medicines;
    private String notes;
    private long timestamp;
    private String date;

    public Prescription() {}

    public Prescription(String id, String doctorId, String doctorName,
                        String patientId, String patientName,
                        String diagnosis, List<Medicine> medicines,
                        String notes, String date) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.patientId = patientId;
        this.patientName = patientName;
        this.diagnosis = diagnosis;
        this.medicines = medicines;
        this.notes = notes;
        this.date = date;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public List<Medicine> getMedicines() { return medicines; }
    public void setMedicines(List<Medicine> medicines) { this.medicines = medicines; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
