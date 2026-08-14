# CareSync - Healthcare Android Application
## Setup Instructions

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 8+
- Firebase account

---

## Step 1: Open Project in Android Studio
1. Unzip `CareSync.zip`
2. Open Android Studio → **File > Open** → select the `CareSync` folder
3. Wait for Gradle sync to complete

---

## Step 2: Firebase Setup (REQUIRED)
1. Go to [https://console.firebase.google.com](https://console.firebase.google.com)
2. Click **Add Project** → name it `CareSync`
3. Click **Add Android App**
   - Package name: `com.caresync.app`
   - App nickname: CareSync
4. Download `google-services.json`
5. **Replace** `app/google-services.json` with your downloaded file

### Enable Firebase Services:
- **Authentication** → Sign-in method → Enable **Email/Password**
- **Firestore Database** → Create database → Start in **Test mode**
- **Storage** → Get started → Start in **Test mode**
- **Cloud Messaging** → Already enabled by default

### Firestore Collections (auto-created on first use):
- `doctors` — doctor profiles
- `patients` — patient profiles
- `admins` — admin profiles
- `appointments` — all bookings
- `prescriptions` — issued prescriptions
- `notifications` — queue change alerts

### Paste these Firestore Rules (Firebase Console → Firestore → Rules):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## Step 3: Build & Run
1. Connect an Android device (API 24+) or start an emulator
2. Click **Run ▶** in Android Studio
3. App will compile and launch

---

## App Flow

```
Splash Screen
    ↓
Login / Sign Up (Firebase Auth)
    ↓
Select Module
  ├── Doctor → Register → Dashboard
  │              ├── Today's Appointments
  │              ├── My Patients
  │              ├── Create Prescription
  │              └── Profile
  ├── Patient → Register → Dashboard
  │              ├── Book Appointment
  │              ├── Queue Status (live updates)
  │              ├── My Prescriptions
  │              └── Medicine Reminders (alarm-based)
  └── Admin → Register → Dashboard
               ├── Manage Queue (toggle status, shuffle)
               ├── Doctors (filtered by hospital)
               └── Patients (filtered by hospital)
```

---

## Module Descriptions

### Doctor Module
- Register with name, age, qualification, specialization, hospital, license number, license file upload
- Dashboard with 4 cards: Today's Appointments, My Patients, Prescription, Profile
- Today's Appointments: real-time list with queue number and status
- My Patients: unique patients who have booked
- Create Prescription: patient name, diagnosis, medicines with morning/afternoon/night slots, notes

### Patient Module
- Register with name, age, gender, phone
- Book Appointment: select hospital → filter doctors → pick date & time → auto queue number
- Queue Status: live Firestore listener shows queue number, updates when admin shuffles
- Prescriptions: view all prescriptions issued by doctors
- Medicine Reminders: set alarm times for morning/afternoon/night doses

### Admin Module
- Register with name, hospital name, hospital ID
- Each admin is scoped to their hospital only
- Manage Queue: tap status badge to toggle Present/Absent; tap arrow to move patient up (emergency shuffle)
- Doctors: all doctors registered to this hospital
- Patients: all patients who visited this hospital

---

## Tech Stack
| Layer | Technology |
|-------|-----------|
| Language | Java |
| UI | XML Layouts |
| Auth | Firebase Authentication |
| Database | Firebase Firestore |
| Storage | Firebase Storage (license docs) |
| Notifications | Firebase Cloud Messaging + AlarmManager |
| Architecture | Activity-based MVC |

---

## Project Structure
```
app/src/main/
├── java/com/caresync/app/
│   ├── activities/          (17 Activity classes)
│   ├── adapters/            (5 RecyclerView Adapters)
│   ├── models/              (6 Data Models)
│   └── utils/               (AlarmReceiver + FCM Service)
└── res/
    ├── layout/              (20 XML layouts)
    ├── drawable/            (45 vector icons + shapes)
    ├── values/              (colors, strings, themes, dimens)
    └── xml/                 (file_paths.xml)
```
