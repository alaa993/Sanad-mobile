package com.brightpath.sanad.feature.patient;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.brightpath.sanad.R;

public class PatientTaskReminderWorker extends Worker {
    public static final String KEY_TITLE = "title";
    public static final String KEY_DESC = "description";
    private static final String CHANNEL_ID = "task_reminders";

    public PatientTaskReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull @Override
    public Result doWork() {
        String title = getInputData().getString(KEY_TITLE);
        String desc = getInputData().getString(KEY_DESC);
        showNotification(title != null ? title : "واجب الجلسة",
                desc != null ? desc : getApplicationContext().getString(R.string.task_reminder_default));
        return Result.success();
    }

    private void showNotification(String title, String message){
        NotificationManager nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getApplicationContext().getString(R.string.task_reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getApplicationContext().getString(R.string.task_reminder_channel_desc));
            nm.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notifications)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}
