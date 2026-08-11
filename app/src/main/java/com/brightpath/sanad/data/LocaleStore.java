package com.brightpath.sanad.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * حفظ/استعادة اللغة المفضلة بحيث لا تعود للغة الجهاز عند كل تشغيل.
 */
public class LocaleStore {
    private static final String PREF = "locale_prefs";
    private static final String KEY = "app_locale";
    private final SharedPreferences prefs;

    public LocaleStore(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void saveLocale(String tag) {
        prefs.edit().putString(KEY, tag == null ? "" : tag).apply();
    }

    public String getSavedLocale() {
        return prefs.getString(KEY, "");
    }

    /**
     * يطبق اللغة المحفوظة على التطبيق باستخدام AppCompatDelegate.
     */
    public void applySavedLocale() {
        String tag = getSavedLocale();
        if (tag == null || tag.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
        }
    }
}
