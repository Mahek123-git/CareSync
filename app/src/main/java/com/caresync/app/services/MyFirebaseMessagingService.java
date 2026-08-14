package com.caresync.app.services;

import androidx.annotation.NonNull;
import com.caresync.app.activities.ManageQueueActivity;
import com.caresync.app.activities.QueueStatusActivity;
import com.caresync.app.activities.TodaysAppointmentsActivity;
import com.caresync.app.utils.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            String clickAction = remoteMessage.getData().get("click_action");

            Class<?> targetActivity = null;
            if ("OPEN_QUEUE_PATIENT".equals(clickAction)) {
                targetActivity = QueueStatusActivity.class;
            } else if ("OPEN_QUEUE_DOCTOR".equals(clickAction)) {
                targetActivity = TodaysAppointmentsActivity.class;
            } else if ("OPEN_QUEUE_ADMIN".equals(clickAction)) {
                targetActivity = ManageQueueActivity.class;
            }

            NotificationHelper.showNotification(this, title, body, targetActivity);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // This is usually handled on login, but we could update it here if the user is already logged in
    }
}
