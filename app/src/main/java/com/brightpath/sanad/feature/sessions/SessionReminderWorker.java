package com.brightpath.sanad.feature.sessions;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.brightpath.sanad.R;
import com.brightpath.sanad.push.PushRegistrar;

public class SessionReminderWorker extends Worker {
    public static final String KEY_TITLE = "title";
    public static final String KEY_BODY = "body";
    public static final String KEY_SESSION_ID = "session_id";
    private static final String CHANNEL_ID = "session_reminders";

    public SessionReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull @Override
    public Result doWork() {
        String title = getInputData().getString(KEY_TITLE);
        String body = getInputData().getString(KEY_BODY);
        int sessionId = getInputData().getInt(KEY_SESSION_ID, -1);
        notifyNow(title, body, sessionId);
        return Result.success();
    }

    private void notifyNow(String title, String body, int sessionId){
        Context context = getApplicationContext();
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.session_reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(context.getString(R.string.session_reminder_channel_desc));
            nm.createNotificationChannel(channel);
        }
        PendingIntent contentIntent = null;
        if (sessionId > 0) {
            contentIntent = PushRegistrar.buildDeepLinkIntent(
                    context,
                    "session",
                    String.valueOf(sessionId),
                    null
            );
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title != null ? title : context.getString(R.string.session_reminder_title))
                .setContentText(body != null ? body : context.getString(R.string.session_reminder_body))
                .setSmallIcon(R.drawable.ic_notifications)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        nm.notify(sessionId > 0 ? sessionId : (int) System.currentTimeMillis(), builder.build());
    }
}
