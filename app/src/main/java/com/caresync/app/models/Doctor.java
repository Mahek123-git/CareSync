package com.caresync.app.models;

import java.util.ArrayList;
import java.util.List;

public class Doctor {
    private String uid;
    private String name;
    private int age;
    private String qualification;
    private String specialization;
    private String hospitalName;
    private String licenseNumber;
    private String licenseUrl;
    private String email;
    private String fcmToken;
    private List<String> timeSlots = new ArrayList<>();
    private boolean isEmergency;

    public Doctor() {}

    public Doctor(String uid, String name, int age, String qualification,
                  String specialization, String hospitalName, String licenseNumber,
                  String licenseUrl, String email) {
        this.uid = uid;
        this.name = name;
        this.age = age;
        this.qualification = qualification;
        this.specialization = specialization;
        this.hospitalName = hospitalName;
        this.licenseNumber = licenseNumber;
        this.licenseUrl = licenseUrl;
        this.email = email;
        this.isEmergency = false;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public List<String> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<String> timeSlots) { this.timeSlots = timeSlots; }

    public boolean isEmergency() { return isEmergency; }
    public void setEmergency(boolean emergency) { isEmergency = emergency; }
}
