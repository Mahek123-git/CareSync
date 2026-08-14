package com.caresync.app.utils;

import com.caresync.app.models.Appointment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class QueueNotificationManager {

    /**
     * Handles notifications when a single appointment status changes (e.g. to "In Progress")
     */
    public static void handleStatusUpdate(Appointment updatedAppt) {
        String status = updatedAppt.getStatus();
        int qNum = updatedAppt.getQueueNumber();

        // 1. "Your Turn Now" logic: Mark as in progress OR reached #1
        if ("in progress".equals(status) || qNum == 1) {
            FcmNotificationSender.sendQueueNotification(
                    updatedAppt.getPatientId(),
                    "Your Turn Now",
                    "Please proceed to the doctor's cabin.",
                    updatedAppt.getId(),
                    "turn"
            );
        }

        // 2. "Stay Ready" logic: If this patient is now "In Progress", notify the NEXT patient (#2)
        if ("in progress".equals(status)) {
            // Re-fetch all to find the next patient without requiring a composite index
            handleQueueShift(updatedAppt.getHospitalName(), updatedAppt.getDoctorId(), updatedAppt.getDate());
        }
    }

    /**
     * Handles notifications for all patients after the queue has been shifted
     */
    public static void handleQueueShift(String hospital, String doctorId, String date) {
        handleQueueShift(hospital, doctorId, date, false, -1);
    }

    /**
     * Overloaded to handle specific cancellation messages
     */
    public static void handleQueueShift(String hospital, String doctorId, String date, boolean isCancellation, int cancelledQueueNum) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("appointments")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        Appointment a = doc.toObject(Appointment.class);
                        a.setId(doc.getId());
                        
                        if ("cancelled".equals(a.getStatus()) || "completed".equals(a.getStatus()) || "absent".equals(a.getStatus())) {
                            continue;
                        }

                        // If it's a cancellation, notify everyone who moved up
                        if (isCancellation && a.getQueueNumber() >= cancelledQueueNum) {
                            FcmNotificationSender.sendQueueNotification(
                                    a.getPatientId(),
                                    "Queue Update",
                                    "A patient ahead of you cancelled their appointment. You have moved up in the queue, please come early!",
                                    a.getId(),
                                    "shift"
                            );
                        }

                        // Standard turn/ready notifications
                        if (a.getQueueNumber() == 1) {
                            FcmNotificationSender.sendQueueNotification(
                                    a.getPatientId(),
                                    "Your Turn Now",
                                    "It's your turn! Please go to the cabin.",
                                    a.getId(),
                                    "turn"
                            );
                        } else if (a.getQueueNumber() == 2) {
                            FcmNotificationSender.sendQueueNotification(
                                    a.getPatientId(),
                                    "Stay Ready",
                                    "You are next in line. Please stay ready.",
                                    a.getId(),
                                    "ready"
                            );
                        }
                    }
                });
    }

    public static void handleQueueMove(Appointment appt1, Appointment appt2) {
        handleStatusUpdate(appt1);
        handleStatusUpdate(appt2);
    }
}
