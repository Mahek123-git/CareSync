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
import com.caresync.app.activities.QueueStatusActivity;
import com.caresync.app.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QueueWatcherService extends Service {

    private static final String CHANNEL_ID = "QueueWatcherChannel";
    private FirebaseFirestore db;
    private String uid;
    private ListenerRegistration apptListener;
    private boolean turnNotified = false;
    private boolean readyNotified = false;
    private boolean emergencyNotified = false;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CareSync Queue Monitor")
                .setContentText("Watching your queue status...")
                .setSmallIcon(R.drawable.ic_queue)
                .build();
        
        startForeground(101, notification);
        startListening();
    }

    private void startListening() {
        if (uid == null) return;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        apptListener = db.collection("appointments")
                .whereEqualTo("patientId", uid)
                .whereEqualTo("date", today)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String status = doc.getString("status");
                        if ("cancelled".equals(status) || "completed".equals(status)) continue;

                        Boolean notifiedTurn = doc.getBoolean("notifiedTurn");
                        Boolean notifiedReady = doc.getBoolean("notifiedReady");
                        Boolean doctorEmergency = doc.getBoolean("doctorEmergency");

                        if (Boolean.TRUE.equals(doctorEmergency) && !emergencyNotified) {
                            String docName = doc.getString("doctorName");
                            NotificationHelper.showNotification(this, "Doctor Emergency Alert", 
                                    "Dr. " + docName + " says: I have emergency, next appointments resume soon", QueueStatusActivity.class);
                            emergencyNotified = true;
                        } else if (!Boolean.TRUE.equals(doctorEmergency)) {
                            emergencyNotified = false; 
                        }

                        if ((Boolean.TRUE.equals(notifiedTurn) || "in progress".equals(status)) && !turnNotified) {
                            NotificationHelper.showNotification(this, "Your Turn Now", 
                                    "Please proceed to the doctor's cabin immediately.", QueueStatusActivity.class);
                            turnNotified = true;
                        } else if (Boolean.TRUE.equals(notifiedReady) && !readyNotified) {
                            NotificationHelper.showNotification(this, "Stay Ready", 
                                    "You are next in line. Please wait near the cabin.", QueueStatusActivity.class);
                            readyNotified = true;
                        }
                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Queue Monitor Service", NotificationManager.IMPORTANCE_LOW);
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
        if (apptListener != null) apptListener.remove();
        super.onDestroy();
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }
}
