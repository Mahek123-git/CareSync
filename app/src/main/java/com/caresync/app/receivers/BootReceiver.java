package com.caresync.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.caresync.app.models.Appointment;
import com.caresync.app.utils.ReminderHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            rescheduleAllReminders(context);
        }
    }

    private void rescheduleAllReminders(Context context) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        FirebaseFirestore.getInstance().collection("appointments")
                .whereEqualTo("patientId", uid)
                .whereGreaterThanOrEqualTo("date", today)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        Appointment appt = doc.toObject(Appointment.class);
                        appt.setId(doc.getId());
                        ReminderHelper.scheduleReminders(context, appt);
                    }
                });
    }
}
