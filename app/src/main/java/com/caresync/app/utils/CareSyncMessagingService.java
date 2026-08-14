package com.caresync.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.caresync.app.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CareSyncMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "caresync_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "CareSync";
        String body = "";

        if (message.getNotification() != null) {
            title = message.getNotification().getTitle() != null
                    ? message.getNotification().getTitle() : title;
            body = message.getNotification().getBody() != null
                    ? message.getNotification().getBody() : body;
        } else if (!message.getData().isEmpty()) {
            title = message.getData().getOrDefault("title", title);
            body = message.getData().getOrDefault("message", body);
        }

        showNotification(title, body);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Token refresh handled in PatientRegistrationActivity
    }

    private void showNotification(String title, String body) {
        createChannel();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_caresync_notif)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        if (nm != null) nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "CareSync Notifications", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
