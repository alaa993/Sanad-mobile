package com.brightpath.sanad.data;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * تطبيق موحّد للغة — يعمل على Android 7+ (AppCompat) و Android 13+ (localeConfig).
 */
public final class LocaleHelper {

    public static final String DEFAULT_TAG = "ar";

    private LocaleHelper() {}

    public static void applySavedLocale(Context context) {
        new LocaleStore(context).applySavedLocale();
    }

    public static void ensureDefaultLocale(Context context) {
        LocaleStore store = new LocaleStore(context);
        if (TextUtils.isEmpty(store.getSavedLocale())) {
            store.saveLocale(DEFAULT_TAG);
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(DEFAULT_TAG));
        }
    }

    public static void applyLocale(Context context, String tag) {
        LocaleStore store = new LocaleStore(context);
        String normalized = normalizeTag(tag);
        if (normalized.isEmpty()) {
            store.saveLocale("");
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            store.saveLocale(normalized);
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized));
            syncLocaleToServer(context, normalized);
        }
    }

    /** اللغة المحفوظة في التفضيلات (الاختيار الصريح للمستخدم). */
    @NonNull
    public static String resolveSavedTag(@NonNull Context context) {
        LocaleStore store = new LocaleStore(context);
        String saved = store.getSavedLocale();
        if (!TextUtils.isEmpty(saved)) {
            return saved.trim().toLowerCase(Locale.US);
        }
        return DEFAULT_TAG;
    }

    /** اللغة المطبّقة فعلياً على واجهة التطبيق الآن. */
    @NonNull
    public static String resolveAppliedTag(@NonNull Context context) {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (!locales.isEmpty() && locales.get(0) != null) {
            return locales.get(0).getLanguage().toLowerCase(Locale.US);
        }
        return Locale.getDefault().getLanguage().toLowerCase(Locale.US);
    }

    /**
     * @deprecated استخدم {@link #resolveSavedTag(Context)} أو {@link #resolveAppliedTag(Context)}
     */
    @Deprecated
    @NonNull
    public static String resolveActiveTag(@NonNull Context context) {
        return resolveSavedTag(context);
    }

    public static void applyLocaleAndRecreate(@NonNull Activity activity, @NonNull String tag) {
        String normalized = normalizeTag(tag);
        if (normalized.isEmpty()) {
            LocaleStore store = new LocaleStore(activity);
            if (TextUtils.isEmpty(store.getSavedLocale())) {
                return;
            }
        } else {
            String saved = resolveSavedTag(activity);
            String applied = resolveAppliedTag(activity);
            if (normalized.equals(saved) && normalized.equalsIgnoreCase(applied)) {
                return;
            }
        }
        applyLocale(activity, tag);
        try {
            com.brightpath.sanad.ui.tour.CoachMarkManager.dismissActive();
        } catch (Throwable ignored) {}
        activity.getWindow().getDecorView().post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                activity.recreate();
            }
        });
    }

    private static void syncLocaleToServer(Context context, String tag) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                com.brightpath.sanad.data.auth.AuthRepository repo =
                        new com.brightpath.sanad.data.auth.AuthRepository(
                                context.getApplicationContext(), AppConfig.BASE_URL);
                if (repo.hasToken()) {
                    repo.updateProfile(null, tag, null);
                }
            } catch (Exception ignored) {
            }
        });
    }

    @NonNull
    private static String normalizeTag(String tag) {
        if (tag == null) return "";
        return tag.trim().toLowerCase(Locale.US);
    }
}
