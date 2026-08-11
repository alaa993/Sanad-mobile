package com.brightpath.sanad.data;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.brightpath.sanad.R;

public class ThemeStore {
    private static final String PREF = "theme_prefs";
    private static final String KEY = "app_theme";

    public static final String THEME_BLUE = "blue";
    public static final String THEME_GRAY = "gray";
    public static final String THEME_PINK = "pink";
    /** Canonical keys (aligned with iOS AppTheme). */
    public static final String THEME_SANAD = "sanad";
    public static final String THEME_ROSE = "rose";
    public static final String THEME_GRAPHITE = "graphite";

    private final SharedPreferences prefs;

    public ThemeStore(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void saveTheme(String theme) {
        String safeTheme = normalizeTheme(theme == null ? THEME_SANAD : theme);
        prefs.edit().putString(KEY, safeTheme).apply();
    }

    public String getSavedTheme() {
        return normalizeTheme(prefs.getString(KEY, THEME_SANAD));
    }

    public static String normalizeTheme(String theme) {
        if (theme == null) return THEME_BLUE;
        String t = theme.trim().toLowerCase();
        if (THEME_SANAD.equals(t) || THEME_BLUE.equals(t)) return THEME_BLUE;
        if (THEME_ROSE.equals(t) || THEME_PINK.equals(t)) return THEME_PINK;
        if (THEME_GRAPHITE.equals(t) || THEME_GRAY.equals(t)) return THEME_GRAY;
        return THEME_BLUE;
    }

    public void applySavedTheme(AppCompatActivity activity) {
        int overlay = resolveOverlayRes(getSavedTheme());
        if (overlay != 0) {
            activity.getTheme().applyStyle(overlay, true);
        }
        applyLightSystemBars(activity);
    }

    /** Light status/nav chrome tinted by theme; dark system icons for readability. */
    public static void applyLightSystemBars(Activity activity) {
        if (activity == null) return;
        try {
            Window window = activity.getWindow();
            if (window == null) return;
            window.setStatusBarColor(chromeStatusBarColor(activity));
            window.setNavigationBarColor(chromeNavigationBarColor(activity));
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(window, window.getDecorView());
            if (controller != null) {
                controller.setAppearanceLightStatusBars(true);
                controller.setAppearanceLightNavigationBars(true);
            }
        } catch (Throwable ignored) {}
    }

    @ColorInt
    public static int chromeStatusBarColor(Context context) {
        String theme = new ThemeStore(context).getSavedTheme();
        if (THEME_PINK.equals(theme)) {
            return ContextCompat.getColor(context, R.color.sanad_canvas_rose);
        }
        if (THEME_GRAY.equals(theme)) {
            return ContextCompat.getColor(context, R.color.sanad_canvas_graphite);
        }
        return ContextCompat.getColor(context, R.color.sanad_canvas);
    }

    @ColorInt
    public static int chromeNavigationBarColor(Context context) {
        String theme = new ThemeStore(context).getSavedTheme();
        if (THEME_PINK.equals(theme)) {
            return ContextCompat.getColor(context, R.color.sanad_tab_bar_bg_rose);
        }
        if (THEME_GRAY.equals(theme)) {
            return ContextCompat.getColor(context, R.color.sanad_tab_bar_bg_graphite);
        }
        return ContextCompat.getColor(context, R.color.sanad_tab_bar_bg);
    }

    public int getLogoRes(boolean noBackground) {
        return resolveLogoRes(getSavedTheme(), noBackground);
    }

    /** Resolved primary color for the currently applied theme overlay. */
    @ColorInt
    public static int primaryColor(Context context) {
        return resolveColorAttr(context, R.attr.sanadPrimary, R.color.sanad_blue_primary);
    }

    @ColorInt
    public static int primaryDarkColor(Context context) {
        return resolveColorAttr(context, R.attr.sanadPrimaryDark, R.color.sanad_blue_primary_dark);
    }

    /** Paint a view with the active theme primary (MIUI-safe — no theme attrs in shapes). */
    public static void tintBackground(View view) {
        if (view == null) return;
        try {
            view.setBackgroundColor(primaryColor(view.getContext()));
        } catch (Throwable ignored) {}
    }

    @ColorInt
    public static int resolveColorAttr(Context context, @AttrRes int attr, int fallbackColorRes) {
        if (context == null) {
            return 0xFF2F55A5;
        }
        try {
            TypedValue tv = new TypedValue();
            Resources.Theme theme = context.getTheme();
            if (theme != null && theme.resolveAttribute(attr, tv, true)) {
                if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT
                        && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    return tv.data;
                }
                if (tv.resourceId != 0) {
                    return ContextCompat.getColor(context, tv.resourceId);
                }
            }
        } catch (Throwable ignored) {}
        try {
            return ContextCompat.getColor(context, fallbackColorRes);
        } catch (Throwable t) {
            return 0xFF2F55A5;
        }
    }

    public static int resolveLogoRes(String theme, boolean noBackground) {
        if (THEME_GRAY.equalsIgnoreCase(theme)) {
            return noBackground ? R.drawable.logograynotbackgraound : R.drawable.logogray;
        }
        if (THEME_PINK.equalsIgnoreCase(theme)) {
            return noBackground ? R.drawable.logorosenotbackground : R.drawable.logorose;
        }
        return noBackground ? R.drawable.logobluenotbackgraound : R.drawable.logoblue;
    }

    public static int resolveOverlayRes(String theme) {
        if (THEME_GRAY.equalsIgnoreCase(theme)) {
            return R.style.ThemeOverlay_Sanad_Gray;
        }
        if (THEME_PINK.equalsIgnoreCase(theme)) {
            return R.style.ThemeOverlay_Sanad_Pink;
        }
        return R.style.ThemeOverlay_Sanad_Blue;
    }
}
