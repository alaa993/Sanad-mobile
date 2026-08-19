package com.brightpath.sanad.data;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.ui.LoginActivity;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Last-resort handler. Worker-thread crashes must not restart MainActivity
 * (that looks like Profile silently jumping to Home on Xiaomi/HyperOS).
 */
public final class CrashSafety {
    private static final String TAG = "CrashSafety";
    private static volatile boolean handling;

    private CrashSafety() {}

    public static void install(Context appContext) {
        final Context app = appContext.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                Log.e(TAG, "Uncaught exception", throwable);
            } catch (Throwable ignored) {}
            try {
                FirebaseCrashlytics.getInstance().recordException(throwable);
            } catch (Throwable ignored) {}

            boolean mainThread = false;
            try {
                mainThread = Looper.getMainLooper().getThread() == thread;
            } catch (Throwable ignored) {}
            if (!mainThread) {
                // Keep the current screen (Profile) alive. OkHttp /me parse
                // used to kill the process and RoleBoot reopened Home.
                return;
            }

            if (handling) {
                Process.killProcess(Process.myPid());
                System.exit(10);
                return;
            }
            handling = true;
            try {
                TokenStore.setSessionListener(null);
            } catch (Throwable ignored) {}
            try {
                boolean hasSession = false;
                try {
                    hasSession = new TokenStore(app).hasToken();
                } catch (Throwable ignored) {}
                Intent intent = new Intent(app, hasSession
                        ? com.brightpath.sanad.ui.MainActivity.class
                        : LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                if (!hasSession) {
                    intent.putExtra("recovery_after_crash", true);
                }
                app.startActivity(intent);
            } catch (Throwable ignored) {}
            try {
                if (previous != null && (app.getApplicationInfo().flags
                        & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    previous.uncaughtException(thread, throwable);
                    return;
                }
            } catch (Throwable ignored) {}
            try {
                String mfr = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase() : "";
                if (mfr.contains("xiaomi") || mfr.contains("redmi") || mfr.contains("poco")) {
                    try {
                        Thread.sleep(400L);
                    } catch (InterruptedException ignored) {}
                }
            } catch (Throwable ignored) {}
            Process.killProcess(Process.myPid());
            System.exit(10);
        });
    }
}
