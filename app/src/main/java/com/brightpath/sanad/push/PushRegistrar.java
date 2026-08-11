package com.brightpath.sanad.push;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.brightpath.sanad.R;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.ui.SplashActivity;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class PushRegistrar {
    public static final String CHANNEL_ID = "sanad_push";
    public static final String EXTRA_PUSH_TYPE = "push_type";
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_SPECIALIST_ID = "specialist_id";
    private static final int PERMISSION_REQUEST = 4101;

    private PushRegistrar() {}

    public static void requestPermissionIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                PERMISSION_REQUEST
        );
    }

    public static void sync(Context context) {
        try {
            if (new TokenStore(context).getToken() == null) {
                return;
            }
            ensureChannel(context);
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> {
                        if (token == null || token.isEmpty()) return;
                        new PushDeviceRepository(context).registerToken(token, new PushDeviceRepository.SimpleCb() {
                            @Override public void ok() {}
                            @Override public void err(Throwable t) {}
                        });
                    })
                    .addOnFailureListener(e -> {});
        } catch (Throwable ignored) {
            // Missing GMS / Firebase on some OEM devices must not crash MainActivity.
        }
    }

    public static void unregisterBeforeLogout(Context context) {
        unregisterBeforeLogout(context, true);
    }

    /** Non-blocking when wait is false — preferred for instant logout UX. */
    public static void unregisterBeforeLogout(Context context, boolean wait) {
        try {
            CountDownLatch latch = wait ? new CountDownLatch(1) : null;
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> {
                        if (token != null && !token.isEmpty()) {
                            new PushDeviceRepository(context).unregisterToken(token, new PushDeviceRepository.SimpleCb() {
                                @Override public void ok() { if (latch != null) latch.countDown(); }
                                @Override public void err(Throwable t) { if (latch != null) latch.countDown(); }
                            });
                        } else if (latch != null) {
                            latch.countDown();
                        }
                    })
                    .addOnFailureListener(e -> { if (latch != null) latch.countDown(); });
            if (latch == null) return;
            try {
                latch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        } catch (Throwable ignored) {
            // Missing GMS / Firebase must not block logout.
        }
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.push_notifications_title),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        manager.createNotificationChannel(channel);
    }

    public static PendingIntent buildDeepLinkIntent(Context context, String type, String sessionId, String specialistId) {
        Intent intent = new Intent(context, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (type != null) intent.putExtra(EXTRA_PUSH_TYPE, type);
        if (sessionId != null) intent.putExtra(EXTRA_SESSION_ID, sessionId);
        if (specialistId != null) intent.putExtra(EXTRA_SPECIALIST_ID, specialistId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, flags);
    }

    public static void showLocalNotification(Context context, String title, String body) {
        showLocalNotification(context, title, body, null);
    }

    public static void showLocalNotification(Context context, String title, String body, PendingIntent contentIntent) {
        ensureChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_home)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if (contentIntent != null) {
            builder.setContentIntent(contentIntent);
        }
        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
    }
}
