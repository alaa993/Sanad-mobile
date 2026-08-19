package com.brightpath.sanad.data;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.annotation.Nullable;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class AppConfig extends Application {
    private static Context context;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        try {
            CrashSafety.install(this);
        } catch (Throwable ignored) {}
        try {
            LocaleHelper.ensureDefaultLocale(this);
            LocaleHelper.applySavedLocale(this);
        } catch (Throwable ignored) {}
        boolean isDebug = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!isDebug);
            } catch (Throwable ignored) {
                // Missing/broken Firebase must never kill cold start.
            }
            try {
                // Warm Retrofit/OkHttp off the main thread to reduce first-navigation jank.
                ApiClient.get(AppConfig.this);
            } catch (Throwable ignored) {
                // Invalid base URL / network stack issues must not crash the process.
            }
            try {
                // Warm auth client (separate from ApiClient) so first login is more reliable.
                com.brightpath.sanad.data.auth.AuthRepository.warmUp(AppConfig.this);
            } catch (Throwable ignored) {}
        });
    }

    public static Context getContext() {
        return context;
    }

    public static final String BASE_URL = "https://dashboard.sanadhub.cloud";

    public static String privacyPolicyUrl() {
        return BASE_URL + "/privacy";
    }

    public static String deleteAccountUrl() {
        return BASE_URL + "/delete-account";
    }

    public static String termsUrl() {
        return BASE_URL + "/terms";
    }

    public static String contactUrl() {
        return BASE_URL + "/contact";
    }

    public static String storageUrl(@Nullable String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("http://")) {
            return "https://" + trimmed.substring("http://".length());
        }
        if (trimmed.startsWith("/")) {
            return BASE_URL + trimmed;
        }
        String clean = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        if (clean.startsWith("storage/")) {
            return BASE_URL + "/" + clean;
        }
        return BASE_URL + "/storage/" + clean;
    }
}
