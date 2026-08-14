package com.caresync.app.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.caresync.app.activities.QueueStatusActivity;
import com.caresync.app.utils.NotificationHelper;

public class AppointmentReminderWorker extends Worker {

    public AppointmentReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = getInputData().getString("title");
        String message = getInputData().getString("message");

        NotificationHelper.showNotification(getApplicationContext(), 
                title != null ? title : "Appointment Reminder", 
                message != null ? message : "You have an upcoming appointment", 
                QueueStatusActivity.class);

        return Result.success();
    }
}
