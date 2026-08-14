package com.caresync.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.caresync.app.R;

public class MedicineAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medicine_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String medName = intent.getStringExtra("medName");
        String slot = intent.getStringExtra("slot");

        if (medName == null) return;

        createChannel(context);

        String title = "💊 Medicine Reminder";
        String message = "Time to take " + medName;
        if (slot != null) {
            switch (slot) {
                case "morning": message += " (Morning dose)"; break;
                case "afternoon": message += " (Afternoon dose)"; break;
                case "night": message += " (Night dose)"; break;
            }
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medicine_notif)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (nm != null) {
            nm.notify((medName + slot).hashCode(), builder.build());
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Medicine Reminders", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminds you to take your medicines on time");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
