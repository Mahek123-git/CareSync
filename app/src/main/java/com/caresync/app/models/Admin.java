package com.caresync.app.models;

public class Admin {
    private String uid;
    private String name;
    private String hospitalName;
    private String hospitalId;
    private String contact;
    private String email;

    public Admin() {}

    public Admin(String uid, String name, String hospitalName, String hospitalId,
                 String contact, String email) {
        this.uid = uid;
        this.name = name;
        this.hospitalName = hospitalName;
        this.hospitalId = hospitalId;
        this.contact = contact;
        this.email = email;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
