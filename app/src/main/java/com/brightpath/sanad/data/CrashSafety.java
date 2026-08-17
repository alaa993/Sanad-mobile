package com.brightpath.sanad.data;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.ui.LoginActivity;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Last-resort handler: record the crash and open Login.
 * On Xiaomi/HyperOS, avoid killProcess when possible — it surfaces as
 * "Sanad keeps stopping" even after recovery.
 */
public final class CrashSafety {
    private static final String TAG = "CrashSafety";
    private static volatile boolean handling;

    private CrashSafety() {}

    public static void install(Context appContext) {
        final Context app = appContext.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (handling) {
                Process.killProcess(Process.myPid());
                System.exit(10);
                return;
            }
            handling = true;
            try {
                Log.e(TAG, "Uncaught exception", throwable);
            } catch (Throwable ignored) {}
            try {
                FirebaseCrashlytics.getInstance().recordException(throwable);
            } catch (Throwable ignored) {}
            try {
                TokenStore.setSessionListener(null);
                // Do NOT clear a valid session on every crash — only navigate to recovery.
            } catch (Throwable ignored) {}
            try {
                boolean hasSession = false;
                try {
                    hasSession = new TokenStore(app).hasToken();
                } catch (Throwable ignored) {}
                // Xiaomi/HyperOS profile crashes must not dump a still-logged-in user on Login.
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
            // Xiaomi shows "keeps stopping" on killProcess; still need to stop a broken process.
            // Prefer a short delay so Login can appear first.
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
