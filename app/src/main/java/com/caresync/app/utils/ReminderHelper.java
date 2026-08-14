package com.caresync.app.utils;

import android.content.Context;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.caresync.app.models.Appointment;
import com.caresync.app.workers.AppointmentReminderWorker;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ReminderHelper {

    public static void scheduleReminders(Context context, Appointment appt) {
        if (appt == null || !"waiting".equals(appt.getStatus())) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date apptDate = sdf.parse(appt.getDate() + " " + appt.getTime());
            if (apptDate == null) return;

            long currentTime = System.currentTimeMillis();

            // 1. One hour before reminder
            long oneHourBefore = apptDate.getTime() - TimeUnit.HOURS.toMillis(1);
            if (oneHourBefore > currentTime) {
                scheduleWork(context, appt.getId() + "_1h", oneHourBefore - currentTime, 
                        "Appointment Reminder", "Your appointment is in 1 hour");
            }

            // 2. Morning of reminder (8:00 AM)
            Calendar morningCal = Calendar.getInstance();
            morningCal.setTime(apptDate);
            morningCal.set(Calendar.HOUR_OF_DAY, 8);
            morningCal.set(Calendar.MINUTE, 0);
            morningCal.set(Calendar.SECOND, 0);

            long morningTime = morningCal.getTimeInMillis();
            if (morningTime > currentTime && morningTime < apptDate.getTime()) {
                scheduleWork(context, appt.getId() + "_morning", morningTime - currentTime,
                        "Appointment Reminder", "Reminder: Your appointment is at " + appt.getTime() + " today");
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static void scheduleWork(Context context, String tag, long delayMs, String title, String message) {
        Data inputData = new Data.Builder()
                .putString("title", title)
                .putString("message", message)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AppointmentReminderWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag(tag)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context).enqueue(request);
    }

    public static void cancelReminders(Context context, String appointmentId) {
        WorkManager.getInstance(context).cancelAllWorkByTag(appointmentId + "_1h");
        WorkManager.getInstance(context).cancelAllWorkByTag(appointmentId + "_morning");
    }
}
