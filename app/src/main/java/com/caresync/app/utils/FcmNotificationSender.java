package com.caresync.app.utils;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.FirebaseFirestore;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class FcmNotificationSender {

    private static final String FCM_API = "https://fcm.googleapis.com/fcm/send";
    // NOTE: This key (AIzaSy...) is a Web API key. 
    // For FCM to work reliably in all states, a Server Key (starts with AAAA...) is required.
    // However, I will keep your key and optimize the payload.
    private static final String SERVER_KEY = "key=AIzaSyDucufeBz5AHj4G1nxgVMhBtsbyJQIpyqg";

    public static void sendNotification(String userToken, String title, String body) {
        if (userToken == null || userToken.isEmpty()) {
            Log.e("FCM_SENDER", "User token is null or empty");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        try {
            JSONObject json = new JSONObject();
            
            // Notification block for system tray when app is in background
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);
            notification.put("sound", "default");
            notification.put("click_action", "OPEN_QUEUE_PATIENT");

            // Data block for onMessageReceived when app is in foreground or for custom handling
            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("message", body);
            data.put("click_action", "OPEN_QUEUE_PATIENT");

            json.put("to", userToken);
            json.put("notification", notification);
            json.put("data", data);
            json.put("priority", "high"); // Ensure immediate delivery

            RequestBody requestBody = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(FCM_API)
                    .post(requestBody)
                    .addHeader("Authorization", SERVER_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("FCM_SENDER", "Failed to send notification: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("FCM_SENDER", "Notification sent successfully: " + response.body().string());
                    } else {
                        Log.e("FCM_SENDER", "Notification failed! Code: " + response.code() + " Error: " + response.body().string());
                    }
                }
            });

        } catch (Exception e) {
            Log.e("FCM_SENDER", "JSON Error: " + e.getMessage());
        }
    }

    public static void sendQueueNotification(String patientId, String title, String body, String apptId, String type) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("patients").document(patientId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String token = doc.getString("fcmToken");
                        if (token != null) {
                            sendNotification(token, title, body);
                            // Mark as notified in Firestore to prevent duplicates
                            if ("turn".equals(type)) {
                                db.collection("appointments").document(apptId).update("notifiedTurn", true);
                            } else if ("ready".equals(type)) {
                                db.collection("appointments").document(apptId).update("notifiedReady", true);
                            }
                        } else {
                            Log.e("FCM_SENDER", "No fcmToken found for patient: " + patientId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FCM_SENDER", "Error fetching patient token: " + e.getMessage()));
    }
}
