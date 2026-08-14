package com.caresync.app.models;

public class Appointment {
    private String id;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String hospitalName;
    private String specialization;
    private String illness; // Added field for what to diagnose
    private String date;
    private String time;
    private int queueNumber;
    private String status; // "waiting", "present", "absent", "cancelled", "completed", "in progress"
    private boolean emergency;
    private long timestamp;
    
    // Notification flags to prevent duplicates
    private boolean notifiedTurn;
    private boolean notifiedReady;
    
    // Emergency alert from doctor
    private boolean doctorEmergency;

    public Appointment() {}

    public Appointment(String id, String patientId, String patientName,
                       String doctorId, String doctorName, String hospitalName,
                       String date, String time, int queueNumber, boolean emergency) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.hospitalName = hospitalName;
        this.date = date;
        this.time = time;
        this.queueNumber = queueNumber;
        this.emergency = emergency;
        this.status = "waiting";
        this.timestamp = System.currentTimeMillis();
        this.notifiedTurn = false;
        this.notifiedReady = false;
        this.doctorEmergency = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getIllness() { return illness; }
    public void setIllness(String illness) { this.illness = illness; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getQueueNumber() { return queueNumber; }
    public void setQueueNumber(int queueNumber) { this.queueNumber = queueNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isEmergency() { return emergency; }
    public void setEmergency(boolean emergency) { this.emergency = emergency; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isNotifiedTurn() { return notifiedTurn; }
    public void setNotifiedTurn(boolean notifiedTurn) { this.notifiedTurn = notifiedTurn; }

    public boolean isNotifiedReady() { return notifiedReady; }
    public void setNotifiedReady(boolean notifiedReady) { this.notifiedReady = notifiedReady; }

    public boolean isDoctorEmergency() { return doctorEmergency; }
    public void setDoctorEmergency(boolean doctorEmergency) { this.doctorEmergency = doctorEmergency; }
}
