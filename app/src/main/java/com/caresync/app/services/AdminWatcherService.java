package com.caresync.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.caresync.app.R;
import com.caresync.app.activities.ManageQueueActivity;
import com.caresync.app.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.HashSet;
import java.util.Set;

public class AdminWatcherService extends Service {

    private static final String CHANNEL_ID = "AdminWatcherChannel";
    private FirebaseFirestore db;
    private String uid, hospitalName;
    private ListenerRegistration adminListener, doctorListener;
    private Set<String> notifiedDoctors = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CareSync Admin Monitor")
                .setContentText("Monitoring hospital doctor status...")
                .setSmallIcon(R.drawable.ic_admin)
                .build();
        
        startForeground(102, notification);
        loadAdminHospital();
    }

    private void loadAdminHospital() {
        if (uid == null) return;
        adminListener = db.collection("admins").document(uid).addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) {
                hospitalName = doc.getString("hospitalName");
                startWatchingDoctors();
            }
        });
    }

    private void startWatchingDoctors() {
        if (hospitalName == null) return;
        if (doctorListener != null) doctorListener.remove();

        doctorListener = db.collection("doctors")
                .whereEqualTo("hospitalName", hospitalName)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Boolean isEmergency = doc.getBoolean("isEmergency");
                        String docId = doc.getId();
                        String docName = doc.getString("name");

                        if (Boolean.TRUE.equals(isEmergency)) {
                            if (!notifiedDoctors.contains(docId)) {
                                NotificationHelper.showNotification(this, "Doctor Emergency Alert", 
                                        "Dr. " + docName + " says: I have emergency, next appointments resume soon", ManageQueueActivity.class);
                                notifiedDoctors.add(docId);
                            }
                        } else {
                            notifiedDoctors.remove(docId);
                        }
                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Admin Monitor Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (adminListener != null) adminListener.remove();
        if (doctorListener != null) doctorListener.remove();
        super.onDestroy();
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }
}
