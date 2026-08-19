package com.brightpath.sanad.feature.profile;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.brightpath.sanad.R;
import com.brightpath.sanad.data.ThemeStore;
import com.brightpath.sanad.data.auth.TokenStore;
import com.brightpath.sanad.ui.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Profile layouts use Material toggles that crash some HyperOS builds during inflate.
 * Keep a working screen (name + logout) instead of letting CrashSafety bounce to Home.
 */
public final class ProfileScreenViews {
    private static final int[] SEGMENT_BUTTONS = {
            R.id.btnLangArabic, R.id.btnLangEnglish, R.id.btnLangTurkish,
            R.id.btnThemeSanad, R.id.btnThemeWardi, R.id.btnThemeGraphite
    };

    private ProfileScreenViews() {}

    @NonNull
    public static View inflate(@NonNull LayoutInflater inflater,
                               @Nullable ViewGroup parent,
                               @LayoutRes int layoutRes,
                               @NonNull Fragment host) {
        try {
            View view = inflater.inflate(layoutRes, parent, false);
            if (view != null) {
                tintSegmentButtons(view.getContext(), view);
                return view;
            }
        } catch (Throwable t) {
            try {
                FirebaseCrashlytics.getInstance().recordException(t);
            } catch (Throwable ignored) {}
        }
        return fallback(inflater.getContext(), host);
    }

    /**
     * Theme toggles must not recreate the Activity while HyperOS synthesizes a
     * click from {@code group.check()}. Same-theme taps are ignored.
     */
    public static void bindThemeGroup(@NonNull Fragment host,
                                      @Nullable MaterialButtonToggleGroup group,
                                      @Nullable View btnSanad,
                                      @Nullable View btnWardi,
                                      @Nullable View btnGraphite) {
        if (group == null || !host.isAdded()) return;
        final Context ctx = host.getContext();
        if (ctx == null) return;
        final ThemeStore store = new ThemeStore(ctx);
        final boolean[] ready = {false};
        group.clearOnButtonCheckedListeners();
        bindThemeButton(btnSanad, ready, () -> applyThemeIfChanged(host, store, ThemeStore.THEME_BLUE));
        bindThemeButton(btnWardi, ready, () -> applyThemeIfChanged(host, store, ThemeStore.THEME_PINK));
        bindThemeButton(btnGraphite, ready, () -> applyThemeIfChanged(host, store, ThemeStore.THEME_GRAY));
        try {
            String saved = store.getSavedTheme();
            if (ThemeStore.THEME_PINK.equalsIgnoreCase(saved) && btnWardi != null) {
                group.check(btnWardi.getId());
            } else if (ThemeStore.THEME_GRAY.equalsIgnoreCase(saved) && btnGraphite != null) {
                group.check(btnGraphite.getId());
            } else if (btnSanad != null) {
                group.check(btnSanad.getId());
            }
        } catch (Throwable ignored) {}
        group.post(() -> ready[0] = true);
    }

    private static void bindThemeButton(@Nullable View button,
                                        @NonNull boolean[] ready,
                                        @NonNull Runnable action) {
        if (button == null) return;
        button.setOnClickListener(v -> {
            if (!ready[0]) return;
            action.run();
        });
    }

    private static void applyThemeIfChanged(@NonNull Fragment host,
                                            @NonNull ThemeStore store,
                                            @NonNull String theme) {
        try {
            if (store.getSavedTheme().equals(ThemeStore.normalizeTheme(theme))) return;
            com.brightpath.sanad.ui.tour.CoachMarkManager.dismissActive();
            store.saveTheme(theme);
            if (!host.isAdded()) return;
            android.app.Activity activity = host.getActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
            try {
                com.brightpath.sanad.router.NavRestore.remember(activity);
            } catch (Throwable ignored) {}
            activity.recreate();
        } catch (Throwable ignored) {}
    }

    public static void tintSegmentButtons(@Nullable Context ctx, @Nullable View root) {
        if (ctx == null || root == null) return;
        try {
            int checked = ThemeStore.primaryColor(ctx);
            int unchecked = ContextCompat.getColor(ctx, R.color.profile_segment_unchecked_bg);
            ColorStateList bg = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{checked, unchecked}
            );
            for (int id : SEGMENT_BUTTONS) {
                View button = root.findViewById(id);
                if (button instanceof MaterialButton) {
                    ((MaterialButton) button).setBackgroundTintList(bg);
                }
            }
        } catch (Throwable ignored) {}
    }

    /** Material toggle groups steal vertical drags on HyperOS; let the profile scroll. */
    public static void passVerticalScrollToParent(@Nullable View child) {
        if (child == null) return;
        try {
            child.setOnTouchListener((v, event) -> {
                ViewParent parent = v.getParent();
                while (parent != null && !(parent instanceof NestedScrollView)) {
                    parent = parent.getParent();
                }
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });
        } catch (Throwable ignored) {}
    }

    @NonNull
    public static String initialOf(@Nullable String name) {
        if (name == null) return "-";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "-";
        try {
            int cp = trimmed.codePointAt(0);
            return new String(Character.toChars(cp)).toUpperCase(java.util.Locale.ROOT);
        } catch (Throwable ignored) {
            return "-";
        }
    }

    @NonNull
    private static View fallback(@NonNull Context ctx, @NonNull Fragment host) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24, ctx.getResources().getDisplayMetrics());
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TokenStore tokens = new TokenStore(ctx);
        TextView name = new TextView(ctx);
        String label = tokens.getUserName();
        name.setText(label != null && !label.isEmpty()
                ? label
                : ctx.getString(R.string.profile_name_placeholder));
        name.setTextSize(20);
        name.setPadding(0, 0, 0, pad);
        root.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button logout = new Button(ctx);
        logout.setText(R.string.logout);
        logout.setOnClickListener(v -> {
            try {
                tokens.clear();
                Intent intent = new Intent(host.requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                host.startActivity(intent);
            } catch (Throwable ignored) {}
        });
        root.addView(logout, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }
}
